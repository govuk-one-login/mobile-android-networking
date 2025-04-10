package uk.gov.android.network.log

fun interface KtorLogger {
    fun log(message: String)

    companion object {
        val noOp = KtorLogger { } // Do nothing
        val simple = KtorLogger { message -> println("HttpClient: $message") }
    }
}
