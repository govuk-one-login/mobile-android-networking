package uk.gov.android.network.util

import org.junit.jupiter.api.Assertions.assertInstanceOf

internal fun NetworkingResult<*>.expectFailure(): Exception {
    assertInstanceOf(NetworkingResult.Failure::class.java, this)
    return (this as NetworkingResult.Failure).exception
}

internal fun <T> NetworkingResult<T>.expectSuccess(): T {
    assertInstanceOf(NetworkingResult.Success::class.java, this)
    return (this as NetworkingResult.Success).value
}
