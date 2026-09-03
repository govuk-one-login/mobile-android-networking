@file:Suppress("MaxLineLength")

package uk.gov.android.network.util

import org.junit.jupiter.api.assertInstanceOf

internal fun NetworkingResult<*>.expectFailure(): Exception = assertInstanceOf<NetworkingResult.Failure<*>>(this).exception

internal fun <T> NetworkingResult<T>.expectSuccess(): T = assertInstanceOf<NetworkingResult.Success<T>>(this).value
