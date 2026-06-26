package uk.gov.android.network.service

import kotlinx.io.IOException
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse
import uk.gov.android.network.api.v2.withHeaders
import uk.gov.android.network.attestation.ClientAttestationProvider
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.client.config.RequestConfig
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.client.headers.AttestationHeaderReader
import uk.gov.android.network.client.headers.AuthorisationHeaderReader
import uk.gov.android.network.client.headers.RefreshDPoPHeaderReader
import uk.gov.android.network.client.v2.GenericHttpClient
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.dpop.DPoPProvider
import uk.gov.android.network.http.Header
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport
import uk.gov.android.network.util.NetworkingResult

/**
 * Default [NetworkService] implementation.
 *
 * To enable authentication, provide an [AuthenticationProvider] using [setAuthenticationProvider].
 * To enable client attestation headers, provide a [ClientAttestationProvider] using [setClientAttestationProvider].
 * To enable demonstrating proof-of-possession (DPoP) headers, provide a [DPoPProvider] using [setDPoPProvider].
 *
 * @sample defaultNetworkServiceSample
 */
class DefaultNetworkService(
    private val httpClient: GenericHttpClient,
) : NetworkService {
    private var attestationHeaderReader = AttestationHeaderReader(null)
    private var authorisationHeaderReader = AuthorisationHeaderReader(null)
    private var refreshDPoPHeaderReader = RefreshDPoPHeaderReader(null)

    override suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit,
    ): ApiResponse<String, NetworkingException> {
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
            response = response.body,
            status = response.status,
        )
    }

    fun setAuthenticationProvider(authenticationProvider: AuthenticationProvider?) {
        authorisationHeaderReader = AuthorisationHeaderReader(authenticationProvider)
    }

    fun setClientAttestationProvider(clientAttestationProvider: ClientAttestationProvider?) {
        attestationHeaderReader = AttestationHeaderReader(clientAttestationProvider)
    }

    fun setDPoPProvider(dpopProvider: DPoPProvider?) {
        refreshDPoPHeaderReader = RefreshDPoPHeaderReader(dpopProvider)
    }

    private suspend fun buildExtraHeaders(config: RequestConfig): NetworkingResult<List<Header>> {
        val attestationHeaders =
            if (config.attestation) {
                when (val result = attestationHeaderReader.getHeaders()) {
                    is NetworkingResult.Failure -> return NetworkingResult.Failure(result.exception)
                    is NetworkingResult.Success -> result.value
                }
            } else {
                emptyList()
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
                when (val result = refreshDPoPHeaderReader.getHeader()) {
                    is NetworkingResult.Failure -> return NetworkingResult.Failure(result.exception)
                    is NetworkingResult.Success -> result.value
                }
            } else {
                null
            }

        return NetworkingResult.Success(
            attestationHeaders +
                listOfNotNull(
                    authHeader,
                    refreshDPoPHeader,
                ),
        )
    }

    private fun Exception.toTransportFailure(): ApiResponse.Failure<TransportException> =
        ApiResponse.Failure(
            TransportException(this),
            null,
        )

    private fun GenericResponseException.toApiResponseFailure(): ApiResponse.Failure<ApiResponseException> =
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
internal suspend fun defaultNetworkServiceSample(
    apiRequest: ApiRequest,
    httpClient: GenericHttpClient,
    authenticationProvider: AuthenticationProvider,
    clientAttestationProvider: ClientAttestationProvider,
    dPoPProvider: DPoPProvider,
) {
    val networkService = DefaultNetworkService(httpClient)

    // Set the authentication provider before making authenticated requests
    networkService.setAuthenticationProvider(authenticationProvider)

    // Set the client attestation provider before asking for client attestation headers
    networkService.setClientAttestationProvider(clientAttestationProvider)

    // Set the DPoP provider before asking for demonstrating proof-of-possession headers
    networkService.setDPoPProvider(dPoPProvider)

    networkService.makeRequest(apiRequest) {
        withAuthentication("scope")
        withAttestation = true
        withRefreshDPoP = true
    }
}
