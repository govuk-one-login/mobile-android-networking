package uk.gov.android.network.client

import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider

class StubHttpClient(
    private val apiResponse: ApiResponse,
) : GenericHttpClient {
    var lastAuthenticationProviderSupplied: AuthenticationProvider? = null

    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse = apiResponse

    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest,
        scope: String,
    ): ApiResponse = apiResponse

    override fun setAuthenticationProvider(provider: AuthenticationProvider) {
        this.lastAuthenticationProviderSupplied = provider
    }
}
