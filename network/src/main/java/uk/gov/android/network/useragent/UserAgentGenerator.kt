package uk.gov.android.network.useragent

@Suppress("LongParameterList")
interface UserAgentGenerator {
    /**
     * Set the user agent
     *
     * @param appName Name of the app as displayed in the play store
     * @param versionName Version of the app
     * @param manufacturer Manufacturer of the device running the app
     * @param model Model number of the device running the app
     * @param sdkVersion The current version of the Android OS
     * @param clientName The name of the third party HTTP client
     * @param clientVersion Version number of the HTTP client
     */
    fun setUserAgent(
        appName: String,
        versionName: String,
        manufacturer: String,
        model: String,
        sdkVersion: Int,
        clientName: String,
        clientVersion: String
    )

    fun getUserAgent(): String
}

class UserAgentGeneratorImpl : UserAgentGenerator {
    private lateinit var userAgent: String

    override fun setUserAgent(
        appName: String,
        versionName: String,
        manufacturer: String,
        model: String,
        sdkVersion: Int,
        clientName: String,
        clientVersion: String
    ) {
        val agent = UserAgentUtil.buildAgent(
            appName,
            versionName,
            manufacturer,
            model,
            sdkVersion,
            clientName,
            clientVersion
        )
        userAgent = agent
    }

    override fun getUserAgent() = userAgent
}

@Suppress("EmptyFunctionBlock")
class UserAgentGeneratorStub(private val stubValue: String) : UserAgentGenerator {
    override fun setUserAgent(
        appName: String,
        versionName: String,
        manufacturer: String,
        model: String,
        sdkVersion: Int,
        clientName: String,
        clientVersion: String
    ) {}

    override fun getUserAgent() = stubValue
}
