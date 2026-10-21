package net.lampstandapp.app.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Generic Convex HTTP client API.
 * Every call posts {path, args, format:"json"} and gets
 * {status:"success", value:...} (or status:"error").
 */
interface ConvexApi {
    @POST("api/query")
    suspend fun query(
        @Body body: JsonObject,
        @Header("Authorization") auth: String? = null
    ): JsonObject

    @POST("api/mutation")
    suspend fun mutation(
        @Body body: JsonObject,
        @Header("Authorization") auth: String? = null
    ): JsonObject

    @POST("api/action")
    suspend fun action(
        @Body body: JsonObject,
        @Header("Authorization") auth: String? = null
    ): JsonObject
}

/** Build {path, args, format} bodies for the three endpoints. */
object ConvexBodies {
    fun query(path: String, args: JsonElement): JsonObject = kotlinx.serialization.json.buildJsonObject {
        put("path", kotlinx.serialization.json.JsonPrimitive(path))
        put("args", args)
        put("format", kotlinx.serialization.json.JsonPrimitive("json"))
    }
    fun action(path: String, args: JsonElement): JsonObject = query(path, args)
    fun mutation(path: String, args: JsonElement): JsonObject = query(path, args)
}
