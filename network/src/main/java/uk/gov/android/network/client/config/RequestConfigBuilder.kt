package uk.gov.android.network.client.config

class RequestConfigBuilder {
    /**
     * Attach a demonstrated proof of possesion (DPoP) header to the request
     */
    var withRefreshDPoP = false

    /**
     * Attach an attestation header to the request
     */
    var withAttestation = false

    internal var authentication: RequestConfig.Authentication? = null

    /**
     * Attach an authorisation header with scoped access token
     *
     * @param scope The scope of the required access token
     */
    fun withAuthentication(scope: String) {
        authentication =
            RequestConfig.Authentication(
                scope = scope,
            )
    }

    internal fun build(): RequestConfig =
        RequestConfig(
            refreshDPoP = withRefreshDPoP,
            attestation = withAttestation,
            authentication = authentication,
        )
}
