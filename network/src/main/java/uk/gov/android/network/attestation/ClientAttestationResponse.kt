package uk.gov.android.network.attestation

/**
 * Represents the result of fetching a client attestation.
 *
 * @see [ClientAttestationProvider]
 */
sealed class ClientAttestationResponse {
    /**
     * A successful client attestation result.
     *
     * @param clientAttestation the client attestation JWT
     * @param attestationPop the client attestation proof of possession JWT
     */
    data class Success(
        val clientAttestation: String,
        val attestationPop: String,
    ) : ClientAttestationResponse()

    /**
     * A failed client attestation result.
     *
     * @param error the exception that caused the failure
     */
    data class Failure(
        val error: Exception,
    ) : ClientAttestationResponse()
}
