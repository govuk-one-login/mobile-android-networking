package uk.gov.android.network.performance

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.logging.api.performance.PerformanceMonitor
import java.net.URL

typealias PerfHttpMethod = uk.gov.logging.api.performance.HttpMethod

class PerformanceMonitorPluginTest {
    private val performanceMonitor: PerformanceMonitor = mock()
    private val metric: PerformanceMonitor.HttpMetric = mock()

    @BeforeEach
    fun setUp() {
        whenever(
            performanceMonitor.newHTTPMetric(any(), any()),
        ).thenReturn(metric)
    }

    @Test
    fun `starts HTTPMetric and records metrics`(): Unit =
        runBlocking {
            val client = createClient()
            client.get("https://example.com/test")

            verify(performanceMonitor).newHTTPMetric(
                eq(URL("https://example.com/test")),
                eq(PerfHttpMethod.GET),
            )

            verify(metric, never()).setContentType(any())
            verify(metric).setResponseCode(200)
            verify(metric).setResponseSize(2L)

            verify(metric).stop()
            verifyNoMoreInteractions(metric)
        }

    @Test
    fun `records optional metrics when present`(): Unit =
        runBlocking {
            val client = createClient()
            client.post("https://example.com/upload") {
                headers {
                    append(HttpHeaders.ContentLength, "123")
                }
                setBody("dummy body")
            }

            verify(performanceMonitor).newHTTPMetric(
                eq(URL("https://example.com/upload")),
                eq(PerfHttpMethod.POST),
            )

            verify(metric).setResponseCode(200)
            verify(metric).setResponseSize(2L)
            verify(metric).setContentType(ContentType.Application.Json.toString())
            verify(metric).stop()
        }

    private fun createClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val headers =
                        when (request.method) {
                            HttpMethod.Post ->
                                headersOf(
                                    HttpHeaders.ContentLength to listOf("2"),
                                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                                )
                            else -> headersOf(HttpHeaders.ContentLength, "2")
                        }
                    respond(
                        content = "OK",
                        status = HttpStatusCode.OK,
                        headers = headers,
                    )
                }
            }

            install(PerformanceMonitorPlugin) {
                performanceMonitor = this@PerformanceMonitorPluginTest.performanceMonitor
            }
        }
}
