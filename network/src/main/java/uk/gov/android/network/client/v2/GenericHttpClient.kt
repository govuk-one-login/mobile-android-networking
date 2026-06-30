package uk.gov.android.network.client.v2

import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import uk.gov.android.network.api.v2.ApiRequest

/**
 * Low-level HTTP client that returns raw responses
 */
fun interface GenericHttpClient {
    /**
     * @throws GenericResponseException if the server returns a non-successful response
     * @throws IOException if there is a connection problem
     * @throws SerializationException if the request body is not serializable
     */
    suspend fun request(request: ApiRequest): GenericHttpResponse
}
