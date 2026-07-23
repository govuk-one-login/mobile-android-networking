package uk.gov.android.network.service.v2

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.gov.android.network.api.v2.ApiRequest
import uk.gov.android.network.api.v3.ApiResponse
import uk.gov.android.network.client.config.RequestConfigBuilder
import uk.gov.android.network.service.ApiResponseException
import uk.gov.android.network.service.NetworkingException
import uk.gov.android.network.service.json.JsonDefaults
import uk.gov.android.network.service.v2.NetworkServiceTypedSuccessExt.makeRequest
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

object NetworkServiceTypedSuccessExt {

    /**
     * Make an HTTP request and parse the JSON response
     *
     * @sample networkServiceParseSuccessSample
     *
     * @param T the success response body type
     * @param apiRequest The base API request
     * @param json The JSON decoder to parse responses with
     * @param configure Configure extra behaviour such as authentication, attestation and DPoP

     * @return ApiResponse<T, String, NetworkingException> The API response or error.
     */
    suspend inline fun <reified T> NetworkService.makeRequest(
        apiRequest: ApiRequest,
        json: Json = JsonDefaults.jsonDecoder,
        noinline configure: RequestConfigBuilder.() -> Unit = {},
    ): ApiResponse<T, String, NetworkingException> {
        val response = makeRequest(apiRequest, configure)

        val success =
            when (response) {
                is ApiResponse.Failure<String, *> -> return response
                is ApiResponse.Success<String> -> response
            }

        val parsed =
            try {
                json.decodeFromString<T>(success.body)
            } catch (e: IllegalArgumentException) {
                return ApiResponse.Failure(
                    error = ApiResponseException("Failed to parse response body as ${T::class}", e),
                    status = success.status,
                    // Don't include the response in the failure result.
                    // Consumers may try to parse this response downstream and expect it to be a
                    // different shape to a successful response body.
                    body = null,
                )
            }

        return ApiResponse.Success(
            body = parsed,
            status = response.status,
        )
    }
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun networkServiceParseSuccessSample(
    request: ApiRequest,
    networkService: NetworkService,
) {
    @Serializable
    @ExcludeFromJacocoGeneratedReport
    data class CustomResponse(
        val subject: String,
        val message: String,
    )

    val response =
        networkService.makeRequest<CustomResponse>(request)

    when (response) {
        is ApiResponse.Success -> {
            // The response is parsed as `CustomResponse`
            val (subject, message) = response.body
            Log.d("demo", "subject: $subject; message: $message")
        }
        else -> { }
    }
}
