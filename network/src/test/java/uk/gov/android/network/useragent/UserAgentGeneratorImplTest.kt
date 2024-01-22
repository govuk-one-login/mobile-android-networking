package uk.gov.android.network.useragent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserAgentGeneratorImplTest {
    @Test
    fun verifySetUserAgent() {
        val sut = UserAgentGeneratorImpl()
        val expectedUserAgent = USER_AGENT
        sut.setUserAgent(
            APP_NAME,
            VERSION_NAME,
            MANUFACTURER,
            MODEL,
            SDK_VERSION,
            CLIENT_NAME,
            CLIENT_VERSION
        )
        val actualUserAgent = sut.getUserAgent()

        assertEquals(expectedUserAgent, actualUserAgent)
    }

    companion object {
        const val USER_AGENT = "GOV.UK Wallet/1.0.0 samsung/SM-G975F Android/31 Ktor/2.3.7"
        const val APP_NAME = "GOV.UK Wallet"
        const val VERSION_NAME = "1.0.0"
        const val MANUFACTURER = "samsung"
        const val MODEL = "SM-G975F"
        const val SDK_VERSION = 31
        const val CLIENT_NAME = "Ktor"
        const val CLIENT_VERSION = "2.3.7"
    }
}
