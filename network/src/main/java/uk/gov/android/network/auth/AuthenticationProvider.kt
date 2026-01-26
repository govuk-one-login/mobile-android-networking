package uk.gov.android.network.auth

import androidx.fragment.app.FragmentActivity

interface AuthenticationProvider {
    @Deprecated(
        "Use fetchBearerToken(scope: String, fragmentActivity: FragmentActivity to allow" +
            " for refresh - aim to be fully removed on 25th of March",
        level = DeprecationLevel.WARNING,
    )
    /**
     * It authorizes requests to a protected endpoint by obtaining a service token.
     * @param scope will be used to fetch and decorate the request with the correct token
     * @return [AuthenticationResponse] response from the service token endpoint
     */
    suspend fun fetchBearerToken(scope: String): AuthenticationResponse

    /**
     * It authorizes requests to a protected endpoint by obtaining a service token.
     * @param scope will be used to fetch and decorate the request with the correct token
     * @param fragmentActivity will be used to enable exchange for tokens required to retrieve a new service token
     * @return [AuthenticationResponse] response from the service token endpoint
     */
    suspend fun fetchBearerToken(
        scope: String,
        fragmentActivity: FragmentActivity,
    ): AuthenticationResponse
}
