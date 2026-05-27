package uk.gov.android.network.attestation

val clientAttestationSuccess =
    ClientAttestationResponse.Success(
        clientAttestation = "client-attestation-jwt",
        attestationPop = "attestation-pop-jwt",
    )

val clientAttestationFailure =
    ClientAttestationResponse.Failure(
        error = Exception("client attestation failed"),
    )
