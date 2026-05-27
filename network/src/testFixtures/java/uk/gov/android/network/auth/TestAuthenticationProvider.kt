package uk.gov.android.network.auth

/**
 * A test double for [AuthenticationProvider].
 *
 * @param response the response to return from [fetchBearerToken].
 * @see authenticationSuccess
 * @see authenticationFailure
 */
class TestAuthenticationProvider(
    var response: AuthenticationResponse = authenticationSuccess,
) : AuthenticationProvider {
    override suspend fun fetchBearerToken(scope: String): AuthenticationResponse = response
}
