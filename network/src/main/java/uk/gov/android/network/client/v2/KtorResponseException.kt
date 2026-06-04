package uk.gov.android.network.client.v2

import io.ktor.client.plugins.ResponseException

/**
 * Wraps Ktor HTTP response exceptions (thrown for 3XX, 4XX and 5XX status codes)
 */
class KtorResponseException(
    private val responseException: ResponseException,
) : GenericResponseException(
        responseException,
    ) {
    override val response: GenericHttpResponse
        get() =
            KtorHttpResponse(responseException.response)
}
