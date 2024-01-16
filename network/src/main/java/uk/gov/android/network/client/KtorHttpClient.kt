package uk.gov.android.network.client

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
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import uk.gov.android.network.api.ApiRequest
import uk.gov.android.network.api.ApiResponse
import uk.gov.android.network.client.ContentType as ContentTypeEnum
import uk.gov.android.network.client.HttpStatusCodeExtensions.TransportError
import uk.gov.android.network.useragent.UserAgentGenerator

@Suppress("TooGenericExceptionCaught", "OptionalWhenBraces")
class KtorHttpClient(
    userAgentGenerator: UserAgentGenerator
) : GenericHttpClient {

    private var httpClient: HttpClient = makeHttpClient(userAgentGenerator)

    internal fun setHttpClient(httpClient: HttpClient) {
        this.httpClient = httpClient
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun makeHttpClient(userAgentGenerator: UserAgentGenerator): HttpClient {
        val simpleLogger = Logger.SIMPLE
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

    override suspend fun makeRequest(apiRequest: ApiRequest): ApiResponse {
        return when (apiRequest) {
            is ApiRequest.Get -> {
                try {
                    val response = httpClient.get(apiRequest.url) {
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
            }

            is ApiRequest.Post<*> -> {
                try {
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
        }
    }

    private fun mapContentType(contentType: ContentTypeEnum?): ContentType? {
        return when (contentType) {
            ContentTypeEnum.APPLICATION_JSON -> ContentType.Application.Json
            else -> null
        }
    }
}
