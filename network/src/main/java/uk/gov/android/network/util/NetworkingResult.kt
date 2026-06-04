package uk.gov.android.network.util

import uk.gov.android.network.api.v2.NetworkingException

sealed class NetworkingResult<T> {
    data class Success<T>(
        val value: T,
    ) : NetworkingResult<T>()

    data class Failure<T>(
        val exception: NetworkingException,
    ) : NetworkingResult<T>()
}
