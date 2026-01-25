package uk.gov.android.network.auth

import androidx.fragment.app.FragmentActivity

interface AuthenticationProvider {
    @Deprecated(
        "Use fetchBearerToken(scope: String, fragmentActivity: FragmentActivity to allow" +
                " for refresh - aim to be fully removed on 25th of March",
        level = DeprecationLevel.WARNING,
    )
    suspend fun fetchBearerToken(scope: String): AuthenticationResponse

    suspend fun fetchBearerToken(
        scope: String,
        fragmentActivity: FragmentActivity,
    ): AuthenticationResponse
}
