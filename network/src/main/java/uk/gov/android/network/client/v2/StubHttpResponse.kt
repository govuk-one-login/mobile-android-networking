package uk.gov.android.network.client.v2

internal data class StubHttpResponse(
    override val status: Int = 200,
    private val responseBody: String = "",
) : GenericHttpResponse {
    override suspend fun body(): String = responseBody
}
