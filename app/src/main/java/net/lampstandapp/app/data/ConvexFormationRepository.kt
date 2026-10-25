package net.lampstandapp.app.data

import android.content.SharedPreferences
import net.lampstandapp.app.data.remote.ConvexApi
import net.lampstandapp.app.data.remote.ConvexBodies
import net.lampstandapp.app.data.remote.ConvexClient
import net.lampstandapp.app.data.remote.ConvexTokenStore
import net.lampstandapp.app.data.remote.CompleteSessionResult
import net.lampstandapp.app.data.remote.DiscoveryItem
import net.lampstandapp.app.data.remote.HomeStateDto
import net.lampstandapp.app.data.remote.MeDto
import net.lampstandapp.app.data.remote.OverviewRequest
import net.lampstandapp.app.data.remote.PathOverviewDto
import net.lampstandapp.app.data.remote.SessionContentDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull

private const val KEY_DISCOVERY = "cache_discovery_v1"
private const val KEY_HOME = "cache_home_v1"
private const val KEY_HOME_AT = "cache_home_at_v1"
private const val KEY_ME = "cache_me_v1"
private fun overviewKey(id: String) = "cache_overview_v1_$id"

/**
 * Direct-query implementation over POST /api/query. No custom bridge.
 * - discovery -> "discovery:getPathsDiscoveryScreen" {}
 * - overview  -> "paths:getPathOverview" {pathId}
 * Sends Bearer JWT when logged in (OTP), works logged-out otherwise.
 * Network-first, cache-fallback so Paths screens render offline.
 */
