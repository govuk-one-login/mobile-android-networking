package uk.gov.android.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.client.v2.GenericHttpClient
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub
import java.net.SocketTimeoutException

class KtorHttpClientV2Test {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")

    private fun createClient(engine: MockEngine): GenericHttpClient =
        KtorHttpClient(
            userAgentGenerator = userAgentGenerator,
            logger = KtorLogger.noOp,
            ktorClientEngine = engine,
        )

    @Test
    fun `Get - returns response body and status`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = RESPONSE_BODY,
                            status = HttpStatusCode.OK,
                        )
                    },
                )

            val response = client.request(ApiRequest.Get(URL))

            assertEquals(200, response.status)
            assertEquals(RESPONSE_BODY, response.body)
        }

    @Test
    fun `Get - error response throws GenericResponseException`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = "unauthorized",
                            status = HttpStatusCode.Unauthorized,
                        )
                    },
                )

            val exception =
                assertThrows<GenericResponseException> {
                    client.request(ApiRequest.Get(URL))
                }

            assertEquals(401, exception.response.status)
        }

    @Test
    fun `Post - returns response body and status`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = RESPONSE_BODY,
                            status = HttpStatusCode.Created,
                        )
                    },
                )

            val response =
                client.request(
                    ApiRequest.Post(
                        url = URL,
                        body = REQUEST_BODY,
                    ),
                )

            assertEquals(201, response.status)
            assertEquals(RESPONSE_BODY, response.body)
        }

    @Test
    fun `Post - given contentType provided, sets content type header`() =
        runTest {
            var receivedContentType: String? = null
            val client =
                createClient(
                    MockEngine { request ->
                        receivedContentType = request.body.contentType?.toString()
                        respond(
                            content = RESPONSE_BODY,
                            status = HttpStatusCode.OK,
                        )
                    },
                )

            client.request(
                ApiRequest.Post(
                    url = URL,
                    body = REQUEST_BODY,
                    contentType = ContentType.APPLICATION_JSON,
                ),
            )

            assertEquals("application/json", receivedContentType)
        }

    @Test
    fun `Post - error response throws GenericResponseException`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = "server error",
                            status = HttpStatusCode.InternalServerError,
                        )
                    },
                )

            val exception =
                assertThrows<GenericResponseException> {
                    client.request(
                        ApiRequest.Post(
                            url = URL,
                            body = REQUEST_BODY,
                            contentType = ContentType.APPLICATION_JSON,
                        ),
                    )
                }

            assertEquals(500, exception.response.status)
        }

    @Test
    fun `FormUrlEncoded - returns response body and status`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = RESPONSE_BODY,
                            status = HttpStatusCode.OK,
                        )
                    },
                )

            val response =
                client.request(
                    ApiRequest.FormUrlEncoded(
                        url = URL,
                        params = listOf("key" to "value"),
                    ),
                )

            assertEquals(200, response.status)
            assertEquals(RESPONSE_BODY, response.body)
        }

    @Test
    fun `FormUrlEncoded - sends params with correct keys and values`() =
        runTest {
            var receivedBody = ""
            val client =
                createClient(
                    MockEngine { request ->
                        receivedBody = request.body.toByteArray().decodeToString()
                        respond(
                            content = RESPONSE_BODY,
                            status = HttpStatusCode.OK,
                        )
                    },
                )

            client.request(
                ApiRequest.FormUrlEncoded(
                    url = URL,
                    params = listOf("field" to "value"),
                ),
            )

            assertEquals("field=value", receivedBody)
        }

    @Test
    fun `FormUrlEncoded - error response throws GenericResponseException`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        respond(
                            content = "bad request",
                            status = HttpStatusCode.BadRequest,
                        )
                    },
                )

            val exception =
                assertThrows<GenericResponseException> {
                    client.request(
                        ApiRequest.FormUrlEncoded(
                            url = URL,
                            params = listOf("key" to "value"),
                        ),
                    )
                }

            assertEquals(400, exception.response.status)
        }

    @Test
    fun `request includes custom headers`() =
        runTest {
            var receivedHeaders: Map<String, List<String>> = emptyMap()
            val client =
                createClient(
                    MockEngine { request ->
                        receivedHeaders = request.headers.entries().associate { it.key to it.value }
                        respond(
                            content = "ok",
                            status = HttpStatusCode.OK,
                        )
                    },
                )

            client.request(
                ApiRequest.Get(
                    url = URL,
                    headers = listOf("X-Custom" to "value"),
                ),
            )

            assertEquals(listOf("value"), receivedHeaders["X-Custom"])
        }

    @Test
    fun `request propogates IOExceptions`() =
        runTest {
            val client =
                createClient(
                    MockEngine {
                        throw SocketTimeoutException()
                    },
                )

            assertThrows<java.io.IOException> {
                client.request(ApiRequest.Get(URL))
            }
        }

    companion object {
        private const val URL = "https://example.com"
        private const val RESPONSE_BODY = "response body"
        private const val REQUEST_BODY = "request body"
    }
}
