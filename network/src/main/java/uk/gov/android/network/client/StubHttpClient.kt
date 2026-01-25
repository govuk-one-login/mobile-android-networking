package uk.gov.android.network.client

import androidx.fragment.app.FragmentActivity
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider

class StubHttpClient(
    private val apiResponse: ApiResponse,
) : GenericHttpClient {
    var lastAuthenticationProviderSupplied: AuthenticationProvider? = null

    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse = apiResponse

    @Deprecated(
        "Use makeAuthorisedRequest with fragmentActivity to allow for authentication - aim to be fully removed on ",
        level = DeprecationLevel.WARNING,
    )
    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest,
        scope: String,
    ): ApiResponse = apiResponse

    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest,
        scope: String,
        fragmentActivity: FragmentActivity,
    ): ApiResponse = apiResponse

    override fun setAuthenticationProvider(provider: AuthenticationProvider) {
        this.lastAuthenticationProviderSupplied = provider
    }
}
