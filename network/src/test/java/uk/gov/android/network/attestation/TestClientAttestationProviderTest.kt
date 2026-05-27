package uk.gov.android.network.attestation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestClientAttestationProviderTest {
    @Test
    fun `defaults to success response`() =
        runTest {
            val provider = TestClientAttestationProvider()

            val result = provider.getClientAttestation()

            assertEquals(clientAttestationSuccess, result)
        }

    @Test
    fun `returns configured failure response`() =
        runTest {
            val provider = TestClientAttestationProvider(clientAttestationFailure)

            val result = provider.getClientAttestation()

            assertEquals(clientAttestationFailure, result)
        }

    @Test
    fun `response can be changed between calls`() =
        runTest {
            val provider = TestClientAttestationProvider()
            assertEquals(clientAttestationSuccess, provider.getClientAttestation())

            provider.response = clientAttestationFailure

            assertEquals(clientAttestationFailure, provider.getClientAttestation())
        }
}
