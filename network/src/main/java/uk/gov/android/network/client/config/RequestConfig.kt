package uk.gov.android.network.client.config

internal data class RequestConfig(
    val refreshDPoP: Boolean,
    val attestation: Boolean,
    val authentication: Authentication?,
) {
    internal data class Authentication(
        val scope: String,
    )
}
