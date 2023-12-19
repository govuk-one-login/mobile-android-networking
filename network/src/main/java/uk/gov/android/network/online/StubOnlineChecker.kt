package uk.gov.android.network.online

import androidx.annotation.VisibleForTesting

/**
 * Stub class shared between unit and instrumentation tests.
 *
 * @param stub The value returned by the overwritten [OnlineChecker] function.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
data class StubOnlineChecker(
    private var stub: Boolean = false
) : OnlineChecker {

    override fun isOnline(): Boolean = stub

    fun setOnline(isOnline: Boolean) = apply {
        stub = isOnline
    }
}
