package uk.gov.android.network.service

/**
 * Base class for all errors resulting from an API request.
 *
 * 'Networking' refers to the name of this library, rather than the type of error.
 */
abstract class NetworkingException(
    message: String? = null,
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
 */
open class ServiceException(
    message: String,
    cause: Throwable?,
) : NetworkingException(message, cause)

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
) : NetworkingException(cause = cause)
