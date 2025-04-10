package uk.gov.android.network.log

import io.ktor.client.plugins.logging.Logger
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class KtorLoggerAdapterTest {
    @Test
    fun `KtorLoggerAdapter should delegate to custom KtorLogger`() {
        val mockLogger = mock<KtorLogger>()
        val adapter: Logger = KtorLoggerAdapter(mockLogger)
        val testMessage = "adapter test"

        adapter.log(testMessage)
        verify(mockLogger).log(testMessage)
    }
}
