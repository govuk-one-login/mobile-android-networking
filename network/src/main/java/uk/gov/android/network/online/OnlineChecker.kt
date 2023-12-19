package uk.gov.android.network.online

/**
 * Abstraction for defining whether a User's device is online.
 */
fun interface OnlineChecker {

    /**
     * Check whether the User's mobile device currently has access to the Internet.
     *
     * @return true when it's possible to perform external API calls.
     */
    fun isOnline(): Boolean
}
