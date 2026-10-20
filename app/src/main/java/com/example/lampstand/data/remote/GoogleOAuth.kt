package com.example.lampstand.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Google login through the backend's own Convex Auth flow
 * (@convex-dev/auth 0.0.92, verified against its implementation):
 *
 * 1. `beginBrowserOAuth` → action `auth:signIn`
 *    {provider:"google", params:{redirectTo}} returns
 *    {redirect:"<convex.site>/api/auth/signin/google?...", verifier}.
 *    NOTE: passing params WITHOUT `code` always returns a redirect,
 *    never tokens — the library has no `idToken` param, which is why
 *    the old Credential-Manager exchange failed with "No tokens".
 * 2. App opens `redirect` in a Custom Tab → Google consent →
 *    Convex callback exchanges the Google code, then 302s to
 *    `REDIRECT_URI?code=<verificationCode>`.
 *    (Requires the backend's auth redirect callback to allowlist the
 *    `com.example.lampstand://` scheme — default validator only
 *    allows SITE_URL and throws "Invalid redirectTo" otherwise.
 *    Google itself never sees the custom scheme.)
 * 3. `finishBrowserOAuth` → action `auth:signIn`
 *    {params:{code}, verifier} (same shape the web client uses in
 *    verifyCode) returns {tokens:{token, refreshToken}}.
 */
object GoogleOAuth {
    const val REDIRECT_URI = "com.example.lampstand://oauth2redirect"

    data class OAuthStep1(val redirectUrl: String, val verifier: String)

    /** Step 1: ask Convex for the Google sign-in URL + round-trip verifier. */
    suspend fun beginBrowserOAuth(api: ConvexApi): Result<OAuthStep1> = try {
        val body = ConvexBodies.action(
            "auth:signIn",
            buildJsonObject {
                put("provider", JsonPrimitive("google"))
                put(
                    "params",
                    buildJsonObject {
                        put("redirectTo", JsonPrimitive(REDIRECT_URI))
                    }
                )
            }
        )
        val value = ConvexClient.valueOf(api.action(body)).jsonObject
        val redirect = value["redirect"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No redirect in response: $value")
        val verifier = value["verifier"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No verifier in response: $value")
        Result.success(OAuthStep1(redirect, verifier))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Step 3: swap the ?code= from the deep link + step-1 verifier for a session. */
    suspend fun finishBrowserOAuth(
        api: ConvexApi,
        tokens: ConvexTokenStore,
        code: String,
        verifier: String
    ): Result<String> = try {
        val body = ConvexBodies.action(
            "auth:signIn",
            buildJsonObject {
                put(
                    "params",
                    buildJsonObject {
                        put("code", JsonPrimitive(code))
                    }
                )
                put("verifier", JsonPrimitive(verifier))
            }
        )
        val value = ConvexClient.valueOf(api.action(body)).jsonObject
        val tok = value["tokens"]?.jsonObject
            ?: throw IllegalStateException("No tokens in response: $value")
        val token = tok.getValue("token").jsonPrimitive.content
        val refresh = tok.getValue("refreshToken").jsonPrimitive.content
        tokens.save(token, refresh)
        Result.success(token)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun JsonObject.getValue(key: String) =
    this[key] ?: throw IllegalStateException("Missing '$key' in $this")
