package uk.gov.android.network.client.headers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import uk.gov.android.network.api.v2.ConfigurationException
import uk.gov.android.network.api.v2.ServiceException
import uk.gov.android.network.auth.TestAuthenticationProvider
import uk.gov.android.network.auth.authenticationFailure
import uk.gov.android.network.auth.authenticationSuccess
import uk.gov.android.network.client.config.RequestConfig
import uk.gov.android.network.util.expectFailure
import uk.gov.android.network.util.expectSuccess

class AuthorisationHeaderReaderTest {
    private val authConfig = RequestConfig.Authentication(scope = "my-scope")
    private val provider = TestAuthenticationProvider(expectedScope = "my-scope")
    private val headerReader = AuthorisationHeaderReader(provider)

    @Test
    fun `given provider is null, getHeader returns missing provider failure`() =
        runTest {
            val headerReader = AuthorisationHeaderReader(null)

            val result = headerReader.getHeader(authConfig)

            assertInstanceOf(ConfigurationException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns failure, getHeader returns service failure`() =
        runTest {
            provider.response = authenticationFailure

            val result = headerReader.getHeader(authConfig)

            assertInstanceOf(ServiceException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns success, getHeader returns authorisation header`() =
        runTest {
            val result = headerReader.getHeader(authConfig)

            assertEquals("Authorization" to "Bearer bearer-token", result.expectSuccess())
        }

    @Test
    fun `given successful auth response, toAuthorisationHeader formats bearer token`() =
        runTest {
            val result = authenticationSuccess.toAuthorisationHeader()

            assertEquals("Authorization" to "Bearer bearer-token", result)
        }
}
