package uk.gov.android.network.auth

class MockAuthProvider(val stubAuthResponse: AuthResponse) : AuthProvider {
    var spyScope: String? = null
    override suspend fun fetchBearerToken(scope: String): AuthResponse {
        this.spyScope = scope
        return stubAuthResponse
    }
}
