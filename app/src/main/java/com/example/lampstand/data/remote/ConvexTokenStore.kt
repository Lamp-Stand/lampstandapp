package com.example.lampstand.data.remote

import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

/** Persists Convex session tokens (JWT + refresh) across restarts. */
class ConvexTokenStore(private val prefs: SharedPreferences?) {
    fun tokenOrNull(): String? =
        prefs?.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun refreshTokenOrNull(): String? =
        prefs?.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }

    fun bearerOrNull(): String? = tokenOrNull()?.let { "Bearer $it" }

    /** True when the JWT is expired or about to expire. */
    fun needsRefresh(leewaySeconds: Long = 300): Boolean {
        val token = tokenOrNull() ?: return false
        val expiresAt = accessTokenExpirySeconds(token) ?: return false
        return expiresAt <= System.currentTimeMillis() / 1000 + leewaySeconds
    }

    private fun accessTokenExpirySeconds(token: String): Long? {
        return try {
            val payload = token.split('.').getOrNull(1) ?: return null
            val decoded = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp", 0L).takeIf { it > 0L }
        } catch (_: Exception) {
            null
        }
    }

    fun save(token: String, refreshToken: String) {
        prefs?.edit()?.putString(KEY_TOKEN, token)?.putString(KEY_REFRESH, refreshToken)?.apply()
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_TOKEN)?.remove(KEY_REFRESH)?.apply()
    }

    companion object {
        private const val KEY_TOKEN = "convex_jwt_v1"
        private const val KEY_REFRESH = "convex_refresh_v1"
    }
}
