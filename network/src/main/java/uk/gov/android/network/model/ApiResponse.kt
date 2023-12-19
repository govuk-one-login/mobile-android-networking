package uk.gov.android.network.model

sealed class ApiResponse {
    data class Success<T>(val response: T) : ApiResponse()
    data class Failure(val status: Int, val error: Exception) : ApiResponse()
    object Loading : ApiResponse() {
        override fun toString(): String = "ApiResponse.Loading"
    }

    object Offline : ApiResponse() {
        override fun toString(): String = "ApiResponse.Offline"
    }
}
