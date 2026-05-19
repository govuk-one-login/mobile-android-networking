package uk.gov.android.network.dpop

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestDPoPProviderTest {
    @Test
    fun `defaults to success response`() =
        runTest {
            val provider = TestDPoPProvider()

            val result = provider.getRefreshDPoP()

            assertEquals(dpopSuccess, result)
        }

    @Test
    fun `returns configured failure response`() =
        runTest {
            val provider = TestDPoPProvider(dpopFailure)

            val result = provider.getRefreshDPoP()

            assertEquals(dpopFailure, result)
        }

    @Test
    fun `response can be changed between calls`() =
        runTest {
            val provider = TestDPoPProvider()
            assertEquals(dpopSuccess, provider.getRefreshDPoP())

            provider.response = dpopFailure

            assertEquals(dpopFailure, provider.getRefreshDPoP())
        }
}
