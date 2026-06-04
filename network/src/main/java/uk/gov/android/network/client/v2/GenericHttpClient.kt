package uk.gov.android.network.client.v2

import kotlinx.io.IOException
import uk.gov.android.network.api.v2.ApiRequest

/**
 * Low-level HTTP client that returns raw responses
 */
fun interface GenericHttpClient {
    /**
     * @throws GenericResponseException if the server returns a non-successful response
     * @throws IOException if there is a connection problem
     */
    suspend fun request(request: ApiRequest): GenericHttpResponse
}
