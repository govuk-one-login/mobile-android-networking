package uk.gov.android.network.log

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class KtorLoggerTest {
    @Test
    fun `NoOp logger should not throw exception`() {
        val logger = KtorLogger.noOp
        Assertions.assertDoesNotThrow {
            logger.log("This should do nothing")
        }
    }

    @Test
    fun `Simple logger should print message to stdout`() {
        val logger = KtorLogger.simple
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            val testMessage = "Test message"
            logger.log(testMessage)
            val output = outputStream.toString().trim()
            Assertions.assertEquals("HttpClient: $testMessage", output)
        } finally {
            System.setOut(originalOut)
        }
    }
}
