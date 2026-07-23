package uk.gov.android.network.service.v2

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v3.ApiResponseAssertions.expectFailure
import uk.gov.android.network.api.v3.ApiResponseAssertions.expectSuccess
import uk.gov.android.network.client.v2.GenericHttpResponse
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.client.v2.StubHttpClient
import uk.gov.android.network.service.ApiResponseException
import uk.gov.android.network.service.TransportException
import uk.gov.android.network.service.v2.NetworkServiceTypedFailureExt.makeRequest

class NetworkServiceTypedFailureExtTest {
    private val httpClient = StubHttpClient()
    private val networkService = DefaultNetworkService(httpClient = httpClient)
    private val request = ApiRequest.Get(url = "https://example.gov.uk")
    private val failureStatus = 500

    @Test
    fun `given valid json response, makeRequest returns parsed success`() =
        runTest {
            httpClient.response = GenericHttpResponse(
                status = 200,
                body = """{"subject":"Test","message":"Hello"}""",
            )

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val success = result.expectSuccess()
            assertEquals(SuccessData("Test", "Hello"), success.body)
            assertEquals(200, success.status)
        }

    @Test
    fun `given failure response with valid json, makeRequest returns parsed failure`() =
        runTest {
            givenFailureResponse()

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val failure = result.expectFailure()
            assertEquals(FailureData(123), failure.body)
            assertEquals(failureStatus, failure.status)
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `given failure response with unparseable json, makeRequest returns failure with null response`() =
        runTest {
            givenFailureResponse("""{"unexpected":"structure"}""")

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val failure = result.expectFailure()
            assertNull(failure.body)
            assertEquals(failureStatus, failure.status)
            assertEquals(
                "Failed to parse response body as class uk.gov.android.network.service.v2.FailureData",
                failure.error.message,
            )
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `given failure response with invalid json, makeRequest returns failure with null response`() =
        runTest {
            givenFailureResponse("not json at all")

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val failure = result.expectFailure()
            assertNull(failure.body)
            assertEquals(failureStatus, failure.status)
            assertEquals(
                "Failed to parse response body as class uk.gov.android.network.service.v2.FailureData",
                failure.error.message,
            )
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `given transport failure, makeRequest returns failure with null response`() =
        runTest {
            httpClient.exception = IOException("connection failed")

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val failure = result.expectFailure()
            assertNull(failure.body)
            assertNull(failure.status)
            assertInstanceOf(TransportException::class.java, failure.error)
        }

    @Test
    fun `given failure response with unknown key, makeRequest returns parsed failure`() =
        runTest {
            givenFailureResponse("""{"errorCode":123,"extra":"unknown"}""")

            val result = networkService.makeRequest<SuccessData, FailureData>(request)

            val failure = result.expectFailure()
            assertEquals(FailureData(123), failure.body)
            assertEquals(failureStatus, failure.status)
        }

    @Test
    fun `when Json is provided, makeRequest uses the given implementation for failure parsing`() =
        runTest {
            val strictJson = Json { ignoreUnknownKeys = false }
            givenFailureResponse("""{"errorCode":123,"extra":"unknown"}""")

            val result = networkService.makeRequest<SuccessData, FailureData>(
                request,
                json = strictJson,
            )

            val failure = result.expectFailure()
            assertNull(failure.body)
            assertInstanceOf(ApiResponseException::class.java, failure.error)
        }

    @Test
    fun `sample runs`() = runTest {
        givenFailureResponse()
        networkServiceParseFailureSample(request, networkService)
    }

    private fun givenFailureResponse(
        body: String = """{"errorCode":123}""",
    ) {
        httpClient.exception = GenericResponseException(
            response = GenericHttpResponse(
                status = failureStatus,
                body = body,
            ),
            cause = IllegalStateException("$failureStatus"),
        )
    }

}

@Serializable
private data class SuccessData(
    val subject: String,
    val message: String,
)

@Serializable
private data class FailureData(
    val errorCode: Int,
)
