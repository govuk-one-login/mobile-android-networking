package uk.gov.android.network.client.headers

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import uk.gov.android.network.dpop.TestDPoPProvider
import uk.gov.android.network.dpop.dpopFailure
import uk.gov.android.network.dpop.dpopSuccess
import uk.gov.android.network.service.ConfigurationException
import uk.gov.android.network.service.ServiceException
import uk.gov.android.network.util.expectFailure
import uk.gov.android.network.util.expectSuccess

class RefreshDPoPHeaderReaderTest {
    private val provider = TestDPoPProvider()
    private val headerReader = RefreshDPoPHeaderReader(provider)

    @Test
    fun `given provider is null, getHeader returns missing provider failure`() =
        runTest {
            val headerReader = RefreshDPoPHeaderReader(null)

            val result = headerReader.getHeader()

            assertInstanceOf(ConfigurationException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns failure, getHeader returns dpop failure`() =
        runTest {
            provider.response = dpopFailure

            val result = headerReader.getHeader()

            assertInstanceOf(ServiceException::class.java, result.expectFailure())
        }

    @Test
    fun `given provider returns success, getHeader returns dpop header`() =
        runTest {
            val result = headerReader.getHeader()

            assertEquals("DPoP" to "dpop-proof-jwt", result.expectSuccess())
        }

    @Test
    fun `given successful dpop response, toDPoPHeader formats header`() =
        runTest {
            val result = dpopSuccess.toDPoPHeader()

            assertEquals("DPoP" to "dpop-proof-jwt", result)
        }
}
