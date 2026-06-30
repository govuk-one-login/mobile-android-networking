package uk.gov.android.network.auth

/**
 * A test double for [AuthenticationProvider].
 *
 * @param response the response to return from [fetchBearerToken].
 * @param expectedScope the scope that must be passed to [fetchBearerToken].
 * @see authenticationSuccess
 * @see authenticationFailure
 */
class TestAuthenticationProvider(
    var response: AuthenticationResponse = authenticationSuccess,
    var expectedScope: String? = null,
) : AuthenticationProvider {
    override suspend fun fetchBearerToken(scope: String): AuthenticationResponse {
        if (expectedScope != null && scope != expectedScope) {
            return AuthenticationResponse.Failure(Exception("unexpected scope $scope"))
        }
        return response
    }
}
