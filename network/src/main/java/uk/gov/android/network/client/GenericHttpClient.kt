package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse

/**
 * Contract for the generic HTTP client
 */
fun interface GenericHttpClient {

    /**
     * Make the HTTP request
     *
     * @param apiRequest The HTTP request
     * @returns ApiResponse the API response
     */
    suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse
}
