package uk.gov.android.network.auth

import androidx.fragment.app.FragmentActivity

class MockAuthenticationProvider(
    val stubAuthenticationResponse: AuthenticationResponse,
) : AuthenticationProvider {
    var spyScope: String? = null

    @Deprecated(
        "Use fetchBearerToken(scope: String, fragmentActivity: FragmentActivity to allow" +
                " for refresh - aim to be fully removed on 25th of March",
        level = DeprecationLevel.WARNING,
    )
    override suspend fun fetchBearerToken(scope: String): AuthenticationResponse {
        this.spyScope = scope
        return stubAuthenticationResponse
    }

    override suspend fun fetchBearerToken(
        scope: String,
        fragmentActivity: FragmentActivity,
    ): AuthenticationResponse {
        this.spyScope = scope
        return stubAuthenticationResponse
    }
}
