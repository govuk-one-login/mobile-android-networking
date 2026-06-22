package uk.gov.android.network.client.headers

import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.auth.AuthenticationResponse
import uk.gov.android.network.client.config.RequestConfig
import uk.gov.android.network.http.Header
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.util.NetworkingResult

private const val AUTH_HEADER_KEY = "Authorization"
private const val AUTH_HEADER_VALUE = "Bearer"

internal class AuthorisationHeaderReader(
    internal val authenticationProvider: AuthenticationProvider?,
) {
    suspend fun getHeader(config: RequestConfig.Authentication): NetworkingResult<Header> {
        val authenticationProvider =
            this.authenticationProvider ?: return missingProviderFailure()

        val authResponse = authenticationProvider.fetchBearerToken(config.scope)
        val header =
            when (authResponse) {
                is AuthenticationResponse.Failure -> return authResponse.authenticationFailure()
                is AuthenticationResponse.Success -> authResponse.toAuthorisationHeader()
            }

        return NetworkingResult.Success(header)
    }

    private fun missingProviderFailure() =
        NetworkingResult.Failure<Header>(
            ConfigurationException("AuthenticationProvider not set"),
        )

    private fun AuthenticationResponse.Failure.authenticationFailure() =
        NetworkingResult.Failure<Header>(
            ServiceException(
                "Authentication provider failed to fetch service token",
                error,
            ),
        )
}

internal fun AuthenticationResponse.Success.toAuthorisationHeader(): Header =
    Header(
        AUTH_HEADER_KEY,
        "$AUTH_HEADER_VALUE $bearerToken",
    )
