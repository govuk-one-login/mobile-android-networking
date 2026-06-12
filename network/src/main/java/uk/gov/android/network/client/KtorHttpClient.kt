package uk.gov.android.network.client

import androidx.annotation.VisibleForTesting
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import uk.gov.android.network.BuildConfig
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.auth.AuthenticationProvider
import uk.gov.android.network.auth.AuthenticationResponse
import uk.gov.android.network.client.HttpStatusCodeExtensions.TransportError
import uk.gov.android.network.client.headers.toAuthorisationHeader
import uk.gov.android.network.client.v2.GenericHttpResponse
import uk.gov.android.network.client.v2.GenericResponseException
import uk.gov.android.network.log.KtorLogger
import uk.gov.android.network.log.KtorLoggerAdapter
import uk.gov.android.network.useragent.UserAgentGenerator
import io.ktor.http.ContentType as KtorContentType
import uk.gov.android.network.api.v2.ApiRequest as ApiRequestV2
import uk.gov.android.network.client.v2.GenericHttpClient as GenericHttpClientV2

@Suppress("TooGenericExceptionCaught")
class KtorHttpClient
    @VisibleForTesting
    constructor(
        userAgentGenerator: UserAgentGenerator,
        logger: KtorLogger,
        ktorClientEngine: HttpClientEngine,
    ) : GenericHttpClient,
        GenericHttpClientV2 {
        private val httpClient: HttpClient =
            makeHttpClient(
                userAgentGenerator = userAgentGenerator,
                logger = logger,
                ktorClientEngine = ktorClientEngine,
            )
        private var authenticationProvider: AuthenticationProvider? = null

        constructor(
            userAgentGenerator: UserAgentGenerator,
            logger: KtorLogger = if (BuildConfig.DEBUG) KtorLogger.simple else KtorLogger.noOp,
        ) : this(
            userAgentGenerator = userAgentGenerator,
            logger = logger,
            ktorClientEngine = Android.create(),
        )

        private fun makeHttpClient(
            userAgentGenerator: UserAgentGenerator,
            logger: KtorLogger,
            ktorClientEngine: HttpClientEngine,
        ): HttpClient {
            return HttpClient(ktorClientEngine) {
                expectSuccess = true

                install(UserAgent) {
                    agent = userAgentGenerator.getUserAgent()
                }

                install(HttpCache)

                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            explicitNulls = false
                        },
                    )
                }

                install(Logging) {
                    this.logger = KtorLoggerAdapter(logger)
                    level = LogLevel.ALL
                }

                HttpResponseValidator {
                    handleResponseExceptionWithRequest { exception, _ ->
                        logger.log(NON_SUCCESS_MESSAGE + exception.toString())

                        val responseException =
                            exception as? ResponseException
                                ?: return@handleResponseExceptionWithRequest
                        val exceptionResponse = responseException.response

                        throw ResponseException(exceptionResponse, exceptionResponse.bodyAsText())
                    }
                }
            }
        }

        override suspend fun request(request: ApiRequestV2): GenericHttpResponse =
            try {
                httpClient
                    .request {
                        url(request.url)
                        method =
                            when (request) {
                                is ApiRequestV2.Get -> HttpMethod.Get
                                is ApiRequestV2.Post<*>,
                                is ApiRequestV2.FormUrlEncoded,
                                -> HttpMethod.Post
                            }

                        headers {
                            request.headers.forEach { (key, value) ->
                                append(key, value)
                            }
                        }

                        if (request is ApiRequestV2.Post<*>) {
                            setBody(request.body)
                            if (request.contentType != null) {
                                contentType(request.contentType.toKtorContentType())
                            }
                        }

                        if (request is ApiRequestV2.FormUrlEncoded) {
                            setBody(
                                FormDataContent(
                                    Parameters.build {
                                        request.params.forEach { (key, value) ->
                                            append(key, value)
                                        }
                                    },
                                ),
                            )
                        }
                    }
            } catch (e: ResponseException) {
                throw GenericResponseException.fromKtorResponseException(e)
            }.let { response ->
                GenericHttpResponse.fromKtorHttpResponse(response)
            }

        override fun setAuthenticationProvider(provider: AuthenticationProvider) {
            this.authenticationProvider = provider
        }

        override suspend fun makeAuthorisedRequest(
            apiRequest: ApiRequest,
            scope: String,
        ): ApiResponse =
            when (val serviceTokenResponse = this.authenticationProvider?.fetchBearerToken(scope)) {
                null ->
                    ApiResponse.Failure(
                        0,
                        Exception("Service Token Provider not initialised"),
                    )

                is AuthenticationResponse.Failure ->
                    ApiResponse.Failure(
                        0,
                        serviceTokenResponse.error,
                    )

                is AuthenticationResponse.Success -> {
                    val authorisedApiRequest = authoriseRequest(apiRequest, serviceTokenResponse)
                    makeRequest(authorisedApiRequest)
                }
            }

        private fun authoriseRequest(
            apiRequest: ApiRequest,
            serviceTokenResponse: AuthenticationResponse.Success,
        ): ApiRequest {
            val authorisationHeader =
                serviceTokenResponse.toAuthorisationHeader()
            return when (apiRequest) {
                is ApiRequest.FormUrlEncoded ->
                    apiRequest.copy(headers = apiRequest.headers + authorisationHeader)

                is ApiRequest.Get ->
                    apiRequest.copy(headers = apiRequest.headers + authorisationHeader)

                is ApiRequest.Post<*> ->
                    apiRequest.copy(headers = apiRequest.headers + authorisationHeader)
            }
        }

        override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse =
            when (apiRequest) {
                is ApiRequest.Get -> makeGetRequest(apiRequest)
                is ApiRequest.Post<*> -> makePostRequest(apiRequest)
                is ApiRequest.FormUrlEncoded -> makeFormRequest(apiRequest)
            }

        private suspend fun makeGetRequest(apiRequest: ApiRequest.Get): ApiResponse =
            try {
                val response =
                    httpClient.get(apiRequest.url) {
                        headers {
                            apiRequest.headers.forEach { header ->
                                append(header.first, header.second)
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

        private suspend fun makePostRequest(apiRequest: ApiRequest.Post<*>): ApiResponse =
            try {
                val response =
                    httpClient.post(apiRequest.url) {
                        headers {
                            apiRequest.headers.forEach { header ->
                                append(header.first, header.second)
                            }
                        }
                        apiRequest.contentType?.toKtorContentType()?.let {
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

        private suspend fun makeFormRequest(apiRequest: ApiRequest.FormUrlEncoded): ApiResponse =
            try {
                val response =
                    httpClient.post(apiRequest.url) {
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
                                },
                            ),
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

        companion object {
            private const val NON_SUCCESS_MESSAGE = "Non-success response received: "
        }
    }

internal fun ContentType.toKtorContentType(): KtorContentType =
    when (this) {
        ContentType.APPLICATION_JSON -> KtorContentType.Application.Json
    }
