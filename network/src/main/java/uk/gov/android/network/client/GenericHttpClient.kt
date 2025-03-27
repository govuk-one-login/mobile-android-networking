package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider

/**
 * Contract for the generic HTTP client
 */
interface GenericHttpClient {
    /**
     * Make the HTTP request
     *
     * @param apiRequest The HTTP request
     * @returns ApiResponse the API response
     */
    suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse

    /**
     * Make an authorised HTTP request to a protected service
     *
     * @param apiRequest The HTTP request to the protected service
     * @param scope will be used to fetch and decorate the request with the correct token
     * @returns ApiResponse the API response
     */
    suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest,
        scope: String,
    ): ApiResponse

    /**
     * Prepares the client for authorised requests
     * @param provider the authentication provider used to exchange the scope for a bearer token
     */
    fun setAuthenticationProvider(provider: AuthenticationProvider)
}
