package uk.gov.android.network.useragent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.APP_NAME
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.CLIENT_NAME
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.CLIENT_VERSION
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.MANUFACTURER
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.MODEL
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.SDK_VERSION
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.USER_AGENT
import uk.gov.android.network.useragent.UserAgentGeneratorImplTest.Companion.VERSION_NAME

class UserAgentUtilTest {
    @Test
    fun testUtil() {
        val expectedUserAgent = USER_AGENT
        val userAgent = UserAgent(
            APP_NAME,
            VERSION_NAME,
            MANUFACTURER,
            MODEL,
            SDK_VERSION,
            CLIENT_NAME,
            CLIENT_VERSION
        )
        val actualUserAgent = UserAgentUtil.buildAgent(userAgent)

        assertEquals(expectedUserAgent, actualUserAgent)
    }
}
