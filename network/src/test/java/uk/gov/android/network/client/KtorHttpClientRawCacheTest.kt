package uk.gov.android.network.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.api.v2.ApiRequest as ApiRequestV2
import uk.gov.android.network.client.v3.RawCacheConfig
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub

class KtorHttpClientRawCacheTest {
    @TempDir
    lateinit var directory: Path

    private val userAgent = UserAgentGeneratorStub("userAgent")

    // Not valid UTF-8 (0x82, 0xFF, 0xC0) and contains NUL, like a real DER CRL.
    private val der =
        byteArrayOf(0x30, 0x82.toByte(), 0x01, 0xE1.toByte(), 0xFF.toByte(), 0x00, 0xC0.toByte())

    private fun config() = RawCacheConfig(directory.toFile())

    private fun clientWith(engine: MockEngine) =
        KtorHttpClient(userAgent, KtorLogger.noOp, engine, config())

    private fun crlEngine(onRequest: () -> Unit = {}) = MockEngine {
        onRequest()
        respond(
            content = der,
            status = HttpStatusCode.OK,
            headers =
                headersOf(
                    HttpHeaders.ContentType to listOf("application/pkix-crl"),
                    HttpHeaders.CacheControl to listOf("max-age=3600"),
                ),
        )
    }

    @Test
    fun `existing constructor has no raw cache and fails clearly`() = runBlocking {
        val client = KtorHttpClient(userAgent, KtorLogger.noOp, crlEngine())

        val e = assertThrows<IllegalStateException> { client.get(URL) }
        assertTrue(e.message!!.contains("RawCacheConfig"))
        assertThrows<IllegalStateException> { client.cached(URL) }
        Unit
    }

    @Test
    fun `raw get returns unmodified bytes and headers`() = runBlocking {
        val response = clientWith(crlEngine()).get(URL)

        assertArrayEquals(der, response.body)
        assertEquals("max-age=3600", response.header("Cache-Control"))
    }

    @Test
    fun `raw cache persists across KtorHttpClient instances`() = runBlocking {
        var calls = 0
        clientWith(crlEngine { calls++ }).get(URL)

        val offline = clientWith(MockEngine { throw IOException("offline") })

        assertArrayEquals(der, offline.get(URL).body)
        assertEquals(1, calls)
    }

    @Test
    fun `existing json api calls are not written to the raw cache`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        HttpHeaders.CacheControl to listOf("max-age=3600"),
                    ),
            )
        }
        val client = clientWith(engine)

        val response = client.makeRequest(ApiRequest.Get(API_URL))

        assertEquals(ApiResponse.Success("{}"), response)
        assertNull(client.cached(API_URL))
        assertTrue(directory.toFile().listFiles().isNullOrEmpty())
    }

    @Test
    fun `v2 request is unaffected and still returns a string body`() = runBlocking {
        val engine = MockEngine {
            respond("hello", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val response = clientWith(engine).request(ApiRequestV2.Get(API_URL))

        assertEquals("hello", response.body)
        assertEquals(200, response.status)
    }

    private companion object {
        const val URL = "http://dvs.verifier-ca.integration.account.gov.uk/crl/example.crl"
        const val API_URL = "https://api.example.com/data"
    }
}
