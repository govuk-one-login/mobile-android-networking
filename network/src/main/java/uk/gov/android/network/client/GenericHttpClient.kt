package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthProvider

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
     * Make an authorised HTTP Post request to a protected service
     *
     * @param apiRequest The HTTP request to the protected service
     * @param scope will be used to fetch and decorate the request with the correct token
     * @returns ApiResponse the API response
     */
    suspend fun makeAuthorisedRequest(apiRequest: ApiRequest.Post<*>, scope: String): ApiResponse

    /**
     * Prepares the http client for authorised requests
     * @param authProvider the provider used to exchange scope with a bearer token
     */
    fun setAuthProvider(authProvider: AuthProvider)
}
