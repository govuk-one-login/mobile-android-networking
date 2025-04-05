package uk.gov.android.network.client

import io.ktor.client.plugins.logging.Logger

class NoOpLogger : Logger {
    override fun log(message: String) = Unit // Do nothing
}
