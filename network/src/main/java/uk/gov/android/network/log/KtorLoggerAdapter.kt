package uk.gov.android.network.log

internal class KtorLoggerAdapter(
    private val logger: KtorLogger,
) : io.ktor.client.plugins.logging.Logger {
    override fun log(message: String) {
        logger.log(message)
    }
}
