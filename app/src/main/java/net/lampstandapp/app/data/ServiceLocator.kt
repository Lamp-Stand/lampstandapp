package net.lampstandapp.app.data

import android.content.Context
import net.lampstandapp.app.data.remote.ConvexClient
import net.lampstandapp.app.data.remote.ConvexConfig
import net.lampstandapp.app.data.remote.ConvexTokenStore
import net.lampstandapp.app.data.remote.HomeStateDto
import java.io.IOException
import java.util.Calendar
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
    private const val KEY_HOME_CACHE = "cache_home_v1"
    private const val KEY_HOME_CACHE_AT = "cache_home_at_v1"
    private const val KEY_LAST_SESSION_COMPLETED = "last_session_completed_at_v1"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled_v1"
    private const val KEY_LAST_APP_OPEN = "last_app_open_at_v1"
    private const val KEY_LAST_REMINDER_AT = "last_reminder_at_v1"
    private const val KEY_REMINDER_MESSAGE_INDEX = "reminder_message_index_v1"

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
    suspend fun beginGoogleOAuth(): Result<net.lampstandapp.app.data.remote.GoogleOAuth.OAuthStep1> =
        net.lampstandapp.app.data.remote.GoogleOAuth.beginBrowserOAuth(convexApi)

    /** Step 3: ?code= from the deep link + step-1 verifier -> Convex session. */
    suspend fun finishGoogleOAuth(code: String, verifier: String): Result<String> =
        net.lampstandapp.app.data.remote.GoogleOAuth.finishBrowserOAuth(
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
        val body = net.lampstandapp.app.data.remote.ConvexBodies.action(
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
        net.lampstandapp.app.data.remote.ConvexClient.valueOf(convexApi.action(body))
        Unit
    }

    /** Step 2: verify the 4-digit code -> Convex session (same as web). */
    suspend fun verifyOtp(email: String, code: String): Result<String> = runCatchingCancellable {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 4) throw IllegalStateException("Enter the 4-digit code")
        val body = net.lampstandapp.app.data.remote.ConvexBodies.action(
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
        val value = net.lampstandapp.app.data.remote.ConvexClient.valueOf(convexApi.action(body))
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
                    net.lampstandapp.app.data.remote.ConvexBodies.action(
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
                        net.lampstandapp.app.data.remote.ConvexBodies.action(
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
                    net.lampstandapp.app.data.remote.ConvexBodies.mutation("users:updateProfile", args),
                    bearer
                )
            )
            Unit
        }

    data class DonationCheckout(val checkoutUrl: String, val donationId: String)

    /**
     * Creates a Flutterwave checkout through the same Convex actions the web
     * support page uses. The returned URL is opened in a Custom Tab; Flutterwave
     * redirects to the Convex site, which forwards back into the app.
     */
    suspend fun initializeDonation(
        kind: String,
        currency: String,
        amountMinor: Long? = null,
        tierKey: String? = null
    ): Result<DonationCheckout> = runCatchingCancellable {
        refreshSessionIfNeeded()
        val bearer = tokenStore.bearerOrNull()
        val returnUrl = ConvexConfig.siteUrl.trimEnd('/') + "/mobile/donation-return"
        val monthly = kind == "monthly"
        val args = kotlinx.serialization.json.buildJsonObject {
            put("triggerContext", JsonPrimitive("post_debrief"))
            put("returnUrl", JsonPrimitive(returnUrl))
            if (monthly) {
                put(
                    "tierKey",
                    JsonPrimitive(
                        tierKey ?: throw IllegalStateException("Choose a monthly tier first")
                    )
                )
            } else {
                put("currency", JsonPrimitive(currency))
                put(
                    "amountMinor",
                    JsonPrimitive(
                        amountMinor?.takeIf { it > 0 }
                            ?: throw IllegalStateException("Enter a valid amount")
                    )
                )
            }
        }
        val actionPath = if (monthly) {
            "donations:initializeMonthlyDonation"
        } else {
            "donations:initializeOneTimeDonation"
        }
        val value = ConvexClient.valueOf(
            convexApi.action(
                net.lampstandapp.app.data.remote.ConvexBodies.action(actionPath, args),
                bearer
            )
        ) as? JsonObject ?: throw IllegalStateException("Invalid checkout response")
        val checkoutUrl = (value["checkoutUrl"] as? JsonPrimitive)?.content
            ?: throw IllegalStateException("Checkout URL missing")
        val donationId = (value["donationId"] as? JsonPrimitive)?.content ?: ""
        DonationCheckout(checkoutUrl, donationId)
    }

    /** Confirms the transaction after Flutterwave redirects back into the app. */
    suspend fun verifyDonation(transactionId: String): Result<String> = runCatchingCancellable {
        val args = kotlinx.serialization.json.buildJsonObject {
            put("transactionId", JsonPrimitive(transactionId))
        }
        val value = ConvexClient.valueOf(
            convexApi.action(
                net.lampstandapp.app.data.remote.ConvexBodies.action(
                    "donations:verifyDonationByTransactionId", args
                ),
                tokenStore.bearerOrNull()
            )
        ) as? JsonObject ?: throw IllegalStateException("Invalid verification response")
        (value["status"] as? JsonPrimitive)?.content ?: "pending"
    }

    /**
     * Web study-time slots -> the exact strings users:updateNotificationPreferences
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
                net.lampstandapp.app.data.remote.ConvexBodies.mutation(
                    "users:updateNotificationPreferences", args
                ),
                bearer
            )
        )
        Unit
    }

    /**
     * Reminders are on by default; the switch in Profile is the only opt-out.
     * Keeping this as an explicit flag (rather than inferring from saved times)
     * means an off choice survives restarts and updates.
     */
    fun reminderEnabled(): Boolean = try {
        cachePrefs?.getBoolean(KEY_REMINDER_ENABLED, true) ?: true
    } catch (_: Exception) {
        true
    }

    fun setReminderEnabled(enabled: Boolean) {
        try {
            cachePrefs?.edit()?.putBoolean(KEY_REMINDER_ENABLED, enabled)?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    /** Launching the app (including starting a session) marks the day as opened. */
    fun markAppOpened() {
        try {
            cachePrefs?.edit()?.putLong(KEY_LAST_APP_OPEN, System.currentTimeMillis())?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    fun appOpenedToday(): Boolean = isToday(lastAppOpenAt())

    private fun lastAppOpenAt(): Long = try {
        cachePrefs?.getLong(KEY_LAST_APP_OPEN, 0L) ?: 0L
    } catch (_: Exception) {
        0L
    }

    fun lastReminderAt(): Long = try {
        cachePrefs?.getLong(KEY_LAST_REMINDER_AT, 0L) ?: 0L
    } catch (_: Exception) {
        0L
    }

    fun recordReminderShown() {
        try {
            cachePrefs?.edit()?.putLong(KEY_LAST_REMINDER_AT, System.currentTimeMillis())?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    /** Rotate through a message pool so the same nudge is not repeated. */
    fun nextReminderMessageIndex(poolSize: Int): Int {
        if (poolSize <= 0) return 0
        val current = try {
            cachePrefs?.getInt(KEY_REMINDER_MESSAGE_INDEX, 0) ?: 0
        } catch (_: Exception) {
            0
        }
        val index = ((current % poolSize) + poolSize) % poolSize
        try {
            cachePrefs?.edit()?.putInt(KEY_REMINDER_MESSAGE_INDEX, (index + 1) % poolSize)?.apply()
        } catch (_: Exception) { /* best-effort */ }
        return index
    }

    /** True when the whole active path is finished, so reminders should stop. */
    fun isPathComplete(): Boolean = cachedHomeState()?.state == "complete"

    /** True once today's session is done, locally or per today's cached home state. */
    fun todaySessionCompleted(): Boolean {
        if (completedSessionToday()) return true
        val home = cachedHomeState() ?: return false
        // Only trust todayDone while the cached home snapshot is from today;
        // yesterday's "done" must not silence the new day's reminders.
        val cachedAt = try {
            cachePrefs?.getLong(KEY_HOME_CACHE_AT, 0L) ?: 0L
        } catch (_: Exception) {
            0L
        }
        return isToday(cachedAt) && home.todayDone == true
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return then.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    /** Last paths:getHomeState response, so reminders can mirror web's "only while active". */
    fun cachedHomeState(): HomeStateDto? {
        val cached = try {
            cachePrefs?.getString(KEY_HOME_CACHE, null)
        } catch (_: Exception) {
            null
        } ?: return null
        return try {
            ConvexClient.json.decodeFromString(HomeStateDto.serializer(), cached)
        } catch (_: Exception) {
            null
        }
    }

    /** Record completion locally so the daily nudge can skip a day already done. */
    fun recordSessionCompletedNow() {
        try {
            cachePrefs?.edit()
                ?.putLong(KEY_LAST_SESSION_COMPLETED, System.currentTimeMillis())
                ?.apply()
        } catch (_: Exception) { /* best-effort */ }
    }

    fun completedSessionToday(): Boolean {
        val last = try {
            cachePrefs?.getLong(KEY_LAST_SESSION_COMPLETED, 0L) ?: 0L
        } catch (_: Exception) {
            0L
        }
        return isToday(last)
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
                net.lampstandapp.app.data.remote.ConvexBodies.mutation("users:updateProfile", profileArgs),
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
                net.lampstandapp.app.data.remote.ConvexBodies.mutation("users:completeOnboarding", onboardArgs),
                bearer
            )
        )
        // Reminders start automatically after onboarding; Profile has the opt-out.
        setReminderEnabled(true)
        saveOnboardingCompleted(true)
        Unit
    }

    /** users:me — null = signed out; check hasCompletedOnboarding to route. */
    suspend fun fetchMeHasCompletedOnboarding(): Boolean? {
        val bearer = tokenStore.bearerOrNull() ?: return null
        return try {
            val v = ConvexClient.valueOf(
                convexApi.query(
                    net.lampstandapp.app.data.remote.ConvexBodies.query(
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
