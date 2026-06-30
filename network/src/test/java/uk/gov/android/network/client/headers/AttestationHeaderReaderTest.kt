package uk.gov.android.network.client.headers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import uk.gov.android.network.attestation.TestClientAttestationProvider
import uk.gov.android.network.attestation.clientAttestationFailure
import uk.gov.android.network.attestation.clientAttestationSuccess
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.util.expectFailure
import uk.gov.android.network.util.expectSuccess
import kotlin.jvm.java

class AttestationHeaderReaderTest {
    private val provider = TestClientAttestationProvider()
    private val headerReader = AttestationHeaderReader(provider)

    @Test
    fun `given provider is null, getHeaders returns missing provider failure`() =
        runTest {
            val headerReader = AttestationHeaderReader(null)

            val result = headerReader.getHeaders()

            assertInstanceOf(ConfigurationException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns failure, getHeaders returns attestation failure`() =
        runTest {
            provider.response = clientAttestationFailure

            val result = headerReader.getHeaders()

            assertInstanceOf(ServiceException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns success, getHeaders returns attestation headers`() =
        runTest {
            val result = headerReader.getHeaders()

            assertEquals(
                listOf(
                    "OAuth-Client-Attestation" to "client-attestation-jwt",
                    "OAuth-Client-Attestation-PoP" to "attestation-pop-jwt",
                ),
                result.expectSuccess(),
            )
        }

    @Test
    fun `given successful attestation response, toAttestationHeaders formats headers`() =
        runTest {
            val result = clientAttestationSuccess.toAttestationHeaders()

            assertEquals(
                listOf(
                    "OAuth-Client-Attestation" to "client-attestation-jwt",
                    "OAuth-Client-Attestation-PoP" to "attestation-pop-jwt",
                ),
                result,
            )
        }
}
