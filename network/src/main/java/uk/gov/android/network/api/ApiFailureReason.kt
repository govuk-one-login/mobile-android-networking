package uk.gov.android.network.api

sealed class ApiFailureReason {
    data object AccessTokenExpired : ApiFailureReason()
    data object General : ApiFailureReason()
    data object AuthProviderNotInitialised : ApiFailureReason()
    data object Non200Response : ApiFailureReason()
    data object AuthFailed : ApiFailureReason()
}
