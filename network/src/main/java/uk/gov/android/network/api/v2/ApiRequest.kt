package uk.gov.android.network.api.v2

import uk.gov.android.network.client.ContentType
import uk.gov.android.network.http.Header

/**
 * An HTTP request
 */
sealed interface ApiRequest {
    val url: String
    val headers: List<Pair<String, String>>

    /**
     * An HTTP GET request
     */
    data class Get(
        override val url: String,
        override val headers: List<Pair<String, String>> = emptyList(),
    ) : ApiRequest

    /**
     * An HTTP POST request
     *
     * @property body The request body, or null for an empty body
     * @property contentType Optional content type header
     */
    data class Post<T>(
        override val url: String,
        val body: T?,
        override val headers: List<Pair<String, String>> = emptyList(),
        val contentType: ContentType? = null,
    ) : ApiRequest

    /**
     * An HTTP POST request for form submissions
     *
     * @property params the form parameters sent as the request body
     */
    data class FormUrlEncoded(
        override val url: String,
        override val headers: List<Pair<String, String>> = emptyList(),
        val params: List<Pair<String, String>>,
    ) : ApiRequest
}

internal fun ApiRequest.withHeaders(headers: List<Header>): ApiRequest {
    val newHeaders = this.headers + headers
    return when (this) {
        is ApiRequest.FormUrlEncoded -> copy(headers = newHeaders)
        is ApiRequest.Get -> copy(headers = newHeaders)
        is ApiRequest.Post<*> -> copy(headers = newHeaders)
    }
}
