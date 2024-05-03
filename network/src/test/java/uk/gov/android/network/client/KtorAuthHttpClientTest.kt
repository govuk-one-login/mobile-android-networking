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
import uk.gov.android.network.auth.AuthResponse.Failure
import uk.gov.android.network.auth.AuthResponse.Success
import uk.gov.android.network.auth.MockAuthProvider
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorAuthHttpClientTest {
    private val userAgentGenerator = UserAgentGeneratorStub("userAgent")

    private var sut = KtorHttpClient(userAgentGenerator)

    @OptIn(ExperimentalSerializationApi::class)
    private fun setupHttpClient(engine: MockEngine) {
        val httpClient = HttpClient(engine) {
            expectSuccess = true

            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    val responseException = exception as? ResponseException
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
                    }
                )
            }
        }
        sut.setHttpClient(httpClient)
    }

    @Test
    fun testSetAuthProvider() {
        val expectedScope = "scope"
        val expectedResultString = "response"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON

        val mockEngine = MockEngine {
            respond(
                content = expectedResultString,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupHttpClient(
            mockEngine
        )
        val expectedBearerToken = "ExpectedBearerToken"
        val newMockAuthProvider = MockAuthProvider(Success(expectedBearerToken))
        sut.setAuthProvider(newMockAuthProvider)
        runBlocking {
            sut.makeAuthorisedRequest(
                ApiRequest.Post(
                    url = url,
                    body = body,
                    contentType = contentType
                ),
                expectedScope
            )
            assertEquals(expectedScope, newMockAuthProvider.spyScope)
            assertEquals(mockEngine.requestHistory.size, 1)
            val headers = mockEngine.requestHistory.first().headers
            assertEquals(headers.get("Authorization"), "Bearer $expectedBearerToken")
        }
    }

    @Test
    fun testMakeAuthorisedRequest_Success() {
        val expectedScope = "scope"
        val expectedResultString = "response"
        val expectedResponse = ApiResponse.Success(expectedResultString)
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON

        val mockEngine = MockEngine {
            respond(
                content = expectedResultString,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupHttpClient(mockEngine)
        val expectedBearerToken = "ExpectedBearerToken"
        val mockAuthProvider = MockAuthProvider(Success(expectedBearerToken))
        sut.setAuthProvider(mockAuthProvider)
        runBlocking {
            val actualResponse = sut.makeAuthorisedRequest(
                ApiRequest.Post(
                    url = url,
                    body = body,
                    contentType = contentType
                ),
                expectedScope
            )
            assertEquals(expectedResponse, actualResponse)
            assertEquals(expectedScope, mockAuthProvider.spyScope)
            assertEquals(mockEngine.requestHistory.size, 1)
            val headers = mockEngine.requestHistory.first().headers
            assertEquals(headers.get("Authorization"), "Bearer $expectedBearerToken")
        }
    }

    @Test
    fun testMakeAuthorisedRequest_FailAuthProviderNotSet() {
        val expectedScope = "scope"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON

        runBlocking {
            val actualResponse = sut.makeAuthorisedRequest(
                ApiRequest.Post(
                    url = url,
                    body = body,
                    contentType = contentType
                ),
                expectedScope
            )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(0, failureResponse.status)
            assertEquals(
                "Service Token Provider not initialised",
                failureResponse.error.localizedMessage
            )
        }
    }

    @Test
    fun testMakeAuthorisedRequest_FailAuthProviderFailedToFetch() {
        val expectedScope = "scope"
        val url = "url"
        val body = TestData("Test", "AB1234567C")
        val contentType = ContentType.APPLICATION_JSON
        val error = Exception("Failed to get token")
        val mockAuthProvider = MockAuthProvider(Failure(error))
        sut.setAuthProvider(mockAuthProvider)
        runBlocking {
            val actualResponse = sut.makeAuthorisedRequest(
                ApiRequest.Post(
                    url = url,
                    body = body,
                    contentType = contentType
                ),
                expectedScope
            )
            assert(actualResponse is ApiResponse.Failure)
            val failureResponse = actualResponse as ApiResponse.Failure
            assertEquals(0, failureResponse.status)
            assertEquals(error, failureResponse.error)
        }
    }
}
