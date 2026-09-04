package uk.gov.android.network.attestation

val clientAttestationSuccess =
    ClientAttestationResponse.Success(
        clientAttestation = "client-attestation-jwt",
        attestationPop = "attestation-pop-jwt",
    )

val clientAttestationFailure =
    ClientAttestationResponse.Failure(
        reason = ClientAttestationErrorReason.GENERIC,
        error = Exception("client attestation failed"),
    )

val clientAttestationIntermittentFailure =
    ClientAttestationResponse.Failure(
        reason = ClientAttestationErrorReason.INTERMITTENT,
        error = Exception("intermittent client attestation failure"),
    )

val clientAttestationAppCheckFailure =
    ClientAttestationResponse.Failure(
        reason = ClientAttestationErrorReason.APP_CHECK_FAILED,
        error = Exception("app check failed"),
    )
