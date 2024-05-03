package uk.gov.android.network.auth

interface AuthenticationProvider {
    suspend fun fetchBearerToken(scope: String): AuthenticationResponse
}
