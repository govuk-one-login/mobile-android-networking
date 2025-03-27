package uk.gov.android.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.api.ApiResponseException
import uk.gov.android.network.client.HttpStatusCodeExtensions.TransportError
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorHttpClientTest {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")
    private val sut = KtorHttpClient(userAgentGenerator)

    @OptIn(ExperimentalSerializationApi::class)
    private fun setupHttpClient(engine: MockEngine) {
        val httpClient =
            HttpClient(engine) {
                expectSuccess = true

                HttpResponseValidator {
                    handleResponseExceptionWithRequest { exception, _ ->
                        val responseException =
                            exception as? ResponseException
                                ?: return@handleResponseExceptionWithRequest
                        val exceptionResponse = responseException.response

                        throw ResponseException(exceptionResponse, exceptionResponse.bodyAsText())
                    }
                }

                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            explicitNulls = false
                        },
                    )
                }
            }
        sut.setHttpClient(httpClient)
    }

    @Test
    fun testGetSuccess() {
        val expectedResultString = "api response"
        val expectedResponse = ApiResponse.Success(expectedResultString)
        val url = "url"
        setupHttpClient(
            MockEngine {
                respond(
                    content = expectedResultString,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assertEquals(expectedResponse, actualResponse)
        }
    }

    @Test
    fun testGetResponseFailureThrown() {
        val errorString = "api response error"
        val url = "url"
        setupHttpClient(
            MockEngine {
                respond(
                    content = errorString,
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.Unauthorized.value, failureResponse.status)
            assert(failureResponse.error is ApiResponseException)
        }
    }

    @Test
    fun testGetExceptionThrown() {
        val errorMessage = "api response error"
        val url = "url"
        setupHttpClient(
            MockEngine {
                throw IllegalStateException(errorMessage)
            },
        )

        runBlocking {
            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.TransportError.value, failureResponse.status)
            assert(failureResponse.error is IllegalStateException)
        }
    }

    @Test
    fun testPostSuccess() {
        val expectedResultString = "response"
        val expectedResponse = ApiResponse.Success(expectedResultString)
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON
        setupHttpClient(
            MockEngine {
                respond(
                    content = expectedResultString,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.Post(
                        url = url,
                        body = body,
                        contentType = contentType,
                    ),
                )
            assertEquals(expectedResponse, actualResponse)
        }
    }

    @Test
    fun testPostSuccessNullBody() {
        val expectedResultString = "response"
        val expectedResponse = ApiResponse.Success(expectedResultString)
        val url = "url"
        val contentType = ContentType.APPLICATION_JSON
        setupHttpClient(
            MockEngine {
                respond(
                    content = expectedResultString,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.Post(
                        url = url,
                        body = null,
                        contentType = contentType,
                    ),
                )
            assertEquals(expectedResponse, actualResponse)
        }
    }

    @Test
    fun testPostResponseFailureThrown() {
        val errorString = "api response error"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON
        setupHttpClient(
            MockEngine {
                respond(
                    content = errorString,
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.Post(
                        url = url,
                        body = body,
                        contentType = contentType,
                    ),
                )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.Unauthorized.value, failureResponse.status)
            assert(failureResponse.error is ApiResponseException)
        }
    }

    @Test
    fun testPostExceptionThrown() {
        val errorMessage = "api response error"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON
        setupHttpClient(
            MockEngine {
                throw IllegalStateException(errorMessage)
            },
        )

        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.Post(
                        url = url,
                        body = body,
                        contentType = contentType,
                    ),
                )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.TransportError.value, failureResponse.status)
            assert(failureResponse.error is IllegalStateException)
        }
    }

    @Test
    fun testPostNullContentTypeExceptionThrown() {
        val errorMessage = "api response error"
        val expectedResultString = "response"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = null
        setupHttpClient(
            MockEngine {
                respond(
                    content = expectedResultString,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.Post(
                        url = url,
                        body = body,
                        contentType = contentType,
                    ),
                )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.TransportError.value, failureResponse.status)
            assert(failureResponse.error is IllegalStateException)
        }
    }

    @Test
    fun testFormUrlEncodedSuccess() {
        val expectedResultString = "response"
        val expectedResponse = ApiResponse.Success(expectedResultString)
        val url = "url"
        setupHttpClient(
            MockEngine {
                respond(
                    content = expectedResultString,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.FormUrlEncoded(
                        url = url,
                        params = listOf(Pair("key", "value")),
                    ),
                )
            assertEquals(expectedResponse, actualResponse)
        }
    }

    @Test
    fun testFormUrlEncodedResponseFailureThrown() {
        val errorString = "api response error"
        val url = "url"
        setupHttpClient(
            MockEngine {
                respond(
                    content = errorString,
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.FormUrlEncoded(
                        url = url,
                        params = listOf(Pair("key", "value")),
                    ),
                )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.Unauthorized.value, failureResponse.status)
            assert(failureResponse.error is ApiResponseException)
        }
    }

    @Test
    fun testFormUrlEncodedExceptionThrown() {
        val errorMessage = "api response error"
        val url = "url"
        setupHttpClient(
            MockEngine {
                throw IllegalStateException(errorMessage)
            },
        )

        runBlocking {
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.FormUrlEncoded(
                        url = url,
                        params = listOf(Pair("key", "value")),
                    ),
                )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.TransportError.value, failureResponse.status)
            assert(failureResponse.error is IllegalStateException)
        }
    }
}
