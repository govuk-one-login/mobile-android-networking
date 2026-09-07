package uk.gov.android.network.dpop

val dpopSuccess =
    DPoPResponse.Success(
        dpop = "dpop-proof-jwt",
    )

val dpopFailure =
    DPoPResponse.Failure(
        error = Exception("DPoP failed"),
    )
