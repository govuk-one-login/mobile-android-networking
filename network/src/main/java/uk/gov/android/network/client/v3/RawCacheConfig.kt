package uk.gov.android.network.client.v3

import java.io.File

/**
 * Configuration for the persistent raw cache behind [uk.gov.android.network.client.KtorHttpClient].
 *
 * @property directory Reserved for this cache. Create a single client per process for a directory.
 * @property timeouts Bounds for raw requests only.
 */
class RawCacheConfig private constructor(
    val directory: File,
    val timeouts: CachingRawHttpClient.Timeouts,
) {
    class Builder(private val directory: File) {
        var timeouts: CachingRawHttpClient.Timeouts = CachingRawHttpClient.Timeouts()

        fun build(): RawCacheConfig = RawCacheConfig(directory, timeouts)
    }
}
