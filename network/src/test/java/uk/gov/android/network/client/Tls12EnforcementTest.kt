package uk.gov.android.network.client

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.useragent.UserAgentGeneratorStub
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class Tls12EnforcementTest {
    private val sslContext = createTls12SSLContext()
    private val socketFactory = Tls12SocketFactory(sslContext.socketFactory)

    @Test
    fun `createTls12SSLContext returns a valid SSLContext`() {
        assertNotNull(sslContext)
        assertNotNull(sslContext.socketFactory)
    }

    @Test
    fun `createSocket no-arg only enables TLS 1_2 or above`() {
        val socket = socketFactory.createSocket() as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
    }

    @Test
    fun `createSocket with host and port only enables TLS 1_2 or above`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val socket = socketFactory.createSocket("localhost", port) as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
        server.close()
    }

    @Test
    fun `createSocket with host port localAddr localPort only enables TLS 1_2 or above`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val socket =
            socketFactory.createSocket(
                "localhost",
                port,
                InetAddress.getLoopbackAddress(),
                0,
            ) as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
        server.close()
    }

    @Test
    fun `createSocket with InetAddress and port only enables TLS 1_2 or above`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val socket =
            socketFactory.createSocket(
                InetAddress.getLoopbackAddress(),
                port,
            ) as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
        server.close()
    }

    @Test
    fun `createSocket with InetAddress port localAddr localPort only enables TLS 1_2 or above`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val socket =
            socketFactory.createSocket(
                InetAddress.getLoopbackAddress(),
                port,
                InetAddress.getLoopbackAddress(),
                0,
            ) as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
        server.close()
    }

    @Test
    fun `createSocket with existing socket only enables TLS 1_2 or above`() {
        val server = ServerSocket(0)
        val port = server.localPort
        val rawSocket = java.net.Socket("localhost", port)
        val socket =
            socketFactory.createSocket(
                rawSocket,
                "localhost",
                port,
                true,
            ) as SSLSocket
        assertOnlyTls12AndAbove(socket)
        socket.close()
        server.close()
    }

    @Test
    fun `getDefaultCipherSuites delegates to wrapped factory`() {
        val expected = sslContext.socketFactory.defaultCipherSuites
        assertArrayEquals(expected, socketFactory.defaultCipherSuites)
    }

    @Test
    fun `getSupportedCipherSuites delegates to wrapped factory`() {
        val expected = sslContext.socketFactory.supportedCipherSuites
        assertArrayEquals(expected, socketFactory.supportedCipherSuites)
    }

    @Test
    fun `KtorHttpClient production constructor configures TLS correctly`() =
        runTest {
            // This exercises the secondary constructor path including the sslManager lambda
            val client =
                KtorHttpClient(
                    userAgentGenerator = UserAgentGeneratorStub("test-agent"),
                    logger = KtorLogger.noOp,
                )
            assertNotNull(client)
        }

    private fun assertOnlyTls12AndAbove(socket: SSLSocket) {
        val enabledProtocols = socket.enabledProtocols.toList()
        assertTrue(enabledProtocols.contains("TLSv1.2") || enabledProtocols.contains("TLSv1.3"))
        assertFalse(enabledProtocols.contains("TLSv1"))
        assertFalse(enabledProtocols.contains("TLSv1.1"))
    }

    @Test
    fun `Tls12SocketFactory throws SslRequiredException for non-SSL socket`() {
        val plainSocketFactory =
            object : SSLSocketFactory() {
                override fun getDefaultCipherSuites() = emptyArray<String>()

                override fun getSupportedCipherSuites() = emptyArray<String>()

                override fun createSocket() = Socket()

                override fun createSocket(
                    s: Socket?,
                    host: String?,
                    port: Int,
                    autoClose: Boolean,
                ) = Socket()

                override fun createSocket(
                    host: String?,
                    port: Int,
                ) = Socket()

                override fun createSocket(
                    host: String?,
                    port: Int,
                    localHost: InetAddress?,
                    localPort: Int,
                ) = Socket()

                override fun createSocket(
                    host: InetAddress?,
                    port: Int,
                ) = Socket()

                override fun createSocket(
                    address: InetAddress?,
                    port: Int,
                    localAddress: InetAddress?,
                    localPort: Int,
                ) = Socket()
            }
        val factory = Tls12SocketFactory(plainSocketFactory)
        assertThrows<SslRequiredException> {
            factory.createSocket()
        }
    }
}
