package uk.gov.android.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorHttpClientCacheTest {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")

    /**
     * Tests that responses with Cache-Control: max-age=3600 are cached for the specified duration.
     * The client should store the response and serve it from cache for subsequent identical requests
     * without making additional network calls.
     */
    @Test
    fun `cache stores response with max-age directive`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            requestCount++
                            respond(
                                content = "cached response",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType to listOf("application/json"),
                                        HttpHeaders.CacheControl to listOf("max-age=3600"),
                                    ),
                            )
                        },
                )

            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("cached response"), response1)
            assertEquals(ApiResponse.Success("cached response"), response2)
            assertEquals(1, requestCount, "Should only make one network request due to caching")
        }

    /**
     * Tests that responses with Cache-Control: no-store are never cached.
     * The no-store directive prevents any caching, so each request must go to the network
     * even for identical URLs.
     */
    @Test
    fun `cache respects no-store directive`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            requestCount++
                            respond(
                                content = "response $requestCount",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType to listOf("application/json"),
                                        HttpHeaders.CacheControl to listOf("no-store"),
                                    ),
                            )
                        },
                )

            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("response 1"), response1)
            assertEquals(ApiResponse.Success("response 2"), response2)
            assertEquals(2, requestCount, "Should make two network requests (no caching)")
        }

    /**
     * Tests ETag-based cache validation using conditional requests.
     * When a cached response has an ETag, subsequent requests include If-None-Match header.
     * If content hasn't changed, server returns 304 Not Modified and cached content is used.
     */
    @Test
    fun `cache validates with ETag`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine { request ->
                            requestCount++
                            if (request.headers[HttpHeaders.IfNoneMatch] == "\"abc123\"") {
                                respond(
                                    content = "",
                                    status = HttpStatusCode.NotModified,
                                    headers =
                                        headersOf(
                                            HttpHeaders.ETag,
                                            "\"abc123\"",
                                        ),
                                )
                            } else {
                                respond(
                                    content = "cached response",
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            HttpHeaders.ContentType to listOf("application/json"),
                                            HttpHeaders.CacheControl to listOf("no-cache"),
                                            HttpHeaders.ETag to listOf("\"abc123\""),
                                        ),
                                )
                            }
                        },
                )

            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("cached response"), response1)
            assertEquals(ApiResponse.Success("cached response"), response2)
            assertEquals(2, requestCount, "Should make validation request with If-None-Match")
        }

    /**
     * Tests that responses without any cache-control headers are not cached.
     * Without explicit caching directives, the client should make fresh network requests
     * for each call to avoid serving stale data.
     */
    @Test
    fun `cache does not store responses without cache headers`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            requestCount++
                            respond(
                                content = "response $requestCount",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        "application/json",
                                    ),
                            )
                        },
                )

            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("response 1"), response1)
            assertEquals(ApiResponse.Success("response 2"), response2)
            assertEquals(2, requestCount, "Should make two requests without cache headers")
        }

    /**
     * Tests that the cache correctly isolates entries by URL.
     * Different URLs should have separate cache entries, so caching one URL
     * should not affect requests to different URLs.
     */
    @Test
    fun `cache isolates different URLs`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine { request ->
                            requestCount++
                            respond(
                                content = "response for ${request.url}",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType to listOf("application/json"),
                                        HttpHeaders.CacheControl to listOf("max-age=3600"),
                                    ),
                            )
                        },
                )

            client.makeRequest(ApiRequest.Get("https://api.example.com/data1"))
            client.makeRequest(ApiRequest.Get("https://api.example.com/data2"))
            client.makeRequest(ApiRequest.Get("https://api.example.com/data1"))

            assertEquals(2, requestCount, "Should cache each URL separately")
        }

    /**
     * Tests that responses with Cache-Control: private, max-age=3600 are cached locally.
     * The 'private' directive indicates the response is intended for a single user
     * and can be cached by the client (but not by shared/proxy caches).
     */
    @Test
    fun `cache respects private directive`() =
        runTest {
            var requestCount = 0
            val client =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            requestCount++
                            respond(
                                content = "private response",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType to listOf("application/json"),
                                        HttpHeaders.CacheControl to listOf("private, max-age=3600"),
                                    ),
                            )
                        },
                )

            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("private response"), response1)
            assertEquals(ApiResponse.Success("private response"), response2)
            assertEquals(1, requestCount, "Should cache private responses locally")
        }
}
