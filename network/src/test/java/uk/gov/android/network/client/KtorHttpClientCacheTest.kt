package uk.gov.android.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorHttpClientCacheTest {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")

    @Test
    fun `cache stores response with max-age directive`() {
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

        runBlocking {
            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("cached response"), response1)
            assertEquals(ApiResponse.Success("cached response"), response2)
            assertEquals(1, requestCount, "Should only make one network request due to caching")
        }
    }

    @Test
    fun `cache respects no-store directive`() {
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

        runBlocking {
            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("response 1"), response1)
            assertEquals(ApiResponse.Success("response 2"), response2)
            assertEquals(2, requestCount, "Should make two network requests (no caching)")
        }
    }

    @Test
    fun `cache validates with ETag`() {
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

        runBlocking {
            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("cached response"), response1)
            assertEquals(ApiResponse.Success("cached response"), response2)
            assertEquals(2, requestCount, "Should make validation request with If-None-Match")
        }
    }

    @Test
    fun `cache does not store responses without cache headers`() {
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

        runBlocking {
            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("response 1"), response1)
            assertEquals(ApiResponse.Success("response 2"), response2)
            assertEquals(2, requestCount, "Should make two requests without cache headers")
        }
    }

    @Test
    fun `cache isolates different URLs`() {
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

        runBlocking {
            client.makeRequest(ApiRequest.Get("https://api.example.com/data1"))
            client.makeRequest(ApiRequest.Get("https://api.example.com/data2"))
            client.makeRequest(ApiRequest.Get("https://api.example.com/data1"))

            assertEquals(2, requestCount, "Should cache each URL separately")
        }
    }

    @Test
    fun `cache respects private directive`() {
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
                                    HttpHeaders.CacheControl to listOf("max-age=3600"),
                                ),
                        )
                    },
            )

        runBlocking {
            val response1 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))
            val response2 = client.makeRequest(ApiRequest.Get("https://api.example.com/data"))

            assertEquals(ApiResponse.Success("private response"), response1)
            assertEquals(ApiResponse.Success("private response"), response2)
            assertEquals(1, requestCount, "Should cache private responses locally")
        }
    }
}
