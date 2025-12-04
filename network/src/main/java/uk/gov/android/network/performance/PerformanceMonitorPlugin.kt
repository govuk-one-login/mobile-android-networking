package uk.gov.android.network.performance

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.contentType
import uk.gov.logging.api.performance.PerformanceMonitor
import java.net.URL

typealias PerfHttpMethod = uk.gov.logging.api.performance.HttpMethod

class PerformanceMonitorConfig {
    lateinit var performanceMonitor: PerformanceMonitor
}

val PerformanceMonitorPlugin =
    createClientPlugin(
        name = "PerformanceMonitorPlugin",
        createConfiguration = ::PerformanceMonitorConfig,
    ) {
        val performanceProvider = pluginConfig.performanceMonitor
        var metric: PerformanceMonitor.HttpMetric? = null

        onRequest { request, _ ->
            val url = URL(request.url.buildString())
            val method = request.method.toPerfHttpMethod()
            metric = performanceProvider.newHTTPMetric(url, method)
            request.contentLength()?.let { length ->
                metric.setRequestSize(length)
            }
        }

        onResponse { response ->
            with(metric ?: return@onResponse) {
                response.contentLength()?.let { length ->
                    setResponseSize(length)
                }
                setResponseCode(response.status.value)
                response.contentType()?.let { type ->
                    var contentType: String = type.contentType
                    val subType: String = type.contentSubtype
                    if (contentType.isNotEmpty() && subType.isNotEmpty()) {
                        contentType = "$contentType/$subType"
                    }
                    setContentType(contentType)
                }

                stop()
            }
        }
    }

private fun HttpMethod.toPerfHttpMethod(): PerfHttpMethod =
    when (this) {
        HttpMethod.Get -> PerfHttpMethod.GET
        HttpMethod.Post -> PerfHttpMethod.POST
        HttpMethod.Put -> PerfHttpMethod.PUT
        HttpMethod.Delete -> PerfHttpMethod.DELETE
        HttpMethod.Patch -> PerfHttpMethod.PATCH
        HttpMethod.Head -> PerfHttpMethod.HEAD
        HttpMethod.Options -> PerfHttpMethod.OPTIONS
        else -> PerfHttpMethod.GET
    }
