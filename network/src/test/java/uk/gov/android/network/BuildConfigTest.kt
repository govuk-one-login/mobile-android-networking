package uk.gov.android.network

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildConfigTest {
    @Test
    fun `KTOR_VERSION variable`() {
        val actual = BuildConfig.KTOR_VERSION
        assertTrue(actual.isNotEmpty())
    }
}
