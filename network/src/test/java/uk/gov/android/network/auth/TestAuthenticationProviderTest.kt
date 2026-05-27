package uk.gov.android.network.auth

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestAuthenticationProviderTest {
    @Test
    fun `defaults to success response`() =
        runTest {
            val provider = TestAuthenticationProvider()

            val result = provider.fetchBearerToken("scope")

            assertEquals(authenticationSuccess, result)
        }

    @Test
    fun `returns configured failure response`() =
        runTest {
            val provider = TestAuthenticationProvider(authenticationFailure)

            val result = provider.fetchBearerToken("scope")

            assertEquals(authenticationFailure, result)
        }

    @Test
    fun `response can be changed between calls`() =
        runTest {
            val provider = TestAuthenticationProvider()
            assertEquals(authenticationSuccess, provider.fetchBearerToken("scope"))

            provider.response = authenticationFailure

            assertEquals(authenticationFailure, provider.fetchBearerToken("scope"))
        }
}
