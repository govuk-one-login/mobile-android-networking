package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthProvider

class StubHttpClient(
    private val apiResponse: ApiResponse
) : GenericHttpClient {
    var lastAuthProviderSupplied: AuthProvider? = null

    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse {
        return apiResponse
    }

    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest.Post<*>,
        scope: String
    ): ApiResponse {
        return apiResponse
    }

    override fun setAuthProvider(authProvider: AuthProvider) {
        this.lastAuthProviderSupplied = authProvider
    }
}
