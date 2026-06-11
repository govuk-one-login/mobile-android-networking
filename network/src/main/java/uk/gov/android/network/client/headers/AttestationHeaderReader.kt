package uk.gov.android.network.client.headers

import uk.gov.android.network.attestation.ClientAttestationProvider
import uk.gov.android.network.attestation.ClientAttestationResponse
import uk.gov.android.network.http.Header
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.util.NetworkingResult

private const val ATTESTATION_HEADER_KEY = "OAuth-Client-Attestation"
private const val ATTESTATION_POP_HEADER_KEY = "OAuth-Client-Attestation-PoP"

internal class AttestationHeaderReader(
    internal val clientAttestationProvider: ClientAttestationProvider?,
) {
    @Suppress(
        // This function uses the return early pattern for different types of failure.
        // There is only one return statement for success, which is the final statement.
        "ReturnCount",
    )
    suspend fun getHeaders(): NetworkingResult<List<Header>> {
        val provider =
            this.clientAttestationProvider ?: return missingProviderFailure()

        val response = provider.getClientAttestation()
        val headers =
            when (response) {
                is ClientAttestationResponse.Failure -> return response.attestationFailure()
                is ClientAttestationResponse.Success -> response.toAttestationHeaders()
            }

        return NetworkingResult.Success(headers)
    }

    private fun missingProviderFailure() =
        NetworkingResult.Failure<List<Header>>(
            ConfigurationException("ClientAttestationProvider not set"),
        )

    private fun ClientAttestationResponse.Failure.attestationFailure() =
        NetworkingResult.Failure<List<Header>>(
            ServiceException(
                "Attestation provider failed to fetch client attestation",
                error,
            ),
        )
}

internal fun ClientAttestationResponse.Success.toAttestationHeaders(): List<Header> =
    listOf(
        Header(ATTESTATION_HEADER_KEY, clientAttestation),
        Header(ATTESTATION_POP_HEADER_KEY, attestationPop),
    )
