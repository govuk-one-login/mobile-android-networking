package uk.gov.android.network.client.v2

internal class StubResponseException(
    override val response: GenericHttpResponse = StubHttpResponse(status = 500),
) : GenericResponseException(Exception("response exception"))
