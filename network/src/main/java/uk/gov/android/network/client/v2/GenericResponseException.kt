package uk.gov.android.network.client.v2

/**
 * Represents an unsuccessful HTTP response
 *
 * @property exception the underlying exception from the HTTP client
 * @property response the non-success HTTP response
 */
abstract class GenericResponseException(
    val exception: Exception,
) : IllegalStateException(
        exception.message,
        exception.cause,
    ) {
    abstract val response: GenericHttpResponse
}
