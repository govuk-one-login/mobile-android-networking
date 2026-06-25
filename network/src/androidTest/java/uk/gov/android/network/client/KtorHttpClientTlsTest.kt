package uk.gov.android.network.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.engine.android.Android
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
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Instrumentation tests verifying that [Tls12SocketFactory] prevents connections to servers
 * that only support TLS 1.1 or below.
 *
 * These tests exercise [createTls12Engine] with a custom [javax.net.ssl.X509TrustManager] and
 * [javax.net.ssl.HostnameVerifier] so the client trusts the local MockWebServer's self-signed
 * certificate. The TLS enforcement logic is identical to production.
 *
 * Note: These tests should ideally be run on Android 10–14 (API 29–34) devices. On Android 15+
 * the platform blocks TLS 1.1 regardless, so the tests will pass even without our enforcement.
 */
@RunWith(AndroidJUnit4::class)
class KtorHttpClientTlsTest {
    private lateinit var server: MockWebServer
    private lateinit var heldCertificate: HeldCertificate
    private lateinit var serverCerts: HandshakeCertificates
    private lateinit var clientCerts: HandshakeCertificates

    @Before
    fun setUp() {
        server = MockWebServer()
        heldCertificate =
            HeldCertificate
                .Builder()
                .addSubjectAlternativeName("localhost")
                .build()
        serverCerts =
            HandshakeCertificates
                .Builder()
                .heldCertificate(heldCertificate)
                .build()
        clientCerts =
            HandshakeCertificates
                .Builder()
                .addTrustedCertificate(heldCertificate.certificate)
                .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun clientRejectsConnectionToTls11OnlyServer() =
        runTest {
            server.useHttps(tls11OnlySocketFactory(), false)
            server.enqueue(MockResponse().setBody("should not reach"))
            server.start()

            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        createKtorAndroidEngine(
                            trustManager = clientCerts.trustManager,
                            hostnameVerifier = HostnameVerifier { _, _ -> true },
                        ),
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
            server.useHttps(serverCerts.sslSocketFactory(), false)
            server.enqueue(MockResponse().setBody("hello"))
            server.start()

            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        createKtorAndroidEngine(
                            trustManager = clientCerts.trustManager,
                            hostnameVerifier = HostnameVerifier { _, _ -> true },
                        ),
                )

            val response =
                client.makeRequest(
                    ApiRequest.Get("https://localhost:${server.port}/test"),
                )

            assertTrue(
                "Expected success but got: $response",
                response is ApiResponse.Success<*>,
            )
        }

    @Test
    fun insecureClientAcceptsConnectionToTls11OnlyServer() =
        runTest {
            server.useHttps(tls11OnlySocketFactory(), false)
            server.enqueue(MockResponse().setBody("connected via tls 1.1"))
            server.start()

            // Engine without TLS version enforcement
            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                    ktorClientEngine =
                        Android.create {
                            sslManager = { connection ->
                                connection.sslSocketFactory =
                                    clientCerts.sslSocketFactory()
                                connection.hostnameVerifier = { _, _ -> true }
                            }
                        },
                )

            val response =
                client.makeRequest(
                    ApiRequest.Get("https://localhost:${server.port}/test"),
                )

            assertTrue(
                "Expected success (proving TLS 1.1 works without enforcement) but got: $response",
                response is ApiResponse.Success<*>,
            )
        }

    private fun tls11OnlySocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
            arrayOf(serverCerts.keyManager),
            arrayOf(serverCerts.trustManager),
            null,
        )
        val delegate = sslContext.socketFactory
        return object : SSLSocketFactory() {
            override fun getDefaultCipherSuites() = delegate.defaultCipherSuites

            override fun getSupportedCipherSuites() = delegate.supportedCipherSuites

            override fun createSocket() = (delegate.createSocket() as SSLSocket).restrictToTls11()

            override fun createSocket(
                s: Socket?,
                host: String?,
                port: Int,
                autoClose: Boolean,
            ) = (delegate.createSocket(s, host, port, autoClose) as SSLSocket)
                .restrictToTls11()

            override fun createSocket(
                host: String?,
                port: Int,
            ) = (delegate.createSocket(host, port) as SSLSocket).restrictToTls11()

            override fun createSocket(
                host: String?,
                port: Int,
                localHost: InetAddress?,
                localPort: Int,
            ) = (delegate.createSocket(host, port, localHost, localPort) as SSLSocket)
                .restrictToTls11()

            override fun createSocket(
                host: InetAddress?,
                port: Int,
            ) = (delegate.createSocket(host, port) as SSLSocket).restrictToTls11()

            override fun createSocket(
                address: InetAddress?,
                port: Int,
                localAddress: InetAddress?,
                localPort: Int,
            ) = (delegate.createSocket(address, port, localAddress, localPort) as SSLSocket)
                .restrictToTls11()

            private fun SSLSocket.restrictToTls11() = apply { enabledProtocols = arrayOf("TLSv1.1") }
        }
    }
}
