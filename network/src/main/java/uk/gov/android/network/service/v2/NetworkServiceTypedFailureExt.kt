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
import uk.gov.android.network.service.v2.NetworkServiceTypedFailureExt.makeRequest
import uk.gov.android.network.service.v2.NetworkServiceTypedSuccessExt.makeRequest
import uk.gov.android.network.util.ExcludeFromJacocoGeneratedReport

object NetworkServiceTypedFailureExt {
    /**
     * Make an HTTP request and parse the JSON response
     *
     * @sample networkServiceParseFailureSample
     *
     * @param T the success response body type
     * @param F the failure response body type
     * @param apiRequest The base API request
     * @param json The JSON decoder to parse responses with
     * @param configure Configure extra behaviour such as authentication, attestation and DPoP

     * @return ApiResponse<T, F, NetworkingException> The API response or error.
     */
    suspend inline fun <reified T, reified F> NetworkService.makeRequest(
        apiRequest: ApiRequest,
        json: Json = JsonDefaults.jsonDecoder,
        noinline configure: RequestConfigBuilder.() -> Unit = {},
    ): ApiResponse<T, F, NetworkingException> {
        val response = makeRequest<T>(apiRequest, json, configure)

        val failure =
            when (response) {
                is ApiResponse.Success<T> -> return response
                is ApiResponse.Failure<String, NetworkingException> -> response
            }

        val body = failure.body ?: return ApiResponse.Failure(
            error = response.error,
            status = response.status,
            body = null,
        )

        val parsed =
            try {
                json.decodeFromString<F>(body)
            } catch (e: IllegalArgumentException) {
                return ApiResponse.Failure(
                    error = ApiResponseException("Failed to parse response body as ${F::class}", e),
                    status = failure.status,
                    body = null,
                )
            }

        return ApiResponse.Failure(
            error = response.error,
            body = parsed,
            status = response.status,
        )
    }
}

@ExcludeFromJacocoGeneratedReport
internal suspend fun networkServiceParseFailureSample(
    request: ApiRequest,
    networkService: NetworkService,
) {
    @Serializable
    @ExcludeFromJacocoGeneratedReport
    data class CustomSuccess(
        val subject: String,
        val message: String,
    )

    @Serializable
    @ExcludeFromJacocoGeneratedReport
    data class CustomFailure(
        val errorCode: String,
    )
    val response =
        networkService.makeRequest<CustomSuccess, CustomFailure>(request)

    when (response) {
        is ApiResponse.Failure -> {
            val errorCode = response.body?.errorCode
            Log.d("demo", "errorCode: $errorCode")
        }
        else -> { }
    }
}
