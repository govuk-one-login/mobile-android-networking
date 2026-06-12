package uk.gov.android.network.client.v2

import io.ktor.client.plugins.ResponseException

/**
 * Represents an unsuccessful HTTP response
 *
 * @property response the non-success HTTP response
 * @param cause the underlying exception from the HTTP client
 */
class GenericResponseException(
    val response: GenericHttpResponse,
    cause: IllegalStateException,
) : IllegalStateException(
        cause.message,
        cause,
    ) {
    companion object {
        internal suspend fun fromKtorResponseException(exception: ResponseException): GenericResponseException =
            GenericResponseException(
                response =
                    GenericHttpResponse.fromKtorHttpResponse(
                        httpResponse = exception.response,
                    ),
                cause = exception,
            )
    }
}
