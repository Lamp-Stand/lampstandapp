package com.example.lampstand.data

/**
 * Display-only identity for the UI layer.
 *
 * The real account lives in Convex (the JWT in the token store plus
 * `users:me`). This is only the shape the screens were built around, so the
 * dashboard/profile can render a name, email and phone without every
 * composable having to unwrap `MeDto` itself.
 */
data class LampUser(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String
)
