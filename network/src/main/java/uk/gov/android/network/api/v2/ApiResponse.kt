package uk.gov.android.network.api.v2

/**
 * The result of an [ApiRequest]
 */
sealed interface ApiResponse<out T> {
    /**
     * @property response the response body
     * @property status the HTTP status code
     */
    data class Success<T>(
        val response: T,
        val status: Int,
    ) : ApiResponse<T>

    /**
     * @property error the cause of the failure
     * @property status the HTTP status code, or null if no response was received (e.g. transport error)
     */
    data class Failure(
        val error: NetworkingException,
        val status: Int? = null,
    ) : ApiResponse<Nothing>
}
