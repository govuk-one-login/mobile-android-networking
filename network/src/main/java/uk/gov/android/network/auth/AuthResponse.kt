package uk.gov.android.network.auth

sealed class AuthResponse {
    data class Success(val bearerToken: String) : AuthResponse()
    data class Failure(val error: Exception) : AuthResponse()
}
