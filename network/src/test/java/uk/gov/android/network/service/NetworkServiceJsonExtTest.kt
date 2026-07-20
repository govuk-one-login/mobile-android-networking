package uk.gov.android.network.service

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
