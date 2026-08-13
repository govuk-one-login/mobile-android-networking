package uk.gov.android.network.api.v3

import org.junit.jupiter.api.assertInstanceOf

object ApiResponseAssertions {
    fun <F> ApiResponse<*, F, *>.expectFailure(): ApiResponse.Failure<F, *> {
        return assertInstanceOf<ApiResponse.Failure<F, *>>(this)
    }

    fun <T, F> ApiResponse<T, F, *>.expectSuccess(): ApiResponse.Success<T> {
        return assertInstanceOf<ApiResponse.Success<T>>(this)
    }
}
