package uk.gov.android.network.api

@Deprecated(
    message = "Migrate to V3. To be removed on 19th January 2027 (DCMAW-24874).",
    replaceWith = ReplaceWith(
        "ApiResponse",
        "uk.gov.android.network.api.v3.ApiResponse",
    ),
)
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
