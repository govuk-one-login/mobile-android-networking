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
import uk.gov.android.network.api.ApiResponseException
import uk.gov.android.network.client.HttpStatusCodeExtensions.TransportError
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorHttpClientTest {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")
    private lateinit var sut: KtorHttpClient

    @Test
    fun `Get - Success`() =
        runTest {
            val expectedResultString = "api response"
            val expectedResponse = ApiResponse.Success(expectedResultString)
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = expectedResultString,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )

            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assertEquals(expectedResponse, actualResponse)
        }

    @Test
    fun `Get - Failure`() =
        runTest {
            val errorString = "api response error"
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = errorString,
                                status = HttpStatusCode.Unauthorized,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.Unauthorized.value, failureResponse.status)
            assert(failureResponse.error is ApiResponseException)
        }

    @Test
    fun `Get - Exception thrown`() =
        runTest {
            val errorMessage = "api response error"
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            throw IllegalStateException(errorMessage)
                        },
                )
            val actualResponse = sut.makeRequest(ApiRequest.Get(url))
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(HttpStatusCode.TransportError.value, failureResponse.status)
            assert(failureResponse.error is IllegalStateException)
        }

    @Test
    fun `Post - Success`() =
        runTest {
            val expectedResultString = "response"
            val expectedResponse = ApiResponse.Success(expectedResultString)
            val url = "url"
            val body = TestData("Test", "AB1234567C")
            val contentType = ContentType.APPLICATION_JSON
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = expectedResultString,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
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

    @Test
    fun `Post - Success with null body`() =
        runTest {
            val expectedResultString = "response"
            val expectedResponse = ApiResponse.Success(expectedResultString)
            val url = "url"
            val contentType = ContentType.APPLICATION_JSON
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = expectedResultString,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
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

    @Test
    fun `Post - Failure`() =
        runTest {
            val errorString = "api response error"
            val url = "url"
            val body = TestData("Test", "AB1234567C")
            val contentType = ContentType.APPLICATION_JSON
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = errorString,
                                status = HttpStatusCode.Unauthorized,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
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

    @Test
    fun `Post - Exception thrown`() =
        runTest {
            val errorMessage = "api response error"
            val url = "url"
            val body = TestData("Test", "AB1234567C")
            val contentType = ContentType.APPLICATION_JSON
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            throw IllegalStateException(errorMessage)
                        },
                )
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

    @Test
    fun `Post - NullContentTypeExceptionThrown`() =
        runTest {
            val expectedResultString = "response"
            val url = "url"
            val body = TestData("Test", "AB1234567C")
            val contentType = null
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = expectedResultString,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
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

    @Test
    fun `FormUrlEncoded - Success`() =
        runTest {
            val expectedResultString = "response"
            val expectedResponse = ApiResponse.Success(expectedResultString)
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = expectedResultString,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
            val actualResponse =
                sut.makeRequest(
                    ApiRequest.FormUrlEncoded(
                        url = url,
                        params = listOf(Pair("key", "value")),
                    ),
                )
            assertEquals(expectedResponse, actualResponse)
        }

    @Test
    fun `FormUrlEncoded - Failure`() =
        runTest {
            val errorString = "api response error"
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            respond(
                                content = errorString,
                                status = HttpStatusCode.Unauthorized,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                )
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

    @Test
    fun `FormUrlEncoded - Exception thrown`() =
        runTest {
            val errorMessage = "api response error"
            val url = "url"
            sut =
                KtorHttpClient(
                    userAgentGenerator = userAgentGenerator,
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        MockEngine {
                            throw IllegalStateException(errorMessage)
                        },
                )
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
