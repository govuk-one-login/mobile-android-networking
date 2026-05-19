package uk.gov.android.network.attestation

/**
 * A test double for [ClientAttestationProvider].
 *
 * @param response the response to return from [getClientAttestation].
 * @see clientAttestationSuccess
 * @see clientAttestationFailure
 */
class TestClientAttestationProvider(
    var response: ClientAttestationResponse = clientAttestationSuccess,
) : ClientAttestationProvider {
    override suspend fun getClientAttestation(): ClientAttestationResponse = response
}
