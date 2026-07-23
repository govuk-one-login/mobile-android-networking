package uk.gov.android.network.service

import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse as ApiResponseV2
import uk.gov.android.network.api.v3.ApiResponse
import uk.gov.android.network.attestation.ClientAttestationProvider
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.client.v2.GenericHttpClient
import uk.gov.android.network.service.v2.DefaultNetworkService as DefaultNetworkServiceV2
import uk.gov.android.network.service.v2.NetworkService as NetworkServiceV2
import uk.gov.android.network.dpop.DPoPProvider
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

/**
 * Default [NetworkService] implementation.
 *
 * To enable authentication, provide an [AuthenticationProvider] using [setAuthenticationProvider].
 * To enable client attestation headers, provide a [ClientAttestationProvider] using [setClientAttestationProvider].
 * To enable demonstrating proof-of-possession (DPoP) headers, provide a [DPoPProvider] using [setDPoPProvider].
 *
 * @sample defaultNetworkServiceSample
 */
@Deprecated(
    "Migrate to v2. To be removed on 23rd September 2026 (DCMAW-21647)",
    replaceWith = ReplaceWith(
        "uk.gov.android.network.service.v2.DefaultNetworkService",
    )
)
class DefaultNetworkService(
    private val delegate: DefaultNetworkServiceV2
) : NetworkService {
    constructor(
        httpClient: GenericHttpClient
    ) : this(
        DefaultNetworkServiceV2(httpClient)
    )

    override suspend fun makeRequest(
        apiRequest: ApiRequest,
        configure: RequestConfigBuilder.() -> Unit,
    ): ApiResponseV2<String, NetworkingException> =
        delegate.makeRequest(apiRequest, configure)
            .toApiResponseV2()

    fun setAuthenticationProvider(authenticationProvider: AuthenticationProvider?) =
        delegate.setAuthenticationProvider(authenticationProvider)

    fun setClientAttestationProvider(clientAttestationProvider: ClientAttestationProvider?) =
        delegate.setClientAttestationProvider(clientAttestationProvider)

    fun setDPoPProvider(dpopProvider: DPoPProvider?) =
        delegate.setDPoPProvider(dpopProvider)

    private fun NetworkServiceV2.RawApiResponse.toApiResponseV2() = when (this) {
        is ApiResponse.Failure<String, NetworkingException> -> ApiResponseV2.Failure(error, status)
        is ApiResponse.Success<String> -> ApiResponseV2.Success(body, status)
    }
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
