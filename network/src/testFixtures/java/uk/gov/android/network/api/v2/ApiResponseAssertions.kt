package uk.gov.android.network.api.v2

import org.junit.jupiter.api.Assertions.assertInstanceOf

object ApiResponseAssertions {
    fun ApiResponse<*, *>.expectFailure(): ApiResponse.Failure<*> = assertInstanceOf(
    ApiResponse.Failure::class.java,
    this,
    )

    fun <T> ApiResponse<T, *>.expectSuccess(): ApiResponse.Success<T> {
        assertInstanceOf(ApiResponse.Success::class.java, this)
        return this as ApiResponse.Success<T>
    }
}
