package uk.gov.android.network.client

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import uk.gov.android.network.api.ApiResponseException

object HttpStatusCodeExtensions {

    val HttpStatusCode.Companion.TransportError: HttpStatusCode
        get() = HttpStatusCode(0, "Transport error")
}

fun ResponseException.mapToApiException(): ApiResponseException {
    return ApiResponseException(this.message ?: "")
}
