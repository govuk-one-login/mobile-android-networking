package uk.gov.android.network.client.v3

import java.io.File

/**
 * Opt-in configuration for the persistent raw cache behind
 * [uk.gov.android.network.client.KtorHttpClient]'s [RawHttpClient] methods.
 *
 * Deliberately free of Ktor types, so callers do not need Ktor on their compile classpath.
 *
 * @param directory Reserved for this cache. One cache must own a directory, so create a single
 *   client per process for a given directory.
 * @param timeouts Bounds for raw requests only. Other requests made by the client are unchanged.
 */
data class RawCacheConfig(
    val directory: File,
    val timeouts: CachingRawHttpClient.Timeouts = CachingRawHttpClient.Timeouts(),
)
