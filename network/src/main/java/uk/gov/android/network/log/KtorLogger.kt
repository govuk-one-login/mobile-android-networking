package uk.gov.android.network.log

fun interface KtorLogger {
    fun log(message: String)

    object NoOp : KtorLogger {
        override fun log(message: String) = Unit // Do nothing
    }

    object Simple : KtorLogger {
        override fun log(message: String) {
            println("HttpClient: $message")
        }
    }
}
