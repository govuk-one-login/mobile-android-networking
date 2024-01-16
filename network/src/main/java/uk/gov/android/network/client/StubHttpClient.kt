package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse

class StubHttpClient(
    private val apiResponse: ApiResponse
) : GenericHttpClient {
    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse {
        return apiResponse
    }
}
