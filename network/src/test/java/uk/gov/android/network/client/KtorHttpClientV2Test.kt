package uk.gov.android.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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

    private val mockEngine =
        MockEngine { respond(content = RESPONSE_BODY, status = HttpStatusCode.OK) }

    private fun createClient(engine: MockEngine = mockEngine): GenericHttpClient =
        KtorHttpClient(
            userAgentGenerator = userAgentGenerator,
            logger = KtorLogger.noOp,
            ktorClientEngine = engine,
        )

    @Test
    fun `Get - returns response body and status`() =
        runTest {
            val client = createClient()

            val response = client.request(ApiRequest.Get(URL))

            assertEquals(200, response.status)
            assertEquals(RESPONSE_BODY, response.body)
        }

    @Test
    fun `Get - error response throws GenericResponseException`() =
        runTest {
            val engine =
                MockEngine { respond(content = "unauthorized", status = HttpStatusCode.Unauthorized) }
            val client = createClient(engine)

            val exception =
                assertThrows<GenericResponseException> {
                    client.request(ApiRequest.Get(URL))
                }

            assertEquals(401, exception.response.status)
        }

    @Test
    fun `Post - returns response body and status`() =
        runTest {
            val engine =
                MockEngine { respond(content = RESPONSE_BODY, status = HttpStatusCode.Created) }
            val client = createClient(engine)

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
            val client = createClient()

            client.request(
                ApiRequest.Post(
                    url = URL,
                    body = REQUEST_BODY,
                    contentType = ContentType.APPLICATION_JSON,
                ),
            )

            val sentRequest = mockEngine.requestHistory.first()
            assertEquals("application/json", sentRequest.body.contentType?.toString())
        }

    @Test
    fun `Post - serializable body returns response`() =
        runTest {
            val client = createClient()

            val response =
                client.request(
                    ApiRequest.Post(
                        url = URL,
                        body = SerializableBody(title = "title", body = "body"),
                        contentType = ContentType.APPLICATION_JSON,
                    ),
                )

            assertEquals(200, response.status)
            assertEquals(RESPONSE_BODY, response.body)
        }

    @Test
    fun `Post - non-serializable body throws SerializationException`() =
        runTest {
            val client = createClient()

            assertThrows<SerializationException> {
                client.request(
                    ApiRequest.Post(
                        url = URL,
                        body = NonSerializableBody(title = "title", body = "body"),
                        contentType = ContentType.APPLICATION_JSON,
                    ),
                )
            }
        }

    @Test
    fun `Post - error response throws GenericResponseException`() =
        runTest {
            val engine =
                MockEngine {
                    respond(content = "server error", status = HttpStatusCode.InternalServerError)
                }
            val client = createClient(engine)

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
            val client = createClient()

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
            val client = createClient()

            client.request(
                ApiRequest.FormUrlEncoded(
                    url = URL,
                    params = listOf("field" to "value"),
                ),
            )

            val sentRequest = mockEngine.requestHistory.first()
            assertEquals("field=value", sentRequest.body.toByteArray().decodeToString())
        }

    @Test
    fun `FormUrlEncoded - sends the correct ContentType header`() =
        runTest {
            val client = createClient()

            client.request(
                ApiRequest.FormUrlEncoded(
                    url = URL,
                    params = listOf("field" to "value"),
                ),
            )

            val sentRequest = mockEngine.requestHistory.first()
            assertEquals(
                "application/x-www-form-urlencoded; charset=UTF-8",
                sentRequest.body.contentType?.toString(),
            )
        }

    @Test
    fun `FormUrlEncoded - error response throws GenericResponseException`() =
        runTest {
            val engine =
                MockEngine { respond(content = "bad request", status = HttpStatusCode.BadRequest) }
            val client = createClient(engine)

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
            val client = createClient()

            client.request(
                ApiRequest.Get(
                    url = URL,
                    headers = listOf("X-Custom" to "value"),
                ),
            )

            val sentRequest = mockEngine.requestHistory.first()
            assertEquals(listOf("value"), sentRequest.headers.getAll("X-Custom"))
        }

    @Test
    fun `request propogates IOExceptions`() =
        runTest {
            val client =
                createClient(MockEngine { throw SocketTimeoutException() })

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

private data class NonSerializableBody(
    val title: String,
    val body: String,
)

@Serializable
private data class SerializableBody(
    val title: String,
    val body: String,
)
