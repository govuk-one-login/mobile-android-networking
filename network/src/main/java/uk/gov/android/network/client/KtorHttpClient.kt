package uk.gov.android.network.client

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.auth.AuthenticationResponse.Failure
import uk.gov.android.network.auth.AuthenticationResponse.Success
import uk.gov.android.network.client.HttpStatusCodeExtensions.TransportError
import uk.gov.android.network.useragent.UserAgentGenerator

@Suppress("TooGenericExceptionCaught", "OptionalWhenBraces")
class KtorHttpClient(
    userAgentGenerator: UserAgentGenerator
) : GenericHttpClient {

    private var httpClient: HttpClient = makeHttpClient(userAgentGenerator)
    private var authenticationProvider: AuthenticationProvider? = null

    internal fun setHttpClient(httpClient: HttpClient) {
        this.httpClient = httpClient
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun makeHttpClient(userAgentGenerator: UserAgentGenerator): HttpClient {
        val simpleLogger = object : Logger {
            override fun log(message: String) {
                Log.d("GenericHttpClient", message)
            }
        }
        return HttpClient(Android) {
            expectSuccess = true

            install(UserAgent) {
                agent = userAgentGenerator.getUserAgent()
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }

            install(Logging) {
                logger = simpleLogger
                level = LogLevel.ALL
            }

            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    simpleLogger.log("Non-success response received: $exception")

                    val responseException = exception as? ResponseException
                        ?: return@handleResponseExceptionWithRequest
                    val exceptionResponse = responseException.response

                    throw ResponseException(exceptionResponse, exceptionResponse.bodyAsText())
                }
            }
        }
    }

    override fun setAuthenticationProvider(provider: AuthenticationProvider) {
        this.authenticationProvider = provider
    }

    override suspend fun makeAuthorisedRequest(
        apiRequest: ApiRequest,
        scope: String
    ): ApiResponse =
        when (val serviceTokenResponse = this.authenticationProvider?.fetchBearerToken(scope)) {
            null -> ApiResponse.Failure(
                0,
                Exception("Service Token Provider not initialised")
            )

            is Failure -> ApiResponse.Failure(
                0,
                serviceTokenResponse.error
            )

            is Success -> {
                val authorisedApiRequest = authoriseRequest(apiRequest, serviceTokenResponse)
                makeRequest(authorisedApiRequest)
            }
        }

    private fun authoriseRequest(
        apiRequest: ApiRequest,
        serviceTokenResponse: Success
    ): ApiRequest {
        val authorisationHeader =
            Pair("Authorization", "Bearer ${serviceTokenResponse.bearerToken}")
        return when (apiRequest) {
            is ApiRequest.FormUrlEncoded -> {
                apiRequest.copy(headers = apiRequest.headers + authorisationHeader)
            }

            is ApiRequest.Get -> {
                apiRequest.copy(headers = apiRequest.headers + authorisationHeader)
            }

            is ApiRequest.Post<*> -> {
                apiRequest.copy(headers = apiRequest.headers + authorisationHeader)
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse {
        return when (apiRequest) {
            is ApiRequest.Get -> {
                makeGetRequest(apiRequest)
            }

            is ApiRequest.Post<*> -> {
                makePostRequest(apiRequest)
            }

            is ApiRequest.FormUrlEncoded -> {
                makeFormRequest(apiRequest)
            }
        }
    }

    private fun mapContentType(
        contentType: uk.gov.android.network.client.ContentType?
    ): ContentType? {
        return when (contentType) {
            uk.gov.android.network.client.ContentType.APPLICATION_JSON ->
                ContentType.Application.Json

            else -> null
        }
    }

    private suspend fun makeGetRequest(apiRequest: ApiRequest.Get): ApiResponse {
        return try {
            val response = httpClient.get(apiRequest.url) {
                headers {
                    apiRequest.headers.forEach { header ->
                        append(header.first, header.second)
                    }
                }
                url {
                    apiRequest.queryParams.forEach {
                        parameters.append(it.first, it.second)
                    }
                }
            }

            if (response.status != HttpStatusCode.OK) {
                throw ResponseException(response, response.body())
            }

            ApiResponse.Success<String>(response.body())
        } catch (re: ResponseException) {
            ApiResponse.Failure(re.response.status.value, re.mapToApiException())
        } catch (e: Exception) {
            ApiResponse.Failure(HttpStatusCode.TransportError.value, e)
        }
    }

    private suspend fun makePostRequest(apiRequest: ApiRequest.Post<*>): ApiResponse {
        return try {
            val response = httpClient.post(apiRequest.url) {
                headers {
                    apiRequest.headers.forEach { header ->
                        append(header.first, header.second)
                    }
                }
                mapContentType(apiRequest.contentType)?.let {
                    contentType(it)
                }
                setBody(apiRequest.body)
            }

            if (response.status != HttpStatusCode.OK) {
                throw ResponseException(response, response.body())
            }

            ApiResponse.Success<String>(response.body())
        } catch (re: ResponseException) {
            ApiResponse.Failure(re.response.status.value, re.mapToApiException())
        } catch (e: Exception) {
            ApiResponse.Failure(HttpStatusCode.TransportError.value, e)
        }
    }

    private suspend fun makeFormRequest(apiRequest: ApiRequest.FormUrlEncoded): ApiResponse {
        return try {
            val response = httpClient.post(apiRequest.url) {
                headers {
                    apiRequest.headers.forEach { header ->
                        append(header.first, header.second)
                    }
                }
                setBody(
                    FormDataContent(
                        Parameters.build {
                            apiRequest.params.forEach {
                                append(it.first, it.second)
                            }
                        }
                    )
                )
            }

            if (response.status != HttpStatusCode.OK) {
                throw ResponseException(response, response.body())
            }

            ApiResponse.Success<String>(response.body())
        } catch (re: ResponseException) {
            ApiResponse.Failure(re.response.status.value, re.mapToApiException())
        } catch (e: Exception) {
            ApiResponse.Failure(HttpStatusCode.TransportError.value, e)
        }
    }
}
