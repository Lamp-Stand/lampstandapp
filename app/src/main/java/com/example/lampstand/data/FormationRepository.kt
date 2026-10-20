package com.example.lampstand.data

import com.example.lampstand.data.remote.CompleteSessionResult
import com.example.lampstand.data.remote.DiscoveryItem
import com.example.lampstand.data.remote.HomeStateDto
import com.example.lampstand.data.remote.MeDto
import com.example.lampstand.data.remote.PathOverviewDto
import com.example.lampstand.data.remote.SessionContentDto

/**
 * Phase 1 read-only contract. Mirrors the public Convex bridge
 * (mobile/discovery, mobile/path-overview). No auth required.
 * Dashboard also reads paths:getHomeState + users:me (same cloud,
 * works logged-out → unauthenticated/no_path).
 */
interface FormationRepository {
    suspend fun getDiscovery(): Result<List<DiscoveryItem>>
    suspend fun getOverview(pathId: String): Result<PathOverviewDto>
    suspend fun getHomeState(): Result<HomeStateDto>
    suspend fun getMe(): Result<MeDto?>
    /** Coming-soon interest (web: discovery:recordTap). Returns new totalTaps. */
    suspend fun recordTap(pathId: String): Result<Int>
    /** Start/resume live path (web: paths:startPath auto-start on overview). */
    suspend fun startPath(pathId: String): Result<Unit>
    /** Today's session content (web: paths:getSessionContent, lock enforced server-side). */
    suspend fun getSessionContent(sessionId: String): Result<SessionContentDto>
    /** Complete session (web: GuidedPrayerSection → paths:completeSession, idempotent). */
    suspend fun completeSession(sessionId: String): Result<CompleteSessionResult>
}
