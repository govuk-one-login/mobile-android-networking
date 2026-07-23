package uk.gov.android.network.api.v3

import org.junit.jupiter.api.Assertions.assertInstanceOf

object ApiResponseAssertions {
    fun <F> ApiResponse<*, F, *>.expectFailure(): ApiResponse.Failure<F, *> {
        assertInstanceOf(
            ApiResponse.Failure::class.java,
            this,
        )
        return this as ApiResponse.Failure<F, *>
    }

    fun <T, F> ApiResponse<T, F, *>.expectSuccess(): ApiResponse.Success<T> {
        assertInstanceOf(ApiResponse.Success::class.java, this)
        return this as ApiResponse.Success<T>
    }
}
