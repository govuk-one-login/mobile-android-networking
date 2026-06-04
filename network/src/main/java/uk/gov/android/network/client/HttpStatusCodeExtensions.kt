package uk.gov.android.network.client

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import uk.gov.android.network.api.ApiResponseException

internal object HttpStatusCodeExtensions {
    val HttpStatusCode.Companion.TransportError: HttpStatusCode
        get() = HttpStatusCode(0, "Transport error")
}

internal fun ResponseException.mapToApiException(): ApiResponseException = ApiResponseException(this.message ?: "")
