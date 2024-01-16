package uk.gov.android.network.useragent

@Suppress("LongParameterList")
object UserAgentUtil {
    /**
     * Build the user agent
     *
     * @param appName Name of the app as displayed in the play store
     * @param versionName Version of the app
     * @param manufacturer Manufacturer of the device running the app
     * @param model Model number of the device running the app
     * @param sdkVersion The current version of the Android OS
     * @param clientName The name of the third party HTTP client
     * @param clientVersion Version number of the HTTP client
     * @return The user agent
     */
    fun buildAgent(
        appName: String,
        versionName: String,
        manufacturer: String,
        model: String,
        sdkVersion: Int,
        clientName: String,
        clientVersion: String
    ): String {
        return "$appName/$versionName $manufacturer/$model Android/$sdkVersion " +
            "$clientName/$clientVersion"
    }
}
