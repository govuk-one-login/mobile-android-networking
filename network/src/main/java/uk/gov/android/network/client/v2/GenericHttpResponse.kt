package uk.gov.android.network.client.v2

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * A raw HTTP response
 */
data class GenericHttpResponse(
    val status: Int,
    val body: String,
) {
    companion object {
        /**
         * @throws IllegalStateException If this [HttpResponse] was already consumed (see [HttpResponse.body])
         */
        internal suspend fun fromKtorHttpResponse(httpResponse: HttpResponse): GenericHttpResponse =
            GenericHttpResponse(
                status = httpResponse.status.value,
                body = httpResponse.body(),
            )
    }
}
