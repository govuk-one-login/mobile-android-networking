package uk.gov.android.network.client.headers

import uk.gov.android.network.dpop.DPoPProvider
import uk.gov.android.network.dpop.DPoPResponse
import uk.gov.android.network.http.Header
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.util.NetworkingResult

private const val DPOP_HEADER_KEY = "DPoP"

internal class RefreshDPoPHeaderReader(
    internal val dpopProvider: DPoPProvider?,
) {
    @Suppress(
        // This function uses the return early pattern for different types of failure.
        // There is only one return statement for success, which is the final statement.
        "ReturnCount",
    )
    suspend fun getHeader(): NetworkingResult<Header> {
        val provider =
            this.dpopProvider ?: return missingProviderFailure()

        val response = provider.getRefreshDPoP()
        val header =
            when (response) {
                is DPoPResponse.Failure -> return response.dpopFailure()
                is DPoPResponse.Success -> response.toDPoPHeader()
            }

        return NetworkingResult.Success(header)
    }

    private fun missingProviderFailure() =
        NetworkingResult.Failure<Header>(
            ConfigurationException("DPoPProvider not set"),
        )

    private fun DPoPResponse.Failure.dpopFailure() =
        NetworkingResult.Failure<Header>(
            ServiceException(
                "DPoP provider failed to fetch refresh DPoP proof",
                error,
            ),
        )
}

internal fun DPoPResponse.Success.toDPoPHeader(): Header = Header(DPOP_HEADER_KEY, dpop)
