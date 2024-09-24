package uk.gov.android.network.api

sealed class ApiResponse {
    data class Success<T>(val response: T) : ApiResponse()

    data class Failure(
        val reason: ApiFailureReason,
        val status: Int,
        val error: Exception
    ) : ApiResponse()
}
