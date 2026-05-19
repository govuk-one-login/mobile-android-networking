package uk.gov.android.network.auth

val authenticationSuccess =
    AuthenticationResponse.Success(
        bearerToken = "bearer-token",
    )

val authenticationFailure =
    AuthenticationResponse.Failure(
        error = Exception("authentication failed"),
    )
