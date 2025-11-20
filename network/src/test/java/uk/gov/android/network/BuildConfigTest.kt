package uk.gov.android.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildConfigTest {
    @Test
    fun `KTOR_VERSION variable`() {
        val expected = "3.3.2"
        val actual = BuildConfig.KTOR_VERSION

        assertEquals(expected, actual)
    }
}
