package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider

class StubHttpClient(
    private val apiResponse: ApiResponse
) : GenericHttpClient {
    var lastAuthenticationProviderSupplied: AuthenticationProvider? = null

    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse {
        return apiResponse
    }

    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest.Post<*>,
        scope: String
    ): ApiResponse {
        return apiResponse
    }

    override fun setAuthenticationProvider(provider: AuthenticationProvider) {
        this.lastAuthenticationProviderSupplied = provider
    }
}
