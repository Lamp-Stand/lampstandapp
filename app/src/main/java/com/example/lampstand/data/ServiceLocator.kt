package com.example.lampstand.data

import android.content.Context
import com.example.lampstand.data.remote.ConvexClient
import com.example.lampstand.data.remote.ConvexConfig
import com.example.lampstand.data.remote.ConvexTokenStore
import java.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * THE switch. The whole app reads the backend through here.
 *
 * Auth is the Convex JWT kept in [ConvexTokenStore]: no session means the
 * sign-in flow (Google OAuth or email OTP) is shown. There is no demo
 * backend — formation data always comes from the same cloud the web app uses.
 */
object ServiceLocator {

    private const val KEY_OAUTH_VERIFIER = "oauth_verifier_v1"
    private const val KEY_ONBOARDING_DONE = "onboarding_done_v1"

    private var appContext: Context? = null

    /** Call once from the UI layer so the token/cache prefs can persist. */
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private val cachePrefs
        get() = try {
            appContext?.getSharedPreferences("lampstand_formation_cache", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }

    private val tokenStore: ConvexTokenStore by lazy { ConvexTokenStore(cachePrefs) }

    private val convexApi by lazy { ConvexClient.create(ConvexConfig.cloudUrl) }

    /** Step 1 of Custom-Tab Google OAuth: Convex sign-in URL + round-trip verifier. */
    suspend fun beginGoogleOAuth(): Result<com.example.lampstand.data.remote.GoogleOAuth.OAuthStep1> =
        com.example.lampstand.data.remote.GoogleOAuth.beginBrowserOAuth(convexApi)

    /** Step 3: ?code= from the deep link + step-1 verifier -> Convex session. */
    suspend fun finishGoogleOAuth(code: String, verifier: String): Result<String> =
        com.example.lampstand.data.remote.GoogleOAuth.finishBrowserOAuth(
            convexApi, tokenStore, code, verifier
        )

    // ── Email OTP (web parity: signIn("resend-otp"). No browser, no backend
    // change, no Google console involved — works the moment Resend is set up. ──
    /** Step 1: send the 4-digit code to the email. Returns {started:true}. */
    suspend fun requestOtp(email: String): Result<Unit> = runCatchingCancellable {
        val clean = email.trim()
        if (!clean.contains("@") || !clean.contains(".")) {
            throw IllegalStateException("Enter a valid email address")
        }
        val body = com.example.lampstand.data.remote.ConvexBodies.action(
            "auth:signIn",
            kotlinx.serialization.json.buildJsonObject {
                put("provider", kotlinx.serialization.json.JsonPrimitive("resend-otp"))
                put(
                    "params",
                    kotlinx.serialization.json.buildJsonObject {
                        put("email", kotlinx.serialization.json.JsonPrimitive(clean))
                    }
                )
            }
        )
        com.example.lampstand.data.remote.ConvexClient.valueOf(convexApi.action(body))
        Unit
    }

    /** Step 2: verify the 4-digit code -> Convex session (same as web). */
    suspend fun verifyOtp(email: String, code: String): Result<String> = runCatchingCancellable {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 4) throw IllegalStateException("Enter the 4-digit code")
        val body = com.example.lampstand.data.remote.ConvexBodies.action(
            "auth:signIn",
            kotlinx.serialization.json.buildJsonObject {
                put(
                    "params",
                    kotlinx.serialization.json.buildJsonObject {
                        put("email", kotlinx.serialization.json.JsonPrimitive(email.trim()))
                        put("code", kotlinx.serialization.json.JsonPrimitive(digits))
                    }
                )
            }
        )
        val value = com.example.lampstand.data.remote.ConvexClient.valueOf(convexApi.action(body))
            as kotlinx.serialization.json.JsonObject
        val tok = value["tokens"] as? kotlinx.serialization.json.JsonObject
            ?: throw IllegalStateException("Invalid or expired code — try again")
        val token = (tok["token"] as kotlinx.serialization.json.JsonPrimitive).content
        val refresh = (tok["refreshToken"] as kotlinx.serialization.json.JsonPrimitive).content
        tokenStore.save(token, refresh)
        token
    }

