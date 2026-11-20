package uk.gov.android.network.client.v3

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.file.Path as NioPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.io.files.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class CachingRawHttpClientTest {
    @TempDir
    lateinit var directory: NioPath

    // Not valid UTF-8 (0x82, 0xFF, 0xC0) and contains NUL, like a real DER CRL.
    private val der = byteArrayOf(0x30, 0x82.toByte(), 0x01, 0xE1.toByte(), 0xFF.toByte(), 0x00, 0xC0.toByte())

    private fun storage(): CacheStorage = FileStorage(Path(directory.toString()))

    private fun client(
        storage: CacheStorage = storage(),
        timeouts: CachingRawHttpClient.Timeouts = CachingRawHttpClient.Timeouts(),
        handler: suspend (io.ktor.client.engine.mock.MockRequestHandleScope, io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ) = CachingRawHttpClient(MockEngine { request -> handler(this, request) }, storage, "test", timeouts)

    private fun offlineClient(storage: CacheStorage = storage()) =
        CachingRawHttpClient(MockEngine { throw IOException("offline") }, storage, "test")

    @Test
    fun `returns binary body unchanged and exposes headers`() = runBlocking {
        val client = client { scope, _ ->
            scope.respond(
                content = der,
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/pkix-crl"),
                    HttpHeaders.CacheControl to listOf("max-age=3600"),
                    HttpHeaders.ETag to listOf("\"abc\""),
                ),
            )
        }

        val response = client.get(URL)

        assertArrayEquals(der, response.body)
        assertEquals("max-age=3600", response.header("cache-control"))
        assertEquals("\"abc\"", response.header("ETag"))
    }

    @Test
    fun `cache persists across client instances`() = runBlocking {
        var firstEngineCalls = 0
        client { scope, _ ->
            firstEngineCalls++
            scope.respond(der, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=3600"))
        }.get(URL)

        // New client, same directory, engine that cannot reach the network at all.
        val response = offlineClient().get(URL)

        assertEquals(1, firstEngineCalls)
        assertArrayEquals(der, response.body)
    }

    @Test
    fun `forced revalidation sends a conditional request even when fresh`() = runBlocking {
        val seenIfNoneMatch = mutableListOf<String?>()
        val client = client { scope, request ->
            seenIfNoneMatch += request.headers[HttpHeaders.IfNoneMatch]
            if (request.headers[HttpHeaders.IfNoneMatch] != null) {
                scope.respond("", HttpStatusCode.NotModified, headersOf(HttpHeaders.ETag, "\"abc\""))
            } else {
                scope.respond(
                    der,
                    HttpStatusCode.OK,
                    headersOf(
                        HttpHeaders.CacheControl to listOf("max-age=3600"),
                        HttpHeaders.ETag to listOf("\"abc\""),
                    ),
                )
            }
        }

        client.get(URL)
        val forced = client.get(URL, forceRevalidation = true)

        assertEquals(listOf(null, "\"abc\""), seenIfNoneMatch)
        assertArrayEquals(der, forced.body)
    }

    @Test
    fun `serves a stale entry when retrieval fails and headers permit`() = runBlocking {
        client { scope, _ ->
            scope.respond(der, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=0"))
        }.get(URL)

        val response = offlineClient().get(URL)

        assertArrayEquals(der, response.body)
    }

    @Test
    fun `does not serve a stale entry marked must-revalidate`() = runBlocking {
        client { scope, _ ->
            scope.respond(
                der,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.CacheControl, "max-age=0, must-revalidate"),
            )
        }.get(URL)

        assertThrows<IOException> { offlineClient().get(URL) }
        Unit
    }

    @Test
    fun `no stale fallback when revalidation was forced`() = runBlocking {
        client { scope, _ ->
            scope.respond(der, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=0"))
        }.get(URL)

        assertThrows<IOException> { offlineClient().get(URL, forceRevalidation = true) }
        Unit
    }

    @Test
    fun `cached reads the stored entry without network and null when absent`() = runBlocking {
        assertNull(offlineClient().cached(URL))

        client { scope, _ ->
            scope.respond(der, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=0"))
        }.get(URL)

        val stored = offlineClient().cached(URL)
        assertNotNull(stored)
        assertArrayEquals(der, stored!!.body)
    }

    @Test
    fun `mutating a returned body never changes the cached response`() = runBlocking {
        val storage = storage()
        val online = client(storage) { scope, _ ->
            scope.respond(der.copyOf(), HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=0"))
        }
        online.get(URL).body[0] = 99 // network path

        val offline = offlineClient(storage)
        offline.cached(URL)!!.body[1] = 98 // cached() path
        offline.get(URL).body[2] = 97 // stale fallback path

        assertArrayEquals(der, offline.cached(URL)!!.body)
        assertArrayEquals(der, offline.get(URL).body)
    }

    /** Serves the CRL with an ETag, and 304 to a matching If-None-Match. */
    private fun etagEngine(maxAge: Int, requests: MutableList<String?>) = MockEngine { request ->
        val ifNoneMatch = request.headers[HttpHeaders.IfNoneMatch]
        requests += ifNoneMatch
        if (ifNoneMatch == "\"v1\"") {
            respond(
                "",
                HttpStatusCode.NotModified,
                headersOf(
                    HttpHeaders.ETag to listOf("\"v1\""),
                    HttpHeaders.CacheControl to listOf("max-age=$maxAge"),
                ),
            )
        } else {
            respond(
                der,
                HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.ETag to listOf("\"v1\""),
                    HttpHeaders.CacheControl to listOf("max-age=$maxAge"),
                ),
            )
        }
    }

    @Test
    fun `source is network, then cache while fresh, with no second request`() = runBlocking {
        val requests = mutableListOf<String?>()
        val client = CachingRawHttpClient(etagEngine(3600, requests), storage(), "test")

        assertEquals(RawResponseSource.NETWORK, client.get(URL).source)
        val second = client.get(URL)

        assertEquals(RawResponseSource.CACHE, second.source)
        assertArrayEquals(der, second.body)
        assertEquals(1, requests.size)
    }

    @Test
    fun `a stale entry is revalidated with a 304 and reports revalidated`() = runBlocking {
        val requests = mutableListOf<String?>()
        val client = CachingRawHttpClient(etagEngine(0, requests), storage(), "test")

        client.get(URL)
        val second = client.get(URL)

        assertEquals(listOf(null, "\"v1\""), requests)
        assertEquals(RawResponseSource.REVALIDATED, second.source)
        assertEquals(200, second.status)
        assertArrayEquals(der, second.body)
    }

    @Test
    fun `forced revalidation of a fresh entry reports revalidated on 304`() = runBlocking {
        val requests = mutableListOf<String?>()
        val client = CachingRawHttpClient(etagEngine(3600, requests), storage(), "test")

        client.get(URL)

        assertEquals(RawResponseSource.REVALIDATED, client.get(URL, forceRevalidation = true).source)
        assertEquals(2, requests.size)
    }

    @Test
    fun `stale fallback and cached report their source`() = runBlocking {
        val storage = storage()
        client(storage) { scope, _ ->
            scope.respond(der, HttpStatusCode.OK, headersOf(HttpHeaders.CacheControl, "max-age=0"))
        }.get(URL)

        assertEquals(RawResponseSource.STALE, offlineClient(storage).get(URL).source)
        assertEquals(RawResponseSource.CACHE, offlineClient(storage).cached(URL)!!.source)
    }

    @Test
    fun `each request is logged once without the body`() = runBlocking {
        val lines = mutableListOf<String>()
        val client = CachingRawHttpClient(
            etagEngine(0, mutableListOf()),
            storage(),
            "test",
            logger = { lines += it },
        )

        client.get(URL)
        client.get(URL)

        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("NETWORK status=200 bytes=${der.size}"), lines[0])
        assertTrue(lines[1].contains("REVALIDATED status=200"), lines[1])
    }

    @Test
    fun `request timeout surfaces as IOException`() = runBlocking {
        val client = client(timeouts = CachingRawHttpClient.Timeouts(requestMillis = 50)) { scope, _ ->
            delay(5_000)
            scope.respond(der, HttpStatusCode.OK)
        }

        assertThrows<IOException> { client.get(URL) }
        Unit
    }

    private companion object {
        const val URL = "http://dvs.verifier-ca.integration.account.gov.uk/crl/example.crl"
    }
}
