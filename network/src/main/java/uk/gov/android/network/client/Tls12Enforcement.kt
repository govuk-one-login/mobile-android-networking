package uk.gov.android.network.client

import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

private val TLS_12_AND_ABOVE = arrayOf("TLSv1.2", "TLSv1.3")

internal fun createTls12SSLContext(): SSLContext =
    SSLContext.getInstance("TLSv1.2").apply {
        init(null, null, null)
    }

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
        localHost: java.net.InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(host, port, localHost, localPort).enforceTls12()

    override fun createSocket(
        host: java.net.InetAddress?,
        port: Int,
    ): Socket = delegate.createSocket(host, port).enforceTls12()

    override fun createSocket(
        address: java.net.InetAddress?,
        port: Int,
        localAddress: java.net.InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(address, port, localAddress, localPort).enforceTls12()

    private fun Socket.enforceTls12(): Socket =
        apply {
            if (this is SSLSocket) {
                enabledProtocols = supportedProtocols.filter { it in TLS_12_AND_ABOVE }.toTypedArray()
            }
        }
}
