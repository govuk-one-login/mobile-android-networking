package uk.gov.android.network.client.v2

import uk.gov.android.network.api.v2.ApiRequest

/**
 * [GenericHttpClient] that can be configured with a pre-defined response or exception to throw,
 * which may be useful for testing.
 */
class StubHttpClient : GenericHttpClient {
    var response: GenericHttpResponse? = null
    var exception: Exception? = null

    /**
     * The most recent request sent to [request]
     */
    var receivedRequest: ApiRequest? = null

    override suspend fun request(request: ApiRequest): GenericHttpResponse {
        receivedRequest = request
        exception?.let { throw it }
        return response!!
    }
}
