package uk.gov.android.network.client.v2

object TestResponseException {
    val internalServerError =
        GenericResponseException(
            response = TestHttpResponse.internalServerError,
            cause = IllegalStateException("response exception"),
        )
}
