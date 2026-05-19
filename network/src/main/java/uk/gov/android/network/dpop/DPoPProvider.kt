package uk.gov.android.network.dpop

/**
 * Provides DPoP (Demonstrating Proof of Possession) tokens for network requests.
 *
 * Consumers implement this interface to supply a DPoP proof JWT, enabling sender-constrained
 * token usage on network calls.
 */
fun interface DPoPProvider {
    suspend fun getRefreshDPoP(): DPoPResponse
}
