package uk.gov.android.network.util

import org.junit.jupiter.api.assertInstanceOf

internal fun NetworkingResult<*>.expectFailure(): Exception {
    return assertInstanceOf<NetworkingResult.Failure<*>>(this).exception
}

internal fun <T> NetworkingResult<T>.expectSuccess(): T {
    return assertInstanceOf<NetworkingResult.Success<T>>(this).value
}
