package com.example.lampstand.data.remote

import android.content.SharedPreferences

/** Persists Convex session tokens (JWT + refresh) across restarts. */
class ConvexTokenStore(private val prefs: SharedPreferences?) {
    fun bearerOrNull(): String? =
        prefs?.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

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
