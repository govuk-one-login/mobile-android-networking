package uk.gov.android.network.service

import kotlinx.io.IOException
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse
import uk.gov.android.network.api.v2.ApiResponseException
import uk.gov.android.network.api.v2.TransportException
import uk.gov.android.network.api.v2.withHeaders
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.client.config.RequestConfig
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.client.headers.AuthorisationHeaderReader
import uk.gov.android.network.client.v2.GenericHttpClient
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.http.Header
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport
import uk.gov.android.network.util.NetworkingResult

/**
 * Default [NetworkingService] implementation.
 *
 * To enable authentication, provide an [AuthenticationProvider] using [setAuthenticationProvider].
 *
 * @sample defaultNetworkingServiceSample
 */
class DefaultNetworkingService(
    private val httpClient: GenericHttpClient,
) : NetworkingService {
    private var authorisationHeaderReader = AuthorisationHeaderReader(null)

    @Suppress(
        // This function uses the return early pattern for different types of failure.
        // There is only one return statement for success, which is the final statement.
        "ReturnCount",
    )
    override suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit,
    ): ApiResponse<String> {
        val config = RequestConfigBuilder().apply { configure() }.build()

        val extraHeaders =
            when (val result = buildExtraHeaders(config)) {
                is NetworkingResult.Failure -> {
                    return ApiResponse.Failure(result.exception)
                }
                is NetworkingResult.Success -> result.value
            }

        val apiRequest = apiRequest.withHeaders(extraHeaders)

        val response =
            try {
                httpClient.request(apiRequest)
            } catch (exception: GenericResponseException) {
                // Unsuccessful (3XX, 4XX, 5XX) response
                return exception.toApiResponseFailure()
            } catch (exception: IOException) {
                return exception.toTransportFailure()
            }

        // Successful (1XX or 2XX) response
        return ApiResponse.Success(
            response = response.body(),
            status = response.status,
        )
    }

    fun setAuthenticationProvider(authenticationProvider: AuthenticationProvider?) {
        authorisationHeaderReader = AuthorisationHeaderReader(authenticationProvider)
    }

    private suspend fun buildExtraHeaders(config: RequestConfig): NetworkingResult<List<Header>> {
        val attestationHeader =
            if (config.attestation) {
                error("Not yet implemented")
            } else {
                null
            }

        val authHeader =
            if (config.authentication != null) {
                when (
                    val result = authorisationHeaderReader.getHeader(config.authentication)
                ) {
                    is NetworkingResult.Failure -> return NetworkingResult.Failure(result.exception)
                    is NetworkingResult.Success -> result.value
                }
            } else {
                null
            }

        val refreshDPoPHeader =
            if (config.refreshDPoP) {
                error("Not yet implemented")
            } else {
                null
            }

        return NetworkingResult.Success(
            listOfNotNull(
                attestationHeader,
                authHeader,
                refreshDPoPHeader,
            ),
        )
    }

    private fun Exception.toTransportFailure(): ApiResponse.Failure =
        ApiResponse.Failure(
            TransportException(this),
            null,
        )

    private fun GenericResponseException.toApiResponseFailure(): ApiResponse.Failure =
        ApiResponse.Failure(
            status = response.status,
            error =
                ApiResponseException(
                    "API responded with ${response.status}",
                    this,
                ),
        )
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun defaultNetworkingServiceSample(
    apiRequest: ApiRequest,
    httpClient: GenericHttpClient,
    authenticationProvider: AuthenticationProvider,
) {
    val networkingService = DefaultNetworkingService(httpClient)

    // Set the authentication provider before making authenticated requests
    networkingService.setAuthenticationProvider(authenticationProvider)

    networkingService.makeRequest(apiRequest) {
        withAuthentication("scope")
    }
}
