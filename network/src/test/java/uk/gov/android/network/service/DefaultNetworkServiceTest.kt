package uk.gov.android.network.service

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse
import uk.gov.android.network.api.v2.expectFailure
import uk.gov.android.network.attestation.TestClientAttestationProvider
import uk.gov.android.network.attestation.clientAttestationFailure
import uk.gov.android.network.auth.TestAuthenticationProvider
import uk.gov.android.network.auth.authenticationFailure
import uk.gov.android.network.client.v2.StubHttpClient
import uk.gov.android.network.client.v2.TestHttpResponse
import uk.gov.android.network.client.v2.TestResponseException
import uk.gov.android.network.dpop.TestDPoPProvider
import uk.gov.android.network.dpop.dpopFailure
import kotlin.jvm.java

class DefaultNetworkServiceTest {
    private val httpClient = StubHttpClient()
    private val networkService = DefaultNetworkService(httpClient = httpClient)
    private val request = ApiRequest.Get(url = "https://example.com")
    private val authProvider = TestAuthenticationProvider(expectedScope = SCOPE)
    private val attestationProvider = TestClientAttestationProvider()
    private val dpopProvider = TestDPoPProvider()

    @Test
    fun `given client returns response, makeRequest returns success with body and status`() =
        runTest {
            httpClient.response = TestHttpResponse.success

            val result = networkService.makeRequest(request)

            assertEquals(ApiResponse.Success("success", status = 200), result)
        }

    @Test
    fun `given client throws ResponseExceptionWrapper, makeRequest returns API response failure`() =
        runTest {
            httpClient.exception = TestResponseException.internalServerError

            val result = networkService.makeRequest(request)

            val failure = result.expectFailure()
            assertInstanceOf(ApiResponseException::class.java, failure.error)
            assertEquals(500, failure.status)
        }

    @Test
    fun `given client throws io exception, makeRequest returns transport failure`() =
        runTest {
            httpClient.exception = IOException("connection failed")

            val result = networkService.makeRequest(request)

            val failure = result.expectFailure()
            assertInstanceOf(TransportException::class.java, failure.error)
            assertEquals(null, failure.status)
        }

    @Test
    fun `given client throws SerializationException, makeRequest returns request failure`() =
        runTest {
            httpClient.exception =
                kotlinx.serialization.SerializationException("Serializer not found")

            val result = networkService.makeRequest(request)

            val failure = result.expectFailure()
            assertInstanceOf(ApiRequestException::class.java, failure.error)
            assertEquals(null, failure.status)
        }

    @Test
    fun `given authentication configured and header reader fails, makeRequest returns failure`() =
        runTest {
            authProvider.response = authenticationFailure
            networkService.setAuthenticationProvider(authProvider)
            httpClient.response = TestHttpResponse.success

            val result =
                networkService.makeRequest(request) {
                    withAuthentication(SCOPE)
                }

            val failure = result.expectFailure()
            assertInstanceOf(ServiceException::class.java, failure.error)
        }

    @Test
    fun `given authentication configured, makeRequest appends authorisation header`() =
        runTest {
            networkService.setAuthenticationProvider(authProvider)
            httpClient.response = TestHttpResponse.success

            networkService.makeRequest(request) {
                withAuthentication(SCOPE)
            }

            assertEquals(
                request.copy(
                    headers = request.headers + ("Authorization" to "Bearer bearer-token"),
                ),
                httpClient.receivedRequest,
            )
        }

    @Test
    fun `given authentication not configured, makeRequest does not add authorisation header`() =
        runTest {
            httpClient.response = TestHttpResponse.success

            networkService.makeRequest(request)

            assertEquals(request, httpClient.receivedRequest)
        }

    @Test
    fun `given attestation configured and header reader fails, makeRequest returns failure`() =
        runTest {
            attestationProvider.response = clientAttestationFailure
            networkService.setClientAttestationProvider(attestationProvider)
            httpClient.response = TestHttpResponse.success

            val result =
                networkService.makeRequest(request) {
                    withAttestation = true
                }

            val failure = result.expectFailure()
            assertInstanceOf(ServiceException::class.java, failure.error)
        }

    @Test
    fun `given attestation configured, makeRequest appends attestation headers`() =
        runTest {
            networkService.setClientAttestationProvider(attestationProvider)
            httpClient.response = TestHttpResponse.success

            networkService.makeRequest(request) {
                withAttestation = true
            }

            assertEquals(
                request.copy(
                    headers =
                        request.headers +
                            listOf(
                                "OAuth-Client-Attestation" to "client-attestation-jwt",
                                "OAuth-Client-Attestation-PoP" to "attestation-pop-jwt",
                            ),
                ),
                httpClient.receivedRequest,
            )
        }

    @Test
    fun `given refreshDPoP configured and header reader fails, makeRequest returns failure`() =
        runTest {
            dpopProvider.response = dpopFailure
            networkService.setDPoPProvider(dpopProvider)
            httpClient.response = TestHttpResponse.success

            val result =
                networkService.makeRequest(request) {
                    withRefreshDPoP = true
                }

            val failure = result.expectFailure()
            assertInstanceOf(ServiceException::class.java, failure.error)
        }

    @Test
    fun `given refreshDPoP configured, makeRequest appends DPoP header`() =
        runTest {
            networkService.setDPoPProvider(dpopProvider)
            httpClient.response = TestHttpResponse.success

            networkService.makeRequest(request) {
                withRefreshDPoP = true
            }

            assertEquals(
                request.copy(
                    headers = request.headers + ("DPoP" to "dpop-proof-jwt"),
                ),
                httpClient.receivedRequest,
            )
        }

    companion object {
        private const val SCOPE = "scope"
    }
}
