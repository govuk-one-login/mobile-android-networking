package uk.gov.android.network.client.v2

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Wraps Ktor HTTP responses
 */
class KtorHttpResponse(
    private val httpResponse: HttpResponse,
) : GenericHttpResponse {
    override val status: Int = httpResponse.status.value

    override suspend fun body(): String = httpResponse.body()
}
