package uk.gov.android.network.attestation

/**
 * Provides client attestation tokens for network requests.
 *
 * Consumers implement this interface to supply a client attestation JWT and its
 * associated proof of possession, enabling app integrity verification on network calls.
 */
fun interface ClientAttestationProvider {
    suspend fun getClientAttestation(): ClientAttestationResponse
}
