package uk.gov.android.network.service

import uk.gov.android.network.attestation.ClientAttestationErrorReason
import uk.gov.android.network.attestation.ClientAttestationProvider
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.dpop.DPoPProvider

/**
 * Base class for all errors resulting from an API request.
 *
 * 'Networking' refers to the name of this library, rather than the type of error.
 */
abstract class NetworkingException(
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * The service that received the request wasn't configured correctly
 */
class ConfigurationException(
    message: String,
) : NetworkingException(message)

/**
 * The service that received the request failed before sending it to the server
 *
 * @see [AuthenticationProviderException]
 * @see [ClientAttestationException]
 * @see [DPoPException]
 */
open class ServiceException(
    message: String,
    cause: Throwable?,
) : NetworkingException(message, cause)

/**
 * The service failed to get the access token needed to make the request.
 *
 * This exception is *not* emitted for 401 unauthorized or 403 forbidden status codes, which
 * you should handle through [ApiResponseException].
 *
 * @see [AuthenticationProvider]
 */
class AuthenticationProviderException(
    message: String,
    cause: Throwable?,
) : ServiceException(message, cause)

/**
 * The service failed to fetch the client attestation needed to make the request
 *
 * @see [ClientAttestationProvider]
 */
class ClientAttestationException(
    message: String,
    val reason: ClientAttestationErrorReason,
    cause: Throwable?,
) : ServiceException(message, cause)

/**
 * The service failed to generate the demonstrating proof-of-possession (DPoP) needed
 * to make the request.
 *
 * @see [DPoPProvider]
 */
class DPoPException(
    message: String,
    cause: Throwable?,
) : ServiceException(message, cause)

/**
 * The request wasn't configured properly
 */
class ApiRequestException(
    message: String,
    cause: Throwable?,
) : NetworkingException(message, cause)

/**
 * The server returned a non-success (3xx, 4xx, 5xx) response or the response body was unusable
 */
class ApiResponseException(
    message: String,
    cause: Throwable?,
) : NetworkingException(message, cause)

/**
 * No response due to network-level failure (e.g. timeout, DNS resolution)
 */
class TransportException(
    cause: Throwable?,
) : NetworkingException("Network transport error", cause = cause)
