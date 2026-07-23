package uk.gov.android.network.service.v2

import uk.gov.android.network.api.v3.ApiResponse
import uk.gov.android.network.service.NetworkingException

/**
 * [ApiResponse] containing raw response body content and [NetworkingException].
 *
 * @see [NetworkService.makeRequest]
 */
typealias NetworkServiceResponse = ApiResponse<String, String, NetworkingException>

