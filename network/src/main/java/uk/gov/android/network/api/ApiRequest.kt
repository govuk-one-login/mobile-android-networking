package uk.gov.android.network.api

import uk.gov.android.network.client.ContentType

@Deprecated(
    message = "Migrate to V2. To be removed on 19th January 2027 (DCMAW-XXXXX).",
    replaceWith = ReplaceWith(
        "ApiRequest",
        "uk.gov.android.network.api.v2.ApiRequest",
    ),
)
sealed class ApiRequest {
    data class Get(val url: String, val headers: List<Pair<String, String>> = emptyList()) :
        ApiRequest()

    data class Post<T>(
        val url: String,
        val body: T?,
        val headers: List<Pair<String, String>> = emptyList(),
        val contentType: ContentType? = null,
    ) : ApiRequest()

    data class FormUrlEncoded(
        val url: String,
        val headers: List<Pair<String, String>> = emptyList(),
        val params: List<Pair<String, String>>,
    ) : ApiRequest()
}
