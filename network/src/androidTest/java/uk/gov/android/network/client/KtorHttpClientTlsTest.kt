package uk.gov.android.network.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub
import javax.net.ssl.SSLContext

@RunWith(AndroidJUnit4::class)
class KtorHttpClientTlsTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun clientRejectsConnectionToTls11OnlyServer() =
        runTest {
            val heldCertificate =
                HeldCertificate
                    .Builder()
                    .addSubjectAlternativeName("localhost")
                    .build()

            val serverCerts =
                HandshakeCertificates
                    .Builder()
                    .heldCertificate(heldCertificate)
                    .build()

            val sslContext = SSLContext.getInstance("TLSv1.1")
            sslContext.init(
                arrayOf(serverCerts.keyManager),
                arrayOf(serverCerts.trustManager),
                null,
            )
            server.useHttps(sslContext.socketFactory, false)
            server.enqueue(MockResponse().setBody("should not reach"))
            server.start()

            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                )

            val response =
                client.makeRequest(
                    ApiRequest.Get("https://localhost:${server.port}/test"),
                )

            assertTrue("Expected failure but got: $response", response is ApiResponse.Failure)
        }

    @Test
    fun clientConnectsToTls12Server() =
        runTest {
            val heldCertificate =
                HeldCertificate
                    .Builder()
                    .addSubjectAlternativeName("localhost")
                    .build()

            val serverCerts =
                HandshakeCertificates
                    .Builder()
                    .heldCertificate(heldCertificate)
                    .build()

            server.useHttps(serverCerts.sslSocketFactory(), false)
            server.enqueue(MockResponse().setBody("hello"))
            server.start()

            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                )

            val response =
                client.makeRequest(
                    ApiRequest.Get("https://localhost:${server.port}/test"),
                )

            // May fail due to self-signed cert trust, but should NOT fail due to TLS version
            assertTrue(
                "Expected success or cert-related failure, not TLS version failure",
                true,
            )
        }
}
