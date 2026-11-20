package uk.gov.android.network.client.v3

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.utils.CacheControl.NO_STORE
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parseHeaderValue
import io.ktor.util.AttributeKey
import io.ktor.util.toMap
import java.io.File
import kotlinx.io.IOException
import kotlinx.io.files.Path
import uk.gov.android.network.client.createKtorAndroidEngine
import uk.gov.android.network.log.KtorLogger

/**
 * PROPOSAL (not yet wired into [uk.gov.android.network.client.KtorHttpClient]).
 *
 * A small Ktor-level client for resources that are binary, need their response headers and must
 * be usable offline, such as X.509 CRLs. It sketches the library changes needed for that:
 *
 * 1. **Bytes, not String**: [RawHttpResponse.body] is a [ByteArray], so DER survives.
 * 2. **Headers exposed**: [RawHttpResponse.headers].
 * 3. **Caller-supplied, persistent cache**: [cacheStorage] is injected (for example Ktor's
 *    `FileStorage`), instead of the default in-memory cache `install(HttpCache)` gives today.
 * 4. **Offline inspection**: [cached] reads the stored entry without any network call.
 * 5. **Bounded timeouts**: [Timeouts] installs Ktor's [HttpTimeout].
 * 6. **Forced revalidation**: `forceRevalidation` sends request `Cache-Control: no-cache`, which
 *    Ktor's cache turns into a conditional request.
 * 7. **Stale-if-error**: Ktor has no such behaviour, so [get] falls back to the stored entry when
 *    retrieval fails, unless the stored response said `no-cache` or `must-revalidate`.
 *
 * Not handled: `s-maxage`, `proxy-revalidate`, `stale-if-error` directives, `Vary`, redirects.
 */
