package uk.gov.android.network.api.v2

/**
 * The result of an [ApiRequest]
 */
@Deprecated(
    "Migrate to v3",
    replaceWith = ReplaceWith(
        "ApiResponse",
        "uk.gov.android.network.api.v3.ApiResponse"
    )
)
sealed interface ApiResponse<out T, out E : Exception> {
    /**
     * @property response the response body
     * @property status the HTTP status code
     */
    data class Success<T>(
        val response: T,
        val status: Int,
    ) : ApiResponse<T, Nothing>

    /**
     * @property error the cause of the failure
     * @property status the HTTP status code, or null if no response was received (e.g. transport error)
     */
    data class Failure<E : Exception>(
        val error: E,
        val status: Int? = null,
    ) : ApiResponse<Nothing, E>
}
