package uk.gov.android.network.service.v2

import java.io.IOException
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.attestation.ClientAttestationProvider
import uk.gov.android.network.attestation.TestClientAttestationProvider
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.auth.TestAuthenticationProvider
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.client.v2.GenericHttpResponse
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.client.v2.StubHttpClient
import uk.gov.android.network.client.v2.TestHttpResponse
import uk.gov.android.network.dpop.DPoPProvider
import uk.gov.android.network.dpop.TestDPoPProvider

/**
 * Test stub for [NetworkService].
 *
 * Unlike [DefaultNetworkService], this test double is preconfigured with provider dependencies
 * needed for features such as authentication.
 */
class StubNetworkService(
    val httpClient: StubHttpClient = StubHttpClient(),
    val testAuthenticationProvider: TestAuthenticationProvider = TestAuthenticationProvider(),
    val testClientAttestationProvider: TestClientAttestationProvider =
        TestClientAttestationProvider(),
    val testDPoPProvider: TestDPoPProvider = TestDPoPProvider()
) : NetworkService {
    private val delegate = DefaultNetworkService(httpClient).apply {
        setAuthenticationProvider(testAuthenticationProvider)
        setClientAttestationProvider(testClientAttestationProvider)
        setDPoPProvider(testDPoPProvider)
    }

    val receivedRequest: ApiRequest? get() = httpClient.receivedRequest

    override suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit
    ): NetworkServiceResponse = delegate.makeRequest(apiRequest, configure)

    fun setAuthenticationProvider(authenticationProvider: AuthenticationProvider?) {
        delegate.setAuthenticationProvider(authenticationProvider)
    }

    fun setClientAttestationProvider(clientAttestationProvider: ClientAttestationProvider?) {
        delegate.setClientAttestationProvider(clientAttestationProvider)
    }

    fun setDPoPProvider(dpopProvider: DPoPProvider?) {
        delegate.setDPoPProvider(dpopProvider)
    }

    fun setSuccessResponse(
        status: Int = TestHttpResponse.success.status,
        body: String = TestHttpResponse.success.body
    ) = setSuccessResponse(
        GenericHttpResponse(status, body)
    )

    fun setSuccessResponse(response: GenericHttpResponse = TestHttpResponse.success) {
        httpClient.response = response
    }

    fun setFailureResponse(
        status: Int = TestHttpResponse.internalServerError.status,
        body: String = TestHttpResponse.internalServerError.body
    ) = setFailureResponse(
        GenericHttpResponse(status, body)
    )

    fun setFailureResponse(response: GenericHttpResponse = TestHttpResponse.internalServerError) {
        httpClient.exception = GenericResponseException(
            response,
            IllegalStateException("Status $response.status")
        )
    }

    fun setTransportException() {
        httpClient.exception = IOException("Connection error")
    }
}
