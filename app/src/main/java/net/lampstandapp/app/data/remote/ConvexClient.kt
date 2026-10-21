package net.lampstandapp.app.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ConvexClient {
    val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun create(cloudUrl: String = ConvexConfig.cloudUrl): ConvexApi {
        val base = if (cloudUrl.endsWith("/")) cloudUrl else "$cloudUrl/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ConvexApi::class.java)
    }

    /** Unwrap {status, value} envelope; throw on error status. */
    fun valueOf(envelope: JsonObject): kotlinx.serialization.json.JsonElement {
        val status = (envelope["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        if (status != "success") {
            val message = (envelope["errorMessage"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.content
                ?.takeIf { it.isNotBlank() }
                ?: envelope.toString()
            throw IllegalStateException(message)
        }
        return envelope.getValue("value")
    }
}
