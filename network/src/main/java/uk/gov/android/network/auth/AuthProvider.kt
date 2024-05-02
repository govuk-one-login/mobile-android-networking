package uk.gov.android.network.auth

interface AuthProvider {
    suspend fun fetchBearerToken(scope: String): AuthResponse
}
