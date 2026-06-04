package uk.gov.android.network.client.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequestConfigBuilderTest {
    private val builder = RequestConfigBuilder()
    private val defaultConfig =
        RequestConfig(
            refreshDPoP = false,
            attestation = false,
            authentication = null,
        )

    @Test
    fun `given nothing set, build returns defaults`() {
        val config = builder.build()

        assertEquals(defaultConfig, config)
    }

    @Test
    fun `given withRefreshDPoP true, build has refreshDPoP enabled`() {
        builder.withRefreshDPoP = true

        val config = builder.build()

        assertEquals(defaultConfig.copy(refreshDPoP = true), config)
    }

    @Test
    fun `given withAttestation true, build has attestation enabled`() {
        builder.withAttestation = true

        val config = builder.build()

        assertEquals(defaultConfig.copy(attestation = true), config)
    }

    @Test
    fun `given withAuthentication called, build has authentication`() {
        val expectedAuthConfig = RequestConfig.Authentication(scope = "my-scope")
        builder.withAuthentication("my-scope")

        val config = builder.build()

        assertEquals(
            defaultConfig.copy(
                authentication = expectedAuthConfig,
            ),
            config,
        )
    }
}
