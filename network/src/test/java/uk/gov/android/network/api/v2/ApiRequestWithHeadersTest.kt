package uk.gov.android.network.api.v2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiRequestWithHeadersTest {
    private val url = "https://example.com"
    private val header1 = "Header-1" to "value1"
    private val header2 = "Header-2" to "value2"
    private val header3 = "Header-3" to "value3"

    @Test
    fun `given Get request, withHeaders appends headers`() {
        val request = ApiRequest.Get(url = url, headers = listOf(header1))

        val result = request.withHeaders(listOf(header2, header3))

        assertEquals(
            ApiRequest.Get(url = url, headers = listOf(header1, header2, header3)),
            result,
        )
    }

    @Test
    fun `given Post request, withHeaders appends headers`() {
        val request = ApiRequest.Post(url = url, body = "body", headers = listOf(header1))

        val result = request.withHeaders(listOf(header2, header3))

        assertEquals(
            ApiRequest.Post(url = url, body = "body", headers = listOf(header1, header2, header3)),
            result,
        )
    }

    @Test
    fun `given FormUrlEncoded request, withHeaders appends headers`() {
        val request = ApiRequest.FormUrlEncoded(url = url, headers = listOf(header1), params = listOf("key" to "val"))

        val result = request.withHeaders(listOf(header2, header3))

        assertEquals(
            ApiRequest.FormUrlEncoded(
                url = url,
                headers = listOf(header1, header2, header3),
                params = listOf("key" to "val"),
            ),
            result,
        )
    }

    @Test
    fun `given request with existing header, withHeaders appends duplicate`() {
        val request = ApiRequest.Get(url = url, headers = listOf(header1))

        val result = request.withHeaders(listOf(header1))

        assertEquals(
            ApiRequest.Get(url = url, headers = listOf(header1, header1)),
            result,
        )
    }
}
