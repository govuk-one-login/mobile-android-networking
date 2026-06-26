package uk.gov.android.network.client

import io.ktor.client.engine.android.AndroidEngineConfig
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

private val TLS_12_AND_ABOVE = arrayOf("TLSv1.2", "TLSv1.3")

/**
 * TLS 1.0 and 1.1 are deprecated and blocked by default on Android 15+ (API 35) for apps
 * targeting that version. However, on Android 10–14 (API 29–34) the platform still permits
 * these older protocols. This enforcement ensures TLS 1.2+ is required on all supported
 * Android versions.
 *
 * @see <a href="https://developer.android.com/about/versions/15/behavior-changes-15#restricted-tls-versions">
 *     Android 15 behaviour changes - Restricted TLS versions</a>
 */

internal fun createTls12SSLContext(): SSLContext =
    SSLContext.getInstance("TLSv1.2").apply {
        init(null, null, null)
    }

/**
 * Configures the [AndroidEngineConfig] SSL manager to enforce TLS 1.2+ via [Tls12SocketFactory].
 *
 * @param trustManager Optional custom trust manager. Intended only for testing with
 *   self-signed certificates (e.g. MockWebServer). Production callers should use the
 *   no-arg overload which uses the system default trust.
 * @param hostnameVerifier Optional custom hostname verifier. Intended only for testing
 *   against localhost. Production callers should use the no-arg overload which uses the
 *   system default hostname verification.
 */
internal fun AndroidEngineConfig.configureSslManagerMinTls12(
    trustManager: X509TrustManager? = null,
    hostnameVerifier: HostnameVerifier? = null,
) {
    sslManager = { connection ->
        val sslContext = createTls12SSLContext()
        if (trustManager != null) {
            sslContext.init(null, arrayOf(trustManager), null)
        }
        connection.sslSocketFactory = Tls12SocketFactory(sslContext.socketFactory)
        if (hostnameVerifier != null) {
            connection.hostnameVerifier = hostnameVerifier
        }
    }
}

/**
 * An [SSLSocketFactory] wrapper that restricts enabled protocols to TLS 1.2 and TLS 1.3.
 *
 * The default SSL context on Android does not restrict which protocols sockets can negotiate —
 * they can still use TLS 1.0/1.1. The protocol must be explicitly restricted at the socket
 * level via [SSLSocket.setEnabledProtocols].
 */
internal class Tls12SocketFactory(
    private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(): Socket = delegate.createSocket().enforceTls12()

    override fun createSocket(
        s: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket = delegate.createSocket(s, host, port, autoClose).enforceTls12()

    override fun createSocket(
        host: String?,
        port: Int,
    ): Socket = delegate.createSocket(host, port).enforceTls12()

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(host, port, localHost, localPort).enforceTls12()

    override fun createSocket(
        host: InetAddress?,
        port: Int,
    ): Socket = delegate.createSocket(host, port).enforceTls12()

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(address, port, localAddress, localPort).enforceTls12()

    private fun Socket.enforceTls12(): Socket =
        apply {
            if (this !is SSLSocket) {
                throw SslRequiredException(
                    "Only SSL connections are permitted. Received ${this::class.simpleName}.",
                )
            }
            enabledProtocols = supportedProtocols.filter { it in TLS_12_AND_ABOVE }.toTypedArray()
        }
}

internal class SslRequiredException(
    message: String,
) : SSLException(message)
