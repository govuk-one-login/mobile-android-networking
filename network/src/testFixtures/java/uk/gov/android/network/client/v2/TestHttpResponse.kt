package uk.gov.android.network.client.v2

object TestHttpResponse {
    val success =
        GenericHttpResponse(
            200,
            "success",
        )

    val internalServerError =
        GenericHttpResponse(
            500,
            "error",
        )
}
