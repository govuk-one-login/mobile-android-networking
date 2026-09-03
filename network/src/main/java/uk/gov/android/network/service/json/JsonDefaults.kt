package uk.gov.android.network.service.json

import kotlinx.serialization.json.Json

object JsonDefaults {
    /**
     * Default JSON decoder for decoding network responses
     */
    val jsonDecoder = Json {
        ignoreUnknownKeys = true
    }
}
