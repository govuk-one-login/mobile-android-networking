package uk.gov.android.network.client

import kotlinx.serialization.Serializable

@Serializable
data class TestData(
    val name: String,
    val id: String,
)