class CachingRawHttpClient internal constructor(
    engine: HttpClientEngine,
    private val cacheStorage: CacheStorage,
    userAgent: String,
    timeouts: Timeouts = Timeouts(),
    private val logger: KtorLogger = KtorLogger.noOp,
) : RawHttpClient {
    data class Timeouts(
        val connectMillis: Long = DEFAULT_CONNECT_MILLIS,
        val requestMillis: Long = DEFAULT_REQUEST_MILLIS,
    )

    private val client =
        HttpClient(engine) {
            install(UserAgent) { agent = userAgent }
            install(HttpTimeout) {
                connectTimeoutMillis = timeouts.connectMillis
                requestTimeoutMillis = timeouts.requestMillis
            }
            install(HttpCache) {
                // Ktor stores non-`private` responses in the public storage and `private` ones in
                // the private storage, so both must point at the same persistent storage.
                publicStorage(cacheStorage)
                privateStorage(cacheStorage)
            }
        }.apply {
            // The receive pipeline runs only for responses that really came from the server: a
            // fresh cache hit finishes the send pipeline early and never reaches it. Phase
            // `Before` precedes HttpCache's phase, so a 304 is still visible here before HttpCache
            // replaces it with the stored response.
            receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
                response.call.attributes.getOrNull(NETWORK_EXCHANGE)?.status = response.status.value
            }
        }

    /** Per-request record of the server exchange, if one happened. */
    private class NetworkExchange(var status: Int? = null)

    /**
     * GET [url].
     *
     * @param forceRevalidation check with the server even if the stored response is still fresh.
     *   No stale fallback is used in this case, because the caller has already decided the stored
     *   copy is not good enough.
     * @return the response for any status. A 2xx is returned from the network or the cache.
     *   Otherwise a permitted stale entry is returned if there is one, else the response itself.
     * @throws IOException when retrieval failed and no stale entry may be served
     */
    override suspend fun get(
        url: String,
        headers: List<Pair<String, String>>,
        forceRevalidation: Boolean,
    ): RawHttpResponse {
        val network =
            try {
                Result.success(fetch(url, headers, forceRevalidation))
            } catch (e: IOException) {
                Result.failure(e)
            }

        val response = network.getOrNull()
        if (response != null && response.status in SUCCESS_RANGE) return response.also {
            log(
                url,
                it
            )
        }

        if (!forceRevalidation) {
            cacheStorage.findAll(Url(url)).latest()?.takeIf { it.mayServeStale() }?.let {
                return it.toRaw(RawResponseSource.STALE).also { stale -> log(url, stale) }
            }
        }
        network.exceptionOrNull()?.let { logger.log("Raw GET $url -> failed: ${it.message}") }
        return network.getOrThrow().also { log(url, it) }
    }

    /** Headers-level only. CRL bodies are never logged. */
    private fun log(url: String, response: RawHttpResponse) {
        logger.log(
            "Raw GET $url -> ${response.source} status=${response.status} " +
                    "bytes=${response.body.size} etag=${response.header(HttpHeaders.ETag)} " +
                    "cache-control=${response.header(HttpHeaders.CacheControl)}",
        )
    }

    /**
     * The stored entry for [url], whatever its age, without contacting the server.
     * Returns null if nothing is stored.
     */
    override suspend fun cached(url: String): RawHttpResponse? =
        cacheStorage.findAll(Url(url)).latest()?.toRaw(RawResponseSource.CACHE)

    private suspend fun fetch(
        url: String,
        headers: List<Pair<String, String>>,
        forceRevalidation: Boolean,
    ): RawHttpResponse {
        val exchange = NetworkExchange()
        val response =
            client.get(url) {
                attributes.put(NETWORK_EXCHANGE, exchange)
                headers.forEach { (name, value) -> header(name, value) }
                if (forceRevalidation) header(HttpHeaders.CacheControl, NO_CACHE)
            }
        return RawHttpResponse(
            status = response.status.value,
            headers = response.headers.toMap(),
            body = response.body(),
            source =
                when (exchange.status) {
                    null -> RawResponseSource.CACHE
                    HTTP_NOT_MODIFIED -> RawResponseSource.REVALIDATED
                    else -> RawResponseSource.NETWORK
                },
        )
    }

    private fun Set<CachedResponseData>.latest(): CachedResponseData? =
        maxByOrNull { it.responseTime.timestamp }

    private fun CachedResponseData.mayServeStale(): Boolean {
        val directives =
            headers.getAll(HttpHeaders.CacheControl)
                .orEmpty()
                .flatMap { parseHeaderValue(it) }
                .map { it.value.lowercase() }
        return NO_CACHE !in directives && NO_STORE !in directives
    }

    private fun CachedResponseData.toRaw(source: RawResponseSource) = RawHttpResponse(
        status = statusCode.value,
        headers = headers.toMap(),
        // Caller-owned copy: the stored array is shared with the cache's in-memory layer.
        body = body.copyOf(),
        source = source,
    )

    companion object {
        /**
         * Production factory. Keeps Ktor out of the caller's compile classpath: uses the Android
         * engine with TLS 1.2+ enforcement and a persistent on-disk cache in [cacheDirectory].
         *
         * Create one instance per process and share it, because the directory is owned by a
         * single cache. Callers choose the directory, for example `noBackupFilesDir/crl-cache`.
         */
        fun create(
            cacheDirectory: File,
            userAgent: String,
            timeouts: Timeouts = Timeouts(),
            logger: KtorLogger = KtorLogger.noOp,
        ): CachingRawHttpClient = create(
            engine = createKtorAndroidEngine(),
            config = RawCacheConfig(cacheDirectory, timeouts),
            userAgent = userAgent,
            logger = logger,
        )

        /** Used by [uk.gov.android.network.client.KtorHttpClient] to share its engine. */
        internal fun create(
            engine: HttpClientEngine,
            config: RawCacheConfig,
            userAgent: String,
            logger: KtorLogger = KtorLogger.noOp,
        ): CachingRawHttpClient = CachingRawHttpClient(
            engine = engine,
            cacheStorage = FileStorage(Path(config.directory.path)),
            userAgent = userAgent,
            timeouts = config.timeouts,
            logger = logger,
        )

        private const val DEFAULT_CONNECT_MILLIS = 10_000L
        private const val DEFAULT_REQUEST_MILLIS = 30_000L
        private const val NO_CACHE = "no-cache"
        private const val MUST_REVALIDATE = "must-revalidate"
        private val SUCCESS_RANGE = 200..299
        private const val HTTP_NOT_MODIFIED = 304
        private val NETWORK_EXCHANGE = AttributeKey<NetworkExchange>("RawHttpNetworkExchange")
    }
}

/** Where the bytes of a [RawHttpResponse] came from. */
enum class RawResponseSource {
    /** Downloaded: the server sent a full response. */
    NETWORK,

    /** The server answered 304 Not Modified, so the stored bytes were reused and refreshed. */
    REVALIDATED,

    /** Served from the cache without contacting the server. */
    CACHE,

    /** Retrieval failed, so a stored entry the headers allow reusing was served instead. */
    STALE,
}

/** A raw HTTP response: status, all headers, the unmodified body bytes and where they came from. */
class RawHttpResponse(
    val status: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
    val source: RawResponseSource = RawResponseSource.NETWORK,
) {
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
}

/** Seam for callers and tests that must not depend on Ktor types. */
interface RawHttpClient {
    /** @see CachingRawHttpClient.get */
    suspend fun get(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        forceRevalidation: Boolean = false,
    ): RawHttpResponse

    /** @see CachingRawHttpClient.cached */
    suspend fun cached(url: String): RawHttpResponse?
}
