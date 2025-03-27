package uk.gov.android.network.online

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

class OnlineCheckerImplTest {
    private val connectivityManager: ConnectivityManager = mock()
    private val network: Network = mock()
    private val networkCapabilities: NetworkCapabilities = mock()
    private val checker = OnlineCheckerImpl(connectivityManager)

    @BeforeEach
    fun setUp() {
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(eq(network)))
            .thenReturn(networkCapabilities)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getValidTransportTypes")
    fun checksSubsetOfTransportTypes(networkCapability: Int) {
        checker.isOnline()
        verify(networkCapabilities).hasTransport(networkCapability)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getInvalidTransportTypes")
    fun doesNotCheckASubsetTransportTypes(networkCapability: Int) {
        checker.isOnline()
        verify(networkCapabilities, never()).hasTransport(networkCapability)
    }

    @Test
    fun testNullNetworkCapabilities() {
        whenever(connectivityManager.getNetworkCapabilities(eq(network)))
            .thenReturn(null)
        assert(!checker.isOnline())
    }

    companion object {
        @JvmStatic
        fun getInvalidTransportTypes(): Stream<Arguments> =
            Stream.of(
                arguments(
                    Named.named(
                        "Transport: Bluetooth",
                        NetworkCapabilities.TRANSPORT_BLUETOOTH,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: Low-Power Wireless Personal Area Network",
                        NetworkCapabilities.TRANSPORT_LOWPAN,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: USB",
                        NetworkCapabilities.TRANSPORT_USB,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: VPN",
                        NetworkCapabilities.TRANSPORT_VPN,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: WiFi awareness",
                        NetworkCapabilities.TRANSPORT_WIFI_AWARE,
                    ),
                ),
            )

        @JvmStatic
        fun getValidTransportTypes(): Stream<Arguments> =
            Stream.of(
                arguments(
                    Named.named(
                        "Transport: Cellular",
                        NetworkCapabilities.TRANSPORT_CELLULAR,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: Ethernet",
                        NetworkCapabilities.TRANSPORT_ETHERNET,
                    ),
                ),
                arguments(
                    Named.named(
                        "Transport: WiFi",
                        NetworkCapabilities.TRANSPORT_WIFI,
                    ),
                ),
            )
    }
}
