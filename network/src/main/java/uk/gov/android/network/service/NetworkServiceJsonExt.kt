package uk.gov.android.network.service

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v2.ApiResponse
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.service.NetworkServiceJsonExt.makeRequest
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

@Deprecated(
    "Migrate to v2",
    replaceWith = ReplaceWith(
        "NetworkServiceTypedSuccessExt",
        "uk.gov.android.network.service.v2.NetworkServiceTypedSuccessExt"
    )
)
object NetworkServiceJsonExt {
    /**
     * Default JSON decoder for decoding network responses
     */
    val jsonDecoder = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Make an HTTP request and parse the JSON response
     *
     * @sample networkServiceParseResponseSample
     *
     * @param apiRequest The base API request
     * @param configure Configure extra behaviour such as authentication, attestation and DPoP

     * @return ApiResponse<T, NetworkingException> The API response or error.
     */
    @Deprecated(
        "Migrate to NetworkServiceTypedSuccessExt.makeRequest",
        replaceWith = ReplaceWith(
            "makeRequest<T>(apiRequest, configure)",
            "uk.gov.android.network.service.v2.NetworkServiceTypedSuccessExt.makeRequest"
        )
    )
    suspend inline fun <reified T> NetworkService.makeRequest(
        apiRequest: ApiRequest,
        json: Json = jsonDecoder,
        noinline configure: RequestConfigBuilder.() -> Unit = {},
    ): ApiResponse<T, NetworkingException> {
        val response = makeRequest(apiRequest, configure)

        val success =
            when (response) {
                is ApiResponse.Failure<*> -> return response
                is ApiResponse.Success<String> -> response
            }

        val parsed =
            try {
                json.decodeFromString<T>(success.response)
            } catch (e: IllegalArgumentException) {
                return ApiResponse.Failure(
                    error = ApiResponseException("Failed to parse response body as ${T::class}", e),
                    status = success.status,
                )
            }

        return ApiResponse.Success(
            response = parsed,
            status = response.status,
        )
    }
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun networkServiceParseResponseSample(
    request: ApiRequest,
    networkService: NetworkService,
) {
    @Serializable
    data class CustomResponse(
        val subject: String,
        val message: String,
    )

    val response =
        networkService.makeRequest<CustomResponse>(request)

    when (response) {
        is ApiResponse.Success -> {
            // The response is parsed as `CustomResponse`
            val (subject, message) = response.response
            Log.d("demo", "subject: $subject; message: $message")
        }
        else -> { }
    }
}
