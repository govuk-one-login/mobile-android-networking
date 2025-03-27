package uk.gov.android.network.useragent

@Suppress("LongParameterList")
object UserAgentUtil {
    /**
     * Build the user agent
     *
     * @param userAgent data class containing the user agent
     * @return The user agent
     */
    fun buildAgent(userAgent: UserAgent): String {
        userAgent.apply {
            return "$appName/$versionName $manufacturer/$model Android/$sdkVersion " +
                "$clientName/$clientVersion"
        }
    }
}
