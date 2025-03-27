package uk.gov.android.network.auth

class MockAuthenticationProvider(
    val stubAuthenticationResponse: AuthenticationResponse,
) : AuthenticationProvider {
    var spyScope: String? = null

    override suspend fun fetchBearerToken(scope: String): AuthenticationResponse {
        this.spyScope = scope
        return stubAuthenticationResponse
    }
}
