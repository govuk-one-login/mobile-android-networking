package uk.gov.android.network.service.v2

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v3.ApiResponseAssertions.expectFailure
import uk.gov.android.network.api.v3.ApiResponseAssertions.expectSuccess
import uk.gov.android.network.client.v2.GenericHttpResponse
import uk.gov.android.network.service.ApiResponseException
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.TransportException

class StubNetworkServiceTest {
    private val networkService = StubNetworkService()
    private val request = ApiRequest.Get(url = "https://example.com")

    @Test
    fun `given success response set, makeRequest returns success`() = runTest {
        networkService.setSuccessResponse()

        val result = networkService.makeRequest(request)

        val success = result.expectSuccess()
        assertEquals(200, success.status)
        assertEquals("success", success.body)
    }

    @Test
    fun `given success response with custom status and body, makeRequest returns matching success`() =
        runTest {
            networkService.setSuccessResponse(status = 201, body = "created")

            val result = networkService.makeRequest(request)

            val success = result.expectSuccess()
            assertEquals(201, success.status)
            assertEquals("created", success.body)
        }

    @Test
    fun `given success response with GenericHttpResponse, makeRequest returns matching success`() =
        runTest {
            val customResponse = GenericHttpResponse(202, "accepted")

            networkService.setSuccessResponse(customResponse)

            val result = networkService.makeRequest(request)

            val success = result.expectSuccess()
            assertEquals(202, success.status)
            assertEquals("accepted", success.body)
        }

    @Test
    fun `given failure response set, makeRequest returns failure with status and body`() = runTest {
        networkService.setFailureResponse()

        val result = networkService.makeRequest(request)

        val failure = result.expectFailure()
        assertInstanceOf<ApiResponseException>(failure.error)
        assertEquals(500, failure.status)
        assertEquals("error", failure.body)
    }

    @Test
    fun `given failure response with custom status and body, makeRequest returns matching failure`() =
        runTest {
            networkService.setFailureResponse(status = 403, body = "forbidden")

            val result = networkService.makeRequest(request)

            val failure = result.expectFailure()
            assertInstanceOf<ApiResponseException>(failure.error)
            assertEquals(403, failure.status)
            assertEquals("forbidden", failure.body)
        }

    @Test
    fun `given failure response with GenericHttpResponse, makeRequest returns matching failure`() =
        runTest {
            val customResponse = GenericHttpResponse(404, "not found")

            networkService.setFailureResponse(customResponse)

            val result = networkService.makeRequest(request)

            val failure = result.expectFailure()
            assertInstanceOf<ApiResponseException>(failure.error)
            assertEquals(404, failure.status)
            assertEquals("not found", failure.body)
        }

    @Test
    fun `given transport exception set, makeRequest returns transport failure`() = runTest {
        networkService.setTransportException()

        val result = networkService.makeRequest(request)

        val failure = result.expectFailure()
        assertInstanceOf<TransportException>(failure.error)
        assertNull(failure.status)
    }

    @Test
    fun `given success response, httpClient receivedRequest matches the sent request`() = runTest {
        networkService.setSuccessResponse()
        val postRequest = ApiRequest.Post(
            url = "https://example.com/data",
            body = "payload"
        )

        networkService.makeRequest(postRequest)

        assertEquals("https://example.com/data", networkService.receivedRequest?.url)
    }

    @Test
    fun `given authentication provider set to null, requesting authentication returns configuration failure`() =
        runTest {
            networkService.setSuccessResponse()
            networkService.setAuthenticationProvider(null)

            val result = networkService.makeRequest(request) {
                withAuthentication("scope")
            }

            val failure = result.expectFailure()
            assertInstanceOf<ConfigurationException>(failure.error)
        }

    @Test
    fun `given attestation provider set to null, requesting attestation returns configuration failure`() =
        runTest {
            networkService.setSuccessResponse()
            networkService.setClientAttestationProvider(null)

            val result = networkService.makeRequest(request) {
                withAttestation = true
            }

            val failure = result.expectFailure()
            assertInstanceOf<ConfigurationException>(failure.error)
        }

    @Test
    fun `given DPoP provider set to null, requesting DPoP returns configuration failure`() =
        runTest {
            networkService.setSuccessResponse()
            networkService.setDPoPProvider(null)

            val result = networkService.makeRequest(request) {
                withRefreshDPoP = true
            }

            val failure = result.expectFailure()
            assertInstanceOf<ConfigurationException>(failure.error)
        }

    @Test
    fun `given default providers, requesting authentication returns success`() = runTest {
        networkService.setSuccessResponse()

        val result = networkService.makeRequest(request) {
            withAuthentication("scope")
        }

        result.expectSuccess()
    }

    @Test
    fun `given default providers, requesting attestation returns success`() = runTest {
        networkService.setSuccessResponse()

        val result = networkService.makeRequest(request) {
            withAttestation = true
        }

        result.expectSuccess()
    }

    @Test
    fun `given default providers, requesting DPoP returns success`() = runTest {
        networkService.setSuccessResponse()

        val result = networkService.makeRequest(request) {
            withRefreshDPoP = true
        }

        result.expectSuccess()
    }
}
