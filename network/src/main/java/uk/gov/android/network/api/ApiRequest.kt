package uk.gov.android.network.api

import uk.gov.android.network.client.ContentType

sealed class ApiRequest(
    var apiHeaders: List<Pair<String, String>> = emptyList()
) {
    data class Get(
        val url: String,
        val headers: List<Pair<String, String>> = emptyList()
    ) : ApiRequest(headers)

    data class Post<T>(
        val url: String,
        val body: T?,
        val headers: List<Pair<String, String>> = emptyList(),
        val contentType: ContentType? = null
    ) : ApiRequest(headers)

    data class FormUrlEncoded(
        val url: String,
        val params: List<Pair<String, String>>,
        val headers: List<Pair<String, String>> = emptyList()
    ) : ApiRequest(headers)
}
