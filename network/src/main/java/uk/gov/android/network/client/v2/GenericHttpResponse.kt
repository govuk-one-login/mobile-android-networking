package uk.gov.android.network.client.v2

/**
 * A raw HTTP response
 */
interface GenericHttpResponse {
    val status: Int

    /**
     * @throws IllegalStateException If [body] was already called
     */
    suspend fun body(): String
}
