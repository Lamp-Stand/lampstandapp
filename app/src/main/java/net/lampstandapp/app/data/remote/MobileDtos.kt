package net.lampstandapp.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryItem(
    val pathId: String,
    val title: String,
    val description: String,
    val category: String,
    val isLive: Boolean,
    val totalTaps: Double = 0.0,
    val userHasTapped: Boolean = false
)

@Serializable
data class SessionStub(
    val sessionId: String,
    val dayNumber: Double,
    val title: String,
    val status: String // "completed" | "available" | "locked"
)

@Serializable
data class PathOverviewDto(
    val pathId: String,
    val title: String,
    val description: String,
    val isLive: Boolean,
    val totalDays: Double,
    val category: String,
    val userPathId: String? = null,
    val startedAt: Double? = null,
    val sessions: List<SessionStub> = emptyList()
)

@Serializable
data class OverviewRequest(val pathId: String)

@Serializable
data class HomeStateDto(
    val state: String, // "unauthenticated" | "no_path" | "active" | "complete"
    val pathId: String? = null,
    val pathTitle: String? = null,
    val currentDay: Double? = null,
    val totalDays: Double? = null,
    val todayDone: Boolean? = null,
    val sessionId: String? = null
)

@Serializable
data class MeDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val spiritualSeason: String? = null,
    val studyTimePreference: String? = null,
    val contactPreference: String? = null,
    val timezone: String? = null,
    val hasCompletedOnboarding: Boolean? = null
)

@Serializable
data class SessionContentDto(
    val sessionId: String = "",
    val dayNumber: Double = 1.0,
    val title: String = "",
    val scriptureReference: String? = null,
    val scriptureVerse: String? = null,
    val scriptureContext: String? = null,
    val insightText: String? = null,
    val applicationText: String? = null,
    val reflectionQuestions: List<String> = emptyList(),
    val guidedPrayer: String? = null,
    // Convex spreads ...session so id may arrive as _id; keep both.
    val _id: String? = null
)

@Serializable
data class CompleteSessionResult(
    val nextDay: Double = 1.0,
    val pathComplete: Boolean = false,
    val completedDays: List<Double> = emptyList(),
    val isNew: Boolean = true
)
