package uk.gov.android.network.useragent

/**
 * The user agent
 *
 * @param appName Name of the app as displayed in the play store
 * @param versionName Version of the app
 * @param manufacturer of the device running the app
 * @param model number of the device running the app
 * @param sdkVersion The current version of the Android OS
 * @param clientName The name of the third party HTTP client
 * @param clientVersion Version number of the HTTP client
 */
data class UserAgent(
    val appName: String,
    val versionName: String,
    val manufacturer: String,
    val model: String,
    val sdkVersion: Int,
    val clientName: String,
    val clientVersion: String,
)
