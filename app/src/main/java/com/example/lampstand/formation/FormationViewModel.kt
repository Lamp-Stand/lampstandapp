package com.example.lampstand.formation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lampstand.data.FormationRepository
import com.example.lampstand.data.ServiceLocator
import com.example.lampstand.data.remote.CompleteSessionResult
import com.example.lampstand.data.remote.DiscoveryItem
import com.example.lampstand.data.remote.HomeStateDto
import com.example.lampstand.data.remote.MeDto
import com.example.lampstand.data.remote.PathOverviewDto
import com.example.lampstand.data.remote.SessionContentDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface DiscoveryUi {
    data object Loading : DiscoveryUi
    data class Content(val items: List<DiscoveryItem>, val offline: Boolean = false) : DiscoveryUi
    data class Error(val message: String) : DiscoveryUi
}

sealed interface OverviewUi {
    data object Loading : OverviewUi
    data class Content(val overview: PathOverviewDto) : OverviewUi
    data class Error(val message: String) : OverviewUi
}

sealed interface HomeUi {
    data object Loading : HomeUi
    data class Content(val home: HomeStateDto, val firstName: String?, val recommendationCount: Int) : HomeUi
    data class Error(val message: String) : HomeUi
}

sealed interface SessionUi {
    data object Loading : SessionUi
    data class Content(val session: SessionContentDto) : SessionUi
    data class Error(val message: String) : SessionUi
}

class FormationViewModel : ViewModel() {
    // Resolved per-call so the repo always sends the current session's token.
    private val repo: FormationRepository get() = ServiceLocator.formationRepository
    private val _discovery = MutableStateFlow<DiscoveryUi>(DiscoveryUi.Loading)
    val discovery: StateFlow<DiscoveryUi> = _discovery

    private val _overview = MutableStateFlow<OverviewUi>(OverviewUi.Loading)
    val overview: StateFlow<OverviewUi> = _overview

    private val _home = MutableStateFlow<HomeUi>(HomeUi.Loading)
    val home: StateFlow<HomeUi> = _home

    private val _session = MutableStateFlow<SessionUi>(SessionUi.Loading)
    val session: StateFlow<SessionUi> = _session

    /** Convex users:me — the signed-in identity. Kept separate from home so the
     *  Profile screen still populates when paths:getHomeState fails. */
    private val _me = MutableStateFlow<MeDto?>(null)
    val me: StateFlow<MeDto?> = _me

    fun loadDiscovery() {
        viewModelScope.launch {
            // Stale-while-revalidate: a Convex useQuery keeps its last value
            // while a refetch is in flight, so the list must not blank to the
            // hardcoded fallback every time it refreshes (it would blow away a
            // coming-soon card's tapped/confirmation state mid-tap).
            if (_discovery.value !is DiscoveryUi.Content) {
                _discovery.value = DiscoveryUi.Loading
            }
            repo.getDiscovery()
                .onSuccess { _discovery.value = DiscoveryUi.Content(it) }
                .onFailure { _discovery.value = DiscoveryUi.Error(it.message ?: "Couldn't load paths") }
        }
    }

    fun loadOverview(pathId: String, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _overview.value !is OverviewUi.Content) {
                _overview.value = OverviewUi.Loading
            }
            repo.getOverview(pathId)
                .onSuccess { _overview.value = OverviewUi.Content(it) }
                .onFailure {
                    android.util.Log.w("Formation", "getPathOverview($pathId) failed", it)
                    // Keep the last good overview on screen during a refresh.
                    if (_overview.value !is OverviewUi.Content) {
                        _overview.value = OverviewUi.Error(it.message ?: "Couldn't load path")
                    }
                }
        }
    }

    /** Mirrors web Dashboard: users:me + paths:getHomeState + discovery count. */
    fun loadHome() {
        viewModelScope.launch {
            // Convex useQuery keeps the last value on screen while it
            // revalidates — don't blank the dashboard back to Loading on
            // every refresh (that's what made Home flash the empty state).
            if (_home.value !is HomeUi.Content) _home.value = HomeUi.Loading
            val homeR = repo.getHomeState()
            val meR = repo.getMe()
            val discR = repo.getDiscovery()
            // users:me first — the Profile screen must populate even when
            // paths:getHomeState fails (otherwise the tab renders empty).
            val me: MeDto? = meR.getOrNull()
            if (meR.isSuccess) _me.value = me
            val home = homeR.getOrNull()
            if (home == null) {
                if (_home.value !is HomeUi.Content) {
                    _home.value = HomeUi.Error(homeR.exceptionOrNull()?.message ?: "Couldn't load home")
                }
                return@launch
            }
            val firstName = me?.firstName?.takeIf { it.isNotBlank() }
                ?: me?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
            val items = discR.getOrNull() ?: emptyList()
            val fearCount = items.count { it.category == "Fear & Anxiety" }.takeIf { it > 0 } ?: 6
            _home.value = HomeUi.Content(home, firstName, fearCount)
        }
    }

    /** Refresh just users:me (profile edit, sign-in, cold start). */
    fun loadMe() {
        viewModelScope.launch {
            repo.getMe().onSuccess { _me.value = it }
        }
    }

    /** Wipe everything on sign-out — the VM outlives the session (activity-scoped),
     *  so without this the next account would briefly see the previous one's data. */
    fun reset() {
        _discovery.value = DiscoveryUi.Loading
        _overview.value = OverviewUi.Loading
        _home.value = HomeUi.Loading
        _session.value = SessionUi.Loading
        _me.value = null
    }

    /** Web ComingSoonPathcard.handleTap: recordTap then refresh discovery (counter + tapped). */
    fun tapComingSoon(pathId: String, onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val r = repo.recordTap(pathId)
            r.onSuccess { loadDiscovery(); loadHome() }
            onResult(r)
        }
    }

    /** Web PathDetail auto-start: paths:startPath then reload overview + home. */
    fun startPath(pathId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val r = repo.startPath(pathId)
            r.onSuccess { loadOverview(pathId); loadHome() }
            onResult(r)
        }
    }

    /** Web session page: paths:getSessionContent (lock enforced server-side). */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _session.value = SessionUi.Loading
            repo.getSessionContent(sessionId)
                .onSuccess { _session.value = SessionUi.Content(it) }
                .onFailure { _session.value = SessionUi.Error(it.message ?: "Session not found") }
        }
    }

    /** Web GuidedPrayerSection: paths:completeSession → toast → next/feedback. */
    fun completeSession(sessionId: String, onResult: (Result<CompleteSessionResult>) -> Unit = {}) {
        viewModelScope.launch {
            val r = repo.completeSession(sessionId)
            r.onSuccess { loadHome(); loadDiscovery() }
            onResult(r)
        }
    }

    /** Live path first, then Coming Soon A–Z (mirrors web ordering for one category). */
    fun fearAnxiety(items: List<DiscoveryItem>): List<DiscoveryItem> {
        val mine = items.filter { it.category == "Fear & Anxiety" }
        val src = if (mine.isEmpty()) items else mine
        return src.sortedWith(compareByDescending<DiscoveryItem> { it.isLive }.thenBy { it.title })
    }
}
