package uk.gov.android.network.dpop

/**
 * Represents the result of fetching a DPoP proof.
 *
 * @see [DPoPProvider]
 */
sealed class DPoPResponse {
    /**
     * A successful DPoP result.
     *
     * @param dpop the DPoP proof JWT
     */
    data class Success(
        val dpop: String,
    ) : DPoPResponse()

    /**
     * A failed DPoP result.
     *
     * @param error the exception that caused the failure
     */
    data class Failure(
        val error: Exception,
    ) : DPoPResponse()
}
