package uk.gov.android.network.useragent

@Suppress("LongParameterList")
interface UserAgentGenerator {

    fun setUserAgent(
        userAgent: UserAgent
    )

    fun getUserAgent(): String
}

class UserAgentGeneratorImpl : UserAgentGenerator {
    private lateinit var userAgentString: String

    override fun setUserAgent(
        userAgent: UserAgent
    ) {
        val agent = UserAgentUtil.buildAgent(
            userAgent
        )
        userAgentString = agent
    }

    override fun getUserAgent() = userAgentString
}

@Suppress("EmptyFunctionBlock")
class UserAgentGeneratorStub(private val stubValue: String) : UserAgentGenerator {
    override fun setUserAgent(
        userAgent: UserAgent
    ) {
        // No Action needed as this is a stub implementation
    }

    override fun getUserAgent() = stubValue
}
