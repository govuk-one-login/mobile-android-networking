package uk.gov.android.network.service

import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

/**
 * [NetworkingService] makes HTTP requests and returns success or failure.
 *
 * Supports appending authentication, attestation, and DPoP headers to requests.
 *
 * @sample networkingServiceSample
 *
 * @see [RequestConfigBuilder]
 */
interface NetworkingService {
    /**
     * Make an HTTP request
     *
     * @sample networkingServiceSample
     *
     * @param apiRequest The base API request
     * @param configure Configure extra behaviour such as authentication, attestation and DPoP

     * @return ApiResponse<String, NetworkingException> The API response or error.
     *   Successful responses include the raw response body
     */
    suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit = { },
    ): ApiResponse<String, NetworkingException>
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun networkingServiceSample(networkingService: NetworkingService) {
    val request =
        ApiRequest.Get(
            url = "https://example.gov.uk",
            headers =
                listOf(
                    "x-example" to "example header",
                ),
        )

    val response =
        networkingService.makeRequest(request) {
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
                is ApiResponseException,
                is ConfigurationException,
                is ServiceException,
                is TransportException,
                -> {
                    // Handle any errors
                }
            }
        }
    }
}
