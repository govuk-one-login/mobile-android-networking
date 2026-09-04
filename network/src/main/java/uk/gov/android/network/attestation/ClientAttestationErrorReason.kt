package uk.gov.android.network.attestation

/**
 * The reason why it was not possible to create a client attestation or associated proof-of-possession.
 */
enum class ClientAttestationErrorReason {
    /**
     * There is doubt about the integrity of the app or device
     */
    APP_CHECK_FAILED,

    /**
     * An internal server error has occurred somewhere along the app integrity chain.
     *
     * That is, either the One Login backend or the Google Play integrity service is unavailable.
     */
    INTERMITTENT,

    /**
     * All the other unspecified errors
     */
    GENERIC
}
