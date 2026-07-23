package uk.gov.android.network.api.v3

import uk.gov.android.network.api.v2.ApiRequest

/**
 * The result of an [ApiRequest]
 *
 * @param T the success response body type
 * @param F the failure response body type
 * @param E the exception type
 */
sealed interface ApiResponse<out T, out F, out E : Exception> {
    /**
     * @property status the HTTP status code
     * @property body the response body
     * @param T the success response body type
     */
    data class Success<T>(
        val status: Int,
        val body: T,
    ) : ApiResponse<T, Nothing, Nothing>

    /**
     * @property error the cause of the failure
     * @property status the HTTP status code, or null if no response was received (e.g. transport error)
     * @property body the response body, or null if no response was received, or it was unusable
     *
     * @param F the failure response body type
     * @param E the exception type
     */
    data class Failure<F, E : Exception>(
        val error: E,
        val status: Int? = null,
        val body: F? = null,
    ) : ApiResponse<Nothing, F, E>
}
