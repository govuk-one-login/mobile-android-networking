package uk.gov.android.network.service.v2

import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v3.ApiResponse
import uk.gov.android.network.attestation.ClientAttestationErrorReason
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.service.ApiRequestException
import uk.gov.android.network.service.ApiResponseException
import uk.gov.android.network.service.AuthenticationProviderException
import uk.gov.android.network.service.ClientAttestationException
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.DPoPException
import uk.gov.android.network.service.NetworkingException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.service.TransportException
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

/**
 * [NetworkService] makes HTTP requests and returns success or failure.
 *
 * Supports appending authentication, attestation, and DPoP headers to requests.
 *
 * To parse JSON responses to a custom response type, use [NetworkServiceTypedSuccessExt.makeRequest].
 *
 * @sample networkServiceSample
 * @sample networkServiceParseSuccessSample
 * @sample networkServiceParseFailureSample
 *
 * @see [RequestConfigBuilder]
 */
interface NetworkService {
    /**
     * Make an HTTP request and return the raw response body.
     *
     * @sample networkServiceSample
     *
     * @param apiRequest The base API request
     * @param configure Configure extra behaviour such as authentication, attestation and DPoP

     * @return ApiResponse<String, String, NetworkingException> The API response or error.
     *   Successful responses include the raw response body
     */
    suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit = { },
    ): NetworkServiceResponse
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun networkServiceSample(networkService: NetworkService) {
    val request =
        ApiRequest.Get(
            url = "https://example.gov.uk",
            headers =
                listOf(
                    "x-example" to "example header",
                ),
        )

    val response =
        networkService.makeRequest(request) {
            withAttestation = true
            withAuthentication("scope")
            withRefreshDPoP = true
        }

    when (response) {
        is ApiResponse.Success -> {
            // Parse the response
        }
        is ApiResponse.Failure -> {
            when (response.error) {
                // Received a failure or unusable response
                is ApiResponseException -> Unit

                // Connection problem
                is TransportException -> Unit

                // NetworkService failed to make the request
                is ClientAttestationException -> when (response.error.reason) {
                    ClientAttestationErrorReason.APP_CHECK_FAILED,
                    ClientAttestationErrorReason.INTERMITTENT,
                    ClientAttestationErrorReason.GENERIC -> {
                        // Handle client attestation errors
                    }
                }
                is AuthenticationProviderException,
                is DPoPException,
                is ServiceException -> Unit

                // NetworkService wasn't configured properly
                is ConfigurationException -> Unit

                // Request wasn't configured properly
                is ApiRequestException -> Unit
            }
        }
    }
}
