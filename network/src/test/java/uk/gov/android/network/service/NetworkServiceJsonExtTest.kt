package uk.gov.android.network.service

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponseAssertions.expectFailure
import uk.gov.android.network.api.v2.ApiResponseAssertions.expectSuccess
import uk.gov.android.network.client.v2.GenericHttpResponse
import uk.gov.android.network.client.v2.StubHttpClient
import uk.gov.android.network.service.NetworkServiceJsonExt.makeRequest

class NetworkServiceJsonExtTest {
    private val httpClient = StubHttpClient()
    private val networkService = DefaultNetworkService(httpClient = httpClient)
    private val request = ApiRequest.Get(url = "https://example.com")
    private val customStrictJson = Json { ignoreUnknownKeys = false }

    @Test
    fun `given valid json response, makeRequest returns parsed object`() =
        runTest {
            httpClient.response = GenericHttpResponse(status = 200, body = """{"subject":"Test","message":"Hello"}""")

            val result = networkService.makeRequest<TestData>(request)

            val success = result.expectSuccess()
            assertEquals(TestData("Test", "Hello"), success.response)
            assertEquals(200, success.status)
        }

    @Test
    fun `given valid json response with unknown key, makeRequest returns parsed object`() =
        runTest {
            httpClient.response = GenericHttpResponse(
                status = 200,
                body = """{"subject":"Test","message":"Hello","new":"Hello"}""".trimIndent(),
            )

            val result = networkService.makeRequest<TestData>(request)

            val success = result.expectSuccess()
            assertEquals(TestData("Test", "Hello"), success.response)
            assertEquals(200, success.status)
        }

    @Test
    fun `when Json is provided, makeRequest uses the given implementation`() =
        runTest {
            // First check that our custom Json is stricter
            assertTrue(
                customStrictJson.configuration.ignoreUnknownKeys !=
                        NetworkServiceJsonExt.jsonDecoder.configuration.ignoreUnknownKeys
            )
            httpClient.response = GenericHttpResponse(
                status = 200,
                body = """{"subject":"Test","message":"Hello","new":"Hello"}""".trimIndent(),
            )

            val result = networkService.makeRequest<TestData>(request, json = customStrictJson)

            val failure = result.expectFailure()
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `given upstream failure, makeRequest returns failure`() =
        runTest {
            httpClient.exception = IOException("connection failed")

            val result = networkService.makeRequest<TestData>(request)

            val failure = result.expectFailure()
            assertInstanceOf(TransportException::class.java, failure.error)
        }

    @Test
    fun `given invalid json, makeRequest returns api response failure`() =
        runTest {
            httpClient.response = GenericHttpResponse(status = 200, body = "not json")

            val result = networkService.makeRequest<TestData>(request)

            val failure = result.expectFailure()
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `given json with wrong structure, makeRequest returns api response failure`() =
        runTest {
            httpClient.response = GenericHttpResponse(status = 200, body = """{"unexpected":"structure"}""")

            val result = networkService.makeRequest<TestData>(request)

            val failure = result.expectFailure()
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }
}

@Serializable
private data class TestData(
    val subject: String,
    val message: String,
)