    /** Step-1 verifier, kept across the Custom-Tab round-trip (cleared on use). */
    fun saveOAuthVerifier(verifier: String?) {
        try {
            val e = cachePrefs?.edit() ?: return
            if (verifier == null) e.remove(KEY_OAUTH_VERIFIER) else e.putString(KEY_OAUTH_VERIFIER, verifier)
            e.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    fun consumeOAuthVerifier(): String? {
        val v = try {
            cachePrefs?.getString(KEY_OAUTH_VERIFIER, null)
        } catch (_: Exception) {
            null
        }
        saveOAuthVerifier(null)
        return v?.takeIf { it.isNotBlank() }
    }

    fun hasConvexSession(): Boolean = tokenStore.bearerOrNull() != null

    /** Local copy of users:me.hasCompletedOnboarding; null = never checked. */
    fun cachedOnboardingCompleted(): Boolean? {
        val prefs = cachePrefs ?: return null
        if (!prefs.contains(KEY_ONBOARDING_DONE)) return null
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun saveOnboardingCompleted(completed: Boolean) {
        try {
            cachePrefs?.edit()?.putBoolean(KEY_ONBOARDING_DONE, completed)?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    /**
     * Refreshes an expired JWT with the stored refresh token. Returns true
     * while the session remains usable. Only a backend rejection clears the
     * local session; an offline refresh keeps the existing tokens.
     */
    suspend fun refreshSessionIfNeeded(): Boolean {
        if (tokenStore.tokenOrNull() == null) return false
        if (!tokenStore.needsRefresh()) return true

        val refresh = tokenStore.refreshTokenOrNull() ?: run {
            signOutEverywhere()
            return false
        }

        return try {
            val value = ConvexClient.valueOf(
                convexApi.action(
                    com.example.lampstand.data.remote.ConvexBodies.action(
                        "auth:signIn",
                        kotlinx.serialization.json.buildJsonObject {
                            put("refreshToken", JsonPrimitive(refresh))
                        }
                    )
                )
            ) as? JsonObject ?: throw IllegalStateException("Invalid refresh response")
            val tokens = value["tokens"] as? JsonObject
                ?: throw IllegalStateException("Missing tokens in refresh response")
            val newToken = (tokens["token"] as? JsonPrimitive)?.content
                ?: throw IllegalStateException("Missing refreshed token")
            val newRefresh = (tokens["refreshToken"] as? JsonPrimitive)?.content
                ?: throw IllegalStateException("Missing refreshed refresh token")
            tokenStore.save(newToken, newRefresh)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: IOException) {
            // Offline: keep the session; cached data still works.
            true
        } catch (_: Exception) {
            signOutEverywhere()
            false
        }
    }

    /**
     * Mirrors web profile page signOut(): invalidate the Convex session
     * server-side first (auth:signOut), then drop every local trace of it.
     * A failure on the network call is not fatal — the local clear still runs,
     * so the user always ends up signed out.
     */
    suspend fun signOut(): Result<Unit> = runCatchingCancellable {
        tokenStore.bearerOrNull()?.let { bearer ->
            try {
                ConvexClient.valueOf(
                    convexApi.action(
                        com.example.lampstand.data.remote.ConvexBodies.action(
                            "auth:signOut",
                            kotlinx.serialization.json.buildJsonObject {}
                        ),
                        bearer
                    )
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Offline / already-expired token: local sign-out below still applies.
            }
        }
        signOutEverywhere()
        Unit
    }

    fun signOutEverywhere() {
        tokenStore.clear()
        saveOAuthVerifier(null)
        // Drop per-account formation caches too — they are global keys, so a
        // different account signing in next would otherwise see this user's
        // profile/home until the network refreshed it.
        clearFormationCache()
    }

    private fun clearFormationCache() {
        try {
            val p = cachePrefs ?: return
            val e = p.edit()
            e.remove("cache_discovery_v1")
            e.remove("cache_home_v1")
            e.remove("cache_me_v1")
            e.remove(KEY_ONBOARDING_DONE)
            e.remove("reminder_slot")
            e.remove("reminder_hour")
            e.remove("reminder_minute")
            p.all.keys.filter { it.startsWith("cache_overview_v1_") }.forEach { e.remove(it) }
            e.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    private val convexFormation: FormationRepository by lazy {
        ConvexFormationRepository(convexApi, cachePrefs, tokenStore)
    }

    /**
     * Dashboard + Paths read the live cloud — the same backend the web app
     * uses. Unauthenticated queries still work (discovery/overview/home fall
     * back to their logged-out shapes); anything needing an account answers
     * with the backend's own error.
     */
    val formationRepository: FormationRepository get() = convexFormation

    /**
     * Profile-screen save. Same mutation the web /profile/personal-information
     * page calls (users:updateProfile), so the name + phone written here are the
     * ones users:me returns (Dashboard greeting + Profile header).
     * A blank phone is omitted rather than sent as null — same as the web page's
     * `phoneNumber: phone || undefined`, so an empty field never wipes a number
     * set during onboarding.
     */
    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String? = null
    ): Result<Unit> =
        runCatchingCancellable {
            val bearer = tokenStore.bearerOrNull()
                ?: throw IllegalStateException("Not signed in — sign in again to edit your profile")
            val args = kotlinx.serialization.json.buildJsonObject {
                put("firstName", kotlinx.serialization.json.JsonPrimitive(firstName.trim()))
                put("lastName", kotlinx.serialization.json.JsonPrimitive(lastName.trim()))
                phone?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    put("phoneNumber", kotlinx.serialization.json.JsonPrimitive(it))
                }
            }
            ConvexClient.valueOf(
                convexApi.mutation(
                    com.example.lampstand.data.remote.ConvexBodies.mutation("users:updateProfile", args),
                    bearer
                )
            )
            Unit
        }

    /**
     * Web study-time slots → the exact strings users:updateNotificationPreferences
     * (and users:completeOnboarding) expect on the backend.
     */
    private val slotNames = listOf("early_morning", "afternoon", "evening", "before_bed")
    fun studyTimeForSlot(slot: Int): String = slotNames.getOrElse(slot) { "early_morning" }

    /** Index of a stored studyTimePreference, for pre-selecting the edit screen. */
    fun slotForStudyTime(value: String?): Int =
        slotNames.indexOf(value ?: "").takeIf { it >= 0 } ?: 0

    /**
     * Reminder toggle from the profile edit screen → backend preferences
     * (users:updateNotificationPreferences, same as web). The exact clock time
     * stays local — the backend only stores the slot + contact method.
     */
    suspend fun updateNotificationPreferences(
        studyTimePreference: String,
        contactPreference: String,
        phone: String? = null,
        timezone: String
    ): Result<Unit> = runCatchingCancellable {
        val bearer = tokenStore.bearerOrNull()
            ?: throw IllegalStateException("Not signed in — sign in again to edit your profile")
        val args = kotlinx.serialization.json.buildJsonObject {
            put("studyTimePreference", kotlinx.serialization.json.JsonPrimitive(studyTimePreference))
            put("contactPreference", kotlinx.serialization.json.JsonPrimitive(contactPreference.ifBlank { "email" }))
            phone?.trim()?.takeIf { it.isNotEmpty() }?.let {
                put("phoneNumber", kotlinx.serialization.json.JsonPrimitive(it))
            }
            put("timezone", kotlinx.serialization.json.JsonPrimitive(timezone.ifBlank { "UTC" }))
        }
        ConvexClient.valueOf(
            convexApi.mutation(
                com.example.lampstand.data.remote.ConvexBodies.mutation(
                    "users:updateNotificationPreferences", args
                ),
                bearer
            )
        )
        Unit
    }

    /** Local-only reminder clock time (the backend stores no clock-time field). */
    data class ReminderSettings(val enabled: Boolean, val slot: Int, val hour: Int, val minute: Int)

    fun reminderSettings(): ReminderSettings? {
        val raw = try {
            cachePrefs?.getString("reminder_slot", null)
        } catch (_: Exception) {
            null
        } ?: return null
        val hour = try {
            cachePrefs?.getInt("reminder_hour", -1) ?: -1
        } catch (_: Exception) {
            -1
        }
        val minute = try {
            cachePrefs?.getInt("reminder_minute", -1) ?: -1
        } catch (_: Exception) {
            -1
        }
        return ReminderSettings(enabled = true, slot = slotForStudyTime(raw), hour = hour, minute = minute)
    }

    /** Persist the exact notification time locally (backend has no clock-time field). */
    fun saveReminderTime(slot: String, hour: Int, minute: Int) {
        if (hour !in 0..23 || minute !in 0..59) return
        try {
            cachePrefs?.edit()
                ?.putString("reminder_slot", slot)
                ?.putInt("reminder_hour", hour)
                ?.putInt("reminder_minute", minute)
                ?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    // ── Onboarding (same mutations as web, zero backend changes) ──
    // Backend gets only the slot string (early_morning|afternoon|evening|before_bed);
    // the exact clock time is local-only for the mobile notification.
    suspend fun submitOnboarding(
        firstName: String,
        lastName: String,
        phoneE164: String?,
        spiritualSeason: String,
        studyTimePreference: String,
        contactPreference: String = "email",
        timezone: String,
        reminderHour: Int = -1,
        reminderMinute: Int = -1
    ): Result<Unit> = runCatchingCancellable {
        val bearer = tokenStore.bearerOrNull()
            ?: throw IllegalStateException("Not signed in — sign in with Google first")
        val profileArgs = kotlinx.serialization.json.buildJsonObject {
            put("firstName", kotlinx.serialization.json.JsonPrimitive(firstName))
            put("lastName", kotlinx.serialization.json.JsonPrimitive(lastName))
        }
        ConvexClient.valueOf(
            convexApi.mutation(
                com.example.lampstand.data.remote.ConvexBodies.mutation("users:updateProfile", profileArgs),
                bearer
            )
        )
        val onboardArgs = kotlinx.serialization.json.buildJsonObject {
            put("spiritualSeason", kotlinx.serialization.json.JsonPrimitive(spiritualSeason))
            put("studyTimePreference", kotlinx.serialization.json.JsonPrimitive(studyTimePreference))
            put("contactPreference", kotlinx.serialization.json.JsonPrimitive(contactPreference.ifBlank { "email" }))
            if (!phoneE164.isNullOrBlank()) {
                put("phoneNumber", kotlinx.serialization.json.JsonPrimitive(phoneE164))
            }
            put("timezone", kotlinx.serialization.json.JsonPrimitive(timezone.ifBlank { "UTC" }))
        }
        ConvexClient.valueOf(
            convexApi.mutation(
                com.example.lampstand.data.remote.ConvexBodies.mutation("users:completeOnboarding", onboardArgs),
                bearer
            )
        )
        // Local-only: persist the exact notification time (backend has no clock-time field).
        saveReminderTime(studyTimePreference, reminderHour, reminderMinute)
        saveOnboardingCompleted(true)
        Unit
    }

    /** users:me — null = signed out; check hasCompletedOnboarding to route. */
    suspend fun fetchMeHasCompletedOnboarding(): Boolean? {
        val bearer = tokenStore.bearerOrNull() ?: return null
        return try {
            val v = ConvexClient.valueOf(
                convexApi.query(
                    com.example.lampstand.data.remote.ConvexBodies.query(
                        "users:me",
                        kotlinx.serialization.json.buildJsonObject {}
                    ),
                    bearer
                )
            ) as? kotlinx.serialization.json.JsonObject ?: return null
            val completed = (v["hasCompletedOnboarding"] as? JsonPrimitive)
                ?.booleanOrNull ?: false
            saveOnboardingCompleted(completed)
            completed
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
