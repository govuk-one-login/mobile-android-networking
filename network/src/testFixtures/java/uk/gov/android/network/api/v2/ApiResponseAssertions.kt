package uk.gov.android.network.api.v2

import org.junit.jupiter.api.assertInstanceOf

object ApiResponseAssertions {
    fun ApiResponse<*, *>.expectFailure(): ApiResponse.Failure<*> = assertInstanceOf<ApiResponse.Failure<*>>(this)

    fun <T> ApiResponse<T, *>.expectSuccess(): ApiResponse.Success<T> = assertInstanceOf<ApiResponse.Success<T>>(this)
}
