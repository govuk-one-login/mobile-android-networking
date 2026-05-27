package uk.gov.android.network.dpop

/**
 * A test double for [DPoPProvider].
 *
 * @param response the response to return from [getRefreshDPoP].
 * @see dpopSuccess
 * @see dpopFailure
 */
class TestDPoPProvider(
    var response: DPoPResponse = dpopSuccess,
) : DPoPProvider {
    override suspend fun getRefreshDPoP(): DPoPResponse = response
}