class ConvexFormationRepository(
    private val api: ConvexApi,
    private val prefs: SharedPreferences?,
    private val tokens: ConvexTokenStore,
    private val json: Json = ConvexClient.json
) : FormationRepository {

    override suspend fun getDiscovery(): Result<List<DiscoveryItem>> {
        return try {
            val envelope = api.query(
                ConvexBodies.query("discovery:getPathsDiscoveryScreen", buildJsonObject {}),
                tokens.bearerOrNull()
            )
            val items = json.decodeFromJsonElement(
                ListSerializer(DiscoveryItem.serializer()),
                ConvexClient.valueOf(envelope)
            )
            prefs?.edit()?.putString(KEY_DISCOVERY, json.encodeToString(ListSerializer(DiscoveryItem.serializer()), items))?.apply()
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = prefs?.getString(KEY_DISCOVERY, null)
            if (cached != null) {
                try {
                    Result.success(json.decodeFromString(ListSerializer(DiscoveryItem.serializer()), cached))
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getOverview(pathId: String): Result<PathOverviewDto> {
        return try {
            val args: JsonObject =
                json.encodeToJsonElement(OverviewRequest.serializer(), OverviewRequest(pathId)) as JsonObject
            val envelope = api.query(
                ConvexBodies.query("paths:getPathOverview", args),
                tokens.bearerOrNull()
            )
            val dto = json.decodeFromJsonElement(PathOverviewDto.serializer(), ConvexClient.valueOf(envelope))
            try {
                prefs?.edit()?.putString(overviewKey(pathId), json.encodeToString(PathOverviewDto.serializer(), dto))?.apply()
            } catch (_: Exception) { /* best-effort */ }
            Result.success(dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = prefs?.getString(overviewKey(pathId), null)
            if (cached != null) {
                try {
                    Result.success(json.decodeFromString(PathOverviewDto.serializer(), cached))
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getHomeState(): Result<HomeStateDto> {
        return try {
            val envelope = api.query(
                ConvexBodies.query("paths:getHomeState", buildJsonObject {}),
                tokens.bearerOrNull()
            )
            val dto = json.decodeFromJsonElement(HomeStateDto.serializer(), ConvexClient.valueOf(envelope))
            try {
                prefs?.edit()
                    ?.putString(KEY_HOME, json.encodeToString(HomeStateDto.serializer(), dto))
                    ?.putLong(KEY_HOME_AT, System.currentTimeMillis())
                    ?.apply()
            } catch (_: Exception) { /* best-effort */ }
            Result.success(dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = prefs?.getString(KEY_HOME, null)
            if (cached != null) {
                try {
                    Result.success(json.decodeFromString(HomeStateDto.serializer(), cached))
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun recordTap(pathId: String): Result<Int> {
        return try {
            val bearer = tokens.bearerOrNull()
                ?: return Result.failure(IllegalStateException("Sign in to express interest"))
            val args = buildJsonObject {
                put("pathId", kotlinx.serialization.json.JsonPrimitive(pathId))
            }
            val envelope = api.mutation(
                ConvexBodies.mutation("discovery:recordTap", args),
                bearer
            )
            val v = ConvexClient.valueOf(envelope) as JsonObject
            val total = (v["totalTaps"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.doubleOrNull?.toInt() ?: 0
            // Refresh discovery cache so the counter updates.
            getDiscovery()
            Result.success(total)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startPath(pathId: String): Result<Unit> {
        return try {
            val bearer = tokens.bearerOrNull()
                ?: return Result.failure(IllegalStateException("Sign in to start a path"))
            val args = buildJsonObject {
                put("pathId", kotlinx.serialization.json.JsonPrimitive(pathId))
            }
            ConvexClient.valueOf(
                api.mutation(ConvexBodies.mutation("paths:startPath", args), bearer)
            )
            // Overview + home both change after start.
            getOverview(pathId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Web ignores PATH_ALREADY_ACTIVE (returns existing record) — treat as success.
            if ((e.message ?: "").contains("PATH_ALREADY_ACTIVE")) Result.success(Unit)
            else Result.failure(e)
        }
    }
    override suspend fun getSessionContent(sessionId: String): Result<SessionContentDto> {
        return try {
            val args = buildJsonObject {
                put("sessionId", kotlinx.serialization.json.JsonPrimitive(sessionId))
            }
            val envelope = api.query(
                ConvexBodies.query("paths:getSessionContent", args),
                tokens.bearerOrNull()
            )
            val raw = ConvexClient.valueOf(envelope)
            // null = signed out / expired (web renders "Session not found").
            if (raw is JsonNull) {
                return Result.failure(IllegalStateException("Session not found — sign in again"))
            }
            val obj = raw as JsonObject
            // Convex spreads ...session so the id arrives as _id; normalize to sessionId.
            val withId = JsonObject(
                obj + ("sessionId" to (obj["sessionId"] ?: obj["_id"] ?: JsonNull))
            )
            Result.success(json.decodeFromJsonElement(SessionContentDto.serializer(), withId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeSession(sessionId: String): Result<CompleteSessionResult> {
        return try {
            val bearer = tokens.bearerOrNull()
                ?: return Result.failure(IllegalStateException("Sign in to complete a session"))
            val args = buildJsonObject {
                put("sessionId", kotlinx.serialization.json.JsonPrimitive(sessionId))
            }
            val envelope = api.mutation(
                ConvexBodies.mutation("paths:completeSession", args),
                bearer
            )
            val dto = json.decodeFromJsonElement(
                CompleteSessionResult.serializer(), ConvexClient.valueOf(envelope)
            )
            // Home + overview both change after completion.
            loadHomeSilent()
            Result.success(dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadHomeSilent() {
        try {
            val envelope = api.query(
                ConvexBodies.query("paths:getHomeState", buildJsonObject {}),
                tokens.bearerOrNull()
            )
            val dto = json.decodeFromJsonElement(HomeStateDto.serializer(), ConvexClient.valueOf(envelope))
            try {
                prefs?.edit()
                    ?.putString(KEY_HOME, json.encodeToString(HomeStateDto.serializer(), dto))
                    ?.putLong(KEY_HOME_AT, System.currentTimeMillis())
                    ?.apply()
            } catch (_: Exception) { /* best-effort */ }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { /* best-effort */ }
    }

    override suspend fun getMe(): Result<MeDto?> {
        return try {
            val raw = ConvexClient.valueOf(
                api.query(
                    ConvexBodies.query("users:me", buildJsonObject {}),
                    tokens.bearerOrNull()
                )
            )
            // users:me returns null when signed out.
            if (raw is JsonNull) {
                Result.success(null)
            } else {
                val dto = json.decodeFromJsonElement(MeDto.serializer(), raw)
                try {
                    prefs?.edit()?.putString(KEY_ME, json.encodeToString(MeDto.serializer(), dto))?.apply()
                } catch (_: Exception) { /* best-effort */ }
                Result.success(dto)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = prefs?.getString(KEY_ME, null)
            if (cached != null) {
                try {
                    Result.success(json.decodeFromString(MeDto.serializer(), cached))
                } catch (_: Exception) {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        }
    }
}
