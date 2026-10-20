package com.example.lampstand

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.lampstand.auth.CreateAccountScreen
import com.example.lampstand.auth.DashboardSignedOutScreen
import com.example.lampstand.auth.LoadingScreen
import com.example.lampstand.auth.LoginScreen
import com.example.lampstand.auth.OnboardingProfileScreen
import com.example.lampstand.dashboard.DashboardScreen
import com.example.lampstand.data.ServiceLocator
import com.example.lampstand.journey.JourneyDashboardScreen
import com.example.lampstand.journey.JourneyDashboardSession
import com.example.lampstand.journey.JourneyListScreen
import com.example.lampstand.journey.JourneyScreen
import com.example.lampstand.paths.FandAPathScreen
import com.example.lampstand.paths.PathsScreen
import com.example.lampstand.profile.ProfileEditScreen
import com.example.lampstand.profile.ProfileScreen
import com.example.lampstand.ui.theme.LampStandTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthRoute { SignedOut, SignUp, LogIn }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Cold start via OAuth deep link (app was not running).
        intent?.data?.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
            oauthCode.value = it
        }

        setContent {
            LampStandApp()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
            oauthCode.value = it
        }
    }

    companion object {
        /** ?code= captured from the OAuth deep link; consumed once by LampStandApp. */
        val oauthCode = androidx.compose.runtime.mutableStateOf<String?>(null)
    }
}

/**
 * Whole app navigation host, extracted from onCreate so @Preview
 * (and interactive mode) renders the REAL flow instead of a
 * static screen. Activity stays thin.
 */
@Composable
fun LampStandApp(
    skipSplash: Boolean = false,
    onGoogleClick: (emailHint: String) -> Unit = {}
) {
    LampStandTheme(dynamicColor = false) {
                // Hoisted so splash timing never resets the auth destination
                // (keeps interactive preview + config changes from sticking on Loading).
                var splashDone by remember(skipSplash) { mutableStateOf(skipSplash) }
                LaunchedEffect(skipSplash) {
                    if (!skipSplash) {
                        delay(3000)
                        splashDone = true
                    }
                }
                // Identity is the Convex JWT (Google OAuth or email OTP) — there is
                // no local/mock account store anymore.
                val context = LocalContext.current
                ServiceLocator.init(context)
                var route by remember { mutableStateOf(AuthRoute.SignedOut) }
                // Email OTP state (web resend-otp parity; no backend change needed).
                var otpEmail by remember { mutableStateOf("") }
                var otpBusy by remember { mutableStateOf(false) }
                var otpError by remember { mutableStateOf<String?>(null) }
                // Signed-in = a Convex session exists (browser OAuth or email OTP).
                // googleSignedIn is the single source of truth for that.
                val hasStoredSession = remember { ServiceLocator.hasConvexSession() }
                var authReady by remember { mutableStateOf(!hasStoredSession) }
                var googleSignedIn by remember { mutableStateOf(hasStoredSession) }
                // Nullable: null = users:me hasn't answered yet. Cached locally so a
                // returning user goes straight to the app instead of flashing onboarding.
                var onboardingDone by remember { mutableStateOf(ServiceLocator.cachedOnboardingCompleted()) }
                var onboardLoading by remember { mutableStateOf(false) }
                var onboardError by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    if (hasStoredSession) {
                        val usable = ServiceLocator.refreshSessionIfNeeded()
                        googleSignedIn = usable
                        if (!usable) onboardingDone = null
                        authReady = true
                    }
                }
                LaunchedEffect(googleSignedIn, authReady) {
                    if (!authReady || !googleSignedIn) return@LaunchedEffect
                    val cached = ServiceLocator.cachedOnboardingCompleted()
                    if (cached != null) onboardingDone = cached
                    val done = ServiceLocator.fetchMeHasCompletedOnboarding()
                    when {
                        done != null -> onboardingDone = done
                        onboardingDone == null -> onboardingDone = false
                    }
                }
                // Shown for 3s after a successful sign-in / sign-up, before the dashboard.
                var sessionLoading by remember { mutableStateOf(false) }
                // Popup (title + message) for auth failures - e.g. wrong email/password.
                var authDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
                val scope = rememberCoroutineScope()
                fun authError(message: String) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                // Google sign-in: Custom Tab over Convex Auth's own OAuth URLs
                // (begin → consent in tab → deep link ?code= → session).
                fun handleGoogleClick() {
                    scope.launch {
                        sessionLoading = true
                        try {
                            val step1 = ServiceLocator.beginGoogleOAuth()
                            step1.onSuccess { (url, verifier) ->
                                ServiceLocator.saveOAuthVerifier(verifier)
                                try {
                                    val tabs = androidx.browser.customtabs.CustomTabsIntent.Builder()
                                        .setShowTitle(true)
                                        .build()
                                    val activity = context as? android.app.Activity
                                    tabs.launchUrl(activity ?: context, android.net.Uri.parse(url))
                                } catch (e: Exception) {
                                    ServiceLocator.saveOAuthVerifier(null)
                                    authDialog = "Google sign-in failed" to
                                        (e.message ?: "Couldn't open browser")
                                }
                            }.onFailure {
                                authDialog = "Google sign-in failed (step 1/3: sign-in URL)" to
                                    (it.message ?: "Try again")
                            }
                        } finally {
                            sessionLoading = false
                        }
                    }
                }
                // OAuth deep-link result: ?code= + step-1 verifier -> Convex session.
                // NOTE: do NOT clear oauthCode before the exchange finishes — oauthCode is
                // this effect's key, so writing null mid-effect cancels it at the next
                // suspension point and leaves sessionLoading stuck on the loading screen.
                val pendingOAuthCode by MainActivity.oauthCode
                LaunchedEffect(pendingOAuthCode) {
                    val code = pendingOAuthCode ?: return@LaunchedEffect
                    val verifier = ServiceLocator.consumeOAuthVerifier()
                    if (verifier == null) {
                        MainActivity.oauthCode.value = null
                        authDialog = "Google sign-in failed" to
                            "Session expired — please try again"
                        return@LaunchedEffect
                    }
                    sessionLoading = true
                    try {
                        ServiceLocator.finishGoogleOAuth(code, verifier)
                            .onSuccess { googleSignedIn = true }
                            .onFailure {
                                authDialog = "Google sign-in failed (step 3/3: session)" to
                                    (it.message ?: "Backend did not accept the Google login")
                            }
                    } finally {
                        sessionLoading = false
                        MainActivity.oauthCode.value = null
                    }
                }
                authDialog?.let { (title, message) ->
                    AlertDialog(
                        onDismissRequest = { authDialog = null },
                        confirmButton = {
                            TextButton(onClick = { authDialog = null }) { Text("OK") }
                        },
                        title = { Text(title) },
                        text = { Text(message) }
                    )
                }
                val checkingOnboarding =
                    googleSignedIn && (!authReady || onboardingDone == null)
                if (!splashDone || sessionLoading || checkingOnboarding) {
                    LoadingScreen()
                } else if (!googleSignedIn) {
                    // ---- Signed-out: auth flow (Google OAuth or email OTP) ----
                    when (route) {
                        AuthRoute.SignedOut -> DashboardSignedOutScreen(
                            onItemSelected = { route = AuthRoute.LogIn },
                            onNotificationClick = { route = AuthRoute.LogIn },
                            onContinueReading = { route = AuthRoute.LogIn },
                            onRecommendedClick = { route = AuthRoute.LogIn },
                            onSignInClick = { route = AuthRoute.LogIn }
                        )
                        AuthRoute.LogIn -> LoginScreen(
                                isLoading = otpBusy,
                                error = otpError,
                                onSendOtp = { email ->
                                    scope.launch {
                                        otpBusy = true
                                        otpError = null
                                        try {
                                            ServiceLocator.requestOtp(email)
                                                .onSuccess {
                                                    otpEmail = email.trim()
                                                    route = AuthRoute.SignUp
                                                }
                                                .onFailure {
                                                    otpError = it.message ?: "Couldn't send code — try again"
                                                }
                                        } finally {
                                            otpBusy = false
                                        }
                                    }
                                },
                                onVerifyOtp = { _, _ -> },
                                onGoogleLogIn = { handleGoogleClick() },
                                onCreateAccount = { route = AuthRoute.SignUp }
                            )
                            AuthRoute.SignUp -> CreateAccountScreen(
                                email = otpEmail,
                                isLoading = otpBusy,
                                error = otpError,
                                onSendOtp = {
                                    scope.launch {
                                        otpBusy = true
                                        otpError = null
                                        try {
                                            ServiceLocator.requestOtp(otpEmail)
                                                .onFailure {
                                                    otpError = it.message ?: "Couldn't send code — try again"
                                                }
                                        } finally {
                                            otpBusy = false
                                        }
                                    }
                                },
                                onVerifyOtp = { email, code ->
                                    scope.launch {
                                        otpBusy = true
                                        otpError = null
                                        try {
                                            ServiceLocator.verifyOtp(email.ifBlank { otpEmail }, code)
                                                .onSuccess { googleSignedIn = true }
                                                .onFailure {
                                                    otpError = it.message ?: "Invalid or expired code"
                                                }
                                        } finally {
                                            otpBusy = false
                                        }
                                    }
                                },
                                onGoogleSignUp = { handleGoogleClick() },
                                onLogIn = { route = AuthRoute.LogIn }
                            )
                        }
                    } else {
                        // ---- Signed-in (any Convex session: Google or email OTP) ----
                        if (onboardingDone == false) {
                            OnboardingProfileScreen(
                                isLoading = onboardLoading,
                                error = onboardError,
                                onFinish = { data ->
                                    scope.launch {
                                        onboardLoading = true
                                        onboardError = null
                                        try {
                                            ServiceLocator.submitOnboarding(
                                                firstName = data.firstName,
                                                lastName = data.lastName,
                                                phoneE164 = data.phoneE164,
                                                spiritualSeason = data.spiritualSeason,
                                                studyTimePreference = data.studyTimePreference,
                                                contactPreference = data.contactPreference,
                                                timezone = data.timezone,
                                                reminderHour = data.reminderHour,
                                                reminderMinute = data.reminderMinute
                                            ).onSuccess {
                                                onboardingDone = true
                                                ServiceLocator.saveOnboardingCompleted(true)
                                            }.onFailure {
                                                onboardError = it.message ?: "Couldn't save — try again"
                                            }
                                        } finally {
                                            onboardLoading = false
                                        }
                                    }
                                }
                            )
                        } else {
                        // ---- Signed-in: main app, fed by the session ----
                        // The Convex JWT is the identity. users:me fills in the name,
                        // email and phone as soon as it loads, so this empty user only
                        // covers the first frames after sign-in.
                        val fallbackUser = com.example.lampstand.data.LampUser(
                            firstName = "", lastName = "", email = "", phone = ""
                        )
                        fallbackUser.let { user ->
                            var selectedTab by remember { mutableStateOf(0) }
                            var showFandA by remember { mutableStateOf(false) }
                            // Web: the Journey tab always lands on /my-journey (the
                            // path list). The 5-section session is only entered from
                            // the dashboard's primary action or a journey card tap.
                            var showSession by remember { mutableStateOf(false) }
                            var showJourneyDashboard by remember { mutableStateOf(false) }
                            var journeyDashboardFromPaths by remember { mutableStateOf(false) }
                            var showProfileEdit by remember { mutableStateOf(false) }
                            // Session opened from the path detail (web's
                            // /my-journey/[id]/session?sessionId=...). Null = fall back to
                            // whatever the backend says is today's session.
                            var selectedSessionId by remember { mutableStateOf<String?>(null) }
                            // Web 5-section session pager: 0 Today's Journey · 1 Context
                            // · 2 Insight · 3 Application · 4 Guided Prayer.
                            var journeySection by remember { mutableStateOf(0) }
                            // Web post-completion chain (pathComplete → /feedback):
                            // feedback → completed → looking-forward → support.
                            // Null = the normal 5-section session pager.
                            var postCompleteFlow by remember { mutableStateOf<String?>(null) }
                            // viewModel() ties the VM to the Activity's ViewModelStore so its
                            // viewModelScope is cancelled cleanly — remember() leaked coroutines
                            // past composition (the "coroutine scope left the composition" crash).
                            val formationVm: com.example.lampstand.formation.FormationViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel()
                            val discoveryUi by formationVm.discovery.collectAsState()
                            val overviewUi by formationVm.overview.collectAsState()
                            val homeUi by formationVm.home.collectAsState()
                            val sessionUi by formationVm.session.collectAsState()
                            val me by formationVm.me.collectAsState()
                            var isCompleting by remember { mutableStateOf(false) }
                            var sessionError by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(googleSignedIn, onboardingDone) { formationVm.loadDiscovery() }
                            // Web parity: the dashboard's Convex useQuery is live and
                            // / remounts on every navigation, so Home always shows the
                            // latest state. Android fetches once — reload on entry.
                            LaunchedEffect(selectedTab, googleSignedIn, onboardingDone) {
                                if (selectedTab == 0) formationVm.loadHome()
                            }
                            // Web: the "My Journey" nav item is a link to /my-journey,
                            // so it always resolves to the path list — never back into
                            // a session the user has navigated away from.
                            val onNavTab: (Int) -> Unit = { tab ->
                                if (tab == 2) {
                                    showSession = false
                                    showJourneyDashboard = false
                                    journeyDashboardFromPaths = false
                                    postCompleteFlow = null
                                    journeySection = 0
                                    selectedSessionId = null
                                    sessionError = null
                                }
                                selectedTab = tab
                            }
                            val discoveryItems = when (val d = discoveryUi) {
                                is com.example.lampstand.formation.DiscoveryUi.Content -> d.items
                                else -> emptyList()
                            }
                            // Web parity: users:me firstName + paths:getHomeState branches.
                            val homeContent = (homeUi as? com.example.lampstand.formation.HomeUi.Content)
                            val home = homeContent?.home
                            val liveFirstName = homeContent?.firstName
                            val liveCount = homeContent?.recommendationCount ?: 6
                            // ---- Profile identity ----
                            // Profile must come from Convex users:me (same source as the
                            // Dashboard greeting). Anything derived from the local user
                            // object is only a last-resort fallback for the very first
                            // frame after sign-in.
                            val nameParts = me?.name?.trim()?.split(" ") ?: emptyList()
                            val profileFirstName = me?.firstName?.takeIf { it.isNotBlank() }
                                ?: nameParts.firstOrNull()?.takeIf { it.isNotBlank() }
                                ?: user.firstName
                            val profileLastName = me?.lastName?.takeIf { it.isNotBlank() }
                                ?: nameParts.drop(1).joinToString(" ").takeIf { it.isNotBlank() }
                                ?: user.lastName
                            val profileEmail = me?.email?.takeIf { it.isNotBlank() } ?: user.email
                            val profileInitials =
                                (profileFirstName.take(1) + profileLastName.take(1)).uppercase()
                                    .ifBlank { profileEmail.take(1).uppercase().ifBlank { "?" } }
                            val pill = when (home?.state) {
                                "active" -> "ACTIVE PATH . DAY ${home.currentDay ?: 1} OF ${home.totalDays ?: 7}"
                                "complete" -> "COMPLETED PATH"
                                else -> "NO ACTIVE PATH"
                            }
                            val liveTitle = home?.pathTitle ?: "Trust in Uncertainty"
                            // Web: the pill wording never changes — only the day
                            // number, which is 1 unless the path is active (the
                            // backend only sets currentDay for active paths).
                            val dayChip =
                                "Day ${home?.currentDay ?: 1}: Naming the Struggle . 7 min"
                            val primaryText = when (home?.state) {
                                "active" -> "Continue Today’s Session"
                                "complete" -> "Review Journey"
                                else -> "Explore Formation Paths"
                            }
                            // Web session page: session?sessionId= first (tapped row or
                            // Continue Reading), else home.sessionId, else the overview's
                            // available day.
                            val overviewForSession = (overviewUi as? com.example.lampstand.formation.OverviewUi.Content)?.overview
                            val effectiveSessionId = selectedSessionId
                                ?: home?.sessionId
                                ?: overviewForSession?.sessions?.firstOrNull { it.status == "available" }?.sessionId
                            LaunchedEffect(effectiveSessionId) {
                                if (effectiveSessionId != null) formationVm.loadSession(effectiveSessionId)
                            }
                            val liveSession = (sessionUi as? com.example.lampstand.formation.SessionUi.Content)?.session
                            if (showProfileEdit) {
                                val savedReminder = ServiceLocator.reminderSettings()
                                ProfileEditScreen(
                                    initialFirstName = profileFirstName,
                                    initialLastName = profileLastName,
                                    initialPhone = me?.phoneNumber ?: user.phone,
                                    initialReminderEnabled = savedReminder?.enabled == true,
                                    initialReminderSlot = savedReminder?.slot
                                        ?: ServiceLocator.slotForStudyTime(me?.studyTimePreference),
                                    initialReminderHour = savedReminder?.hour ?: -1,
                                    initialReminderMinute = savedReminder?.minute ?: -1,
                                    onBack = { showProfileEdit = false },
                                    onItemSelected = {
                                        showProfileEdit = false
                                        selectedTab = it
                                    },
                                    onSave = { first, last, phone, reminderEnabled, slot, times ->
                                        scope.launch {
                                            // Real identity lives in Convex: users:me feeds
                                            // the Dashboard + Profile, so write there and
                                            // refresh. updateProfile itself answers "Not
                                            // signed in" if the session has gone away.
                                            ServiceLocator.updateProfile(first, last, phone)
                                                .onSuccess {
                                                    if (reminderEnabled) {
                                                        val study = ServiceLocator.studyTimeForSlot(slot)
                                                        ServiceLocator.updateNotificationPreferences(
                                                            studyTimePreference = study,
                                                            contactPreference = me?.contactPreference ?: "email",
                                                            phone = phone,
                                                            timezone = me?.timezone
                                                                ?: java.util.TimeZone.getDefault().id
                                                        )
                                                        times[slot]?.let { (h, m) ->
                                                            ServiceLocator.saveReminderTime(study, h, m)
                                                        }
                                                    }
                                                    showProfileEdit = false
                                                    formationVm.loadMe()
                                                    formationVm.loadHome()
                                                }
                                                .onFailure { authError(it.message ?: "Save failed") }
                                        }
                                    }
                                )
                            } else if (showFandA) {
                                FandAPathScreen(
                                    onBack = { showFandA = false },
                                    onItemSelected = {
                                        showFandA = false
                                        onNavTab(it)
                                    },
                                    onCardClick = {
                                        showFandA = false
                                        selectedTab = 2
                                        journeyDashboardFromPaths = true
                                        showJourneyDashboard = true
                                        home?.pathId?.let(formationVm::loadOverview)
                                    },
                                    onPathClick = { pathId ->
                                        showFandA = false
                                        selectedTab = 2
                                        journeyDashboardFromPaths = true
                                        showJourneyDashboard = true
                                        formationVm.loadOverview(pathId)
                                    },
                                    onComingSoonClick = { pathId, onResult ->
                                        // Web ComingSoonPathcard.handleTap: the card
                                        // optimistically bumps the counter and shows its
                                        // own inline confirmation, so no toast here.
                                        formationVm.tapComingSoon(pathId) { r ->
                                            r.onSuccess { onResult(true) }
                                            r.onFailure {
                                                authError(it.message ?: "Couldn't save interest")
                                                onResult(false)
                                            }
                                        }
                                    },
                                    discoveryItems = discoveryItems
                                )
                            } else when (selectedTab) {
                                1 -> PathsScreen(
                                    // Web /paths back arrow is a link to "/".
                                    onBack = { selectedTab = 0 },
                                    onItemSelected = onNavTab,
                                    onViewPathsClick = { showFandA = true }
                                )
                                2 -> if (!showSession) {
                                    val listOverview = (overviewUi as? com.example.lampstand.formation.OverviewUi.Content)?.overview
                                    val dashboardDescription = listOverview?.description
                                        ?: discoveryItems.firstOrNull { it.pathId == home?.pathId }?.description
                                        ?: "Learn to rest in God\u2019s unchanging character even when the path ahead is unclear or unsettling."
                                    if (showJourneyDashboard) {
                                        val dashboardSessions = listOverview?.sessions?.map {
                                            JourneyDashboardSession(
                                                sessionId = it.sessionId,
                                                dayNumber = it.dayNumber,
                                                title = it.title,
                                                status = it.status
                                            )
                                        } ?: emptyList()
                                        val availableSession = listOverview?.sessions?.firstOrNull { it.status == "available" }
                                        val lastCompletedSession = listOverview?.sessions?.lastOrNull { it.status == "completed" }
                                        val continueSession = availableSession
                                            ?: lastCompletedSession
                                            ?: listOverview?.sessions?.firstOrNull()
                                        JourneyDashboardScreen(
                                            onBack = {
                                                showJourneyDashboard = false
                                                if (journeyDashboardFromPaths) {
                                                    journeyDashboardFromPaths = false
                                                    showFandA = true
                                                    selectedTab = 1
                                                }
                                            },
                                            onItemSelected = onNavTab,
                                            onContinueClick = {
                                                val sid = continueSession?.sessionId ?: home?.sessionId
                                                if (sid != null) {
                                                    selectedSessionId = sid
                                                    journeySection = 0
                                                    postCompleteFlow = null
                                                    showSession = true
                                                } else {
                                                    authError("No session available yet")
                                                }
                                            },
                                            onSessionClick = { sid ->
                                                selectedSessionId = sid
                                                journeySection = 0
                                                postCompleteFlow = null
                                                showSession = true
                                            },
                                            title = listOverview?.title
                                                ?: home?.pathTitle
                                                ?: "Trust in Uncertainty",
                                            description = dashboardDescription,
                                            totalDays = listOverview?.totalDays ?: home?.totalDays ?: 7,
                                            sessions = dashboardSessions,
                                            currentDay = availableSession?.dayNumber
                                                ?: lastCompletedSession?.dayNumber,
                                            isReviewing = availableSession == null && lastCompletedSession != null,
                                            selectedIndex = if (journeyDashboardFromPaths) 1 else 2
                                        )
                                    } else {
                                        // Web /my-journey: the Journey tab's entry screen —
                                        // one card per active path, or the empty state.
                                        JourneyListScreen(
                                            onItemSelected = onNavTab,
                                            onPathClick = {
                                                home?.pathId?.let(formationVm::loadOverview)
                                                journeyDashboardFromPaths = false
                                                showJourneyDashboard = true
                                            },
                                            onGoToPaths = { selectedTab = 1 },
                                            pathId = home?.pathId,
                                            pathTitle = home?.pathTitle ?: "",
                                            pathDescription = listOverview?.description
                                                ?: discoveryItems.firstOrNull { it.pathId == home?.pathId }?.description
                                                ?: "Grow in understanding God's character through these curated paths",
                                            durationDays = home?.totalDays ?: 7
                                        )
                                    }
                                } else {
                                    // Web /my-journey/[id]/session: 5-section pager plus
                                    // the post-completion feedback chain.
                                    val completeCurrentSession: () -> Unit = {
                                        val sid = effectiveSessionId
                                        if (sid == null) {
                                            sessionError = "No available session — start a path first"
                                        } else {
                                            isCompleting = true
                                            sessionError = null
                                            formationVm.completeSession(sid) { r ->
                                                isCompleting = false
                                                r.onSuccess { res ->
                                                    // Web GuidedPrayerSection: toast Day X
                                                    // Completed, and navigation only fires
                                                    // when the toast dismisses (2000ms).
                                                    Toast.makeText(
                                                        context,
                                                        "Day ${liveSession?.dayNumber ?: home?.currentDay ?: ""} Completed".trim(),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Back to the backend's idea of "today" so the next
                                                    // available session is picked up after a completion.
                                                    selectedSessionId = null
                                                    // Fresh session starts at section 0.
                                                    journeySection = 0
                                                    formationVm.loadHome()
                                                    if (home?.pathId != null) formationVm.loadOverview(home.pathId!!)
                                                    scope.launch {
                                                        delay(2000)
                                                        // Web: pathComplete → /my-journey/[id]/feedback,
                                                        // else → the Journey list.
                                                        if (res.pathComplete) {
                                                            postCompleteFlow = "feedback"
                                                        } else {
                                                            showSession = false
                                                        }
                                                    }
                                                }.onFailure {
                                                    sessionError = when {
                                                        (it.message ?: "").contains("SESSION_LOCKED") -> "This session is locked — come back tomorrow"
                                                        (it.message ?: "").contains("NO_ACTIVE_PATH") -> "Start a path first"
                                                        else -> it.message ?: "Couldn't complete session"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Bottom nav / leaving the flow resets session state —
                                    // web remounts the session page fresh each visit.
                                    val leaveJourneyFlow: (Int) -> Unit = { tab ->
                                        onNavTab(tab)
                                        showSession = false
                                        postCompleteFlow = null
                                        journeySection = 0
                                        selectedSessionId = null
                                        sessionError = null
                                    }
                                    // Web: top-left back on every session section returned
                                    // to /my-journey/[id]; with that page gone, it goes
                                    // back to the Journey list.
                                    val backToJourney: () -> Unit = {
                                        journeySection = 0
                                        postCompleteFlow = null
                                        sessionError = null
                                        showSession = false
                                    }
                                    when (postCompleteFlow) {
                                        // Web feedback → completed → looking-forward → support.
                                        "feedback" -> com.example.lampstand.journey.JourneyReviewScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            // Web router.back() → the session page that
                                            // pushed feedback (GuidedPrayerSection).
                                            onBack = {
                                                postCompleteFlow = null
                                                journeySection = 4
                                                showSession = true
                                            },
                                            // Web "Continue to Completed" → /completed.
                                            onBackToHome = { postCompleteFlow = "completed" }
                                        )
                                        "completed" -> com.example.lampstand.journey.JourneyCompletionReviewScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "feedback" },
                                            // Web "Finish" → /looking-forward (local-only, as on web).
                                            onSubmit = { _, _, _, _ -> postCompleteFlow = "lookingForward" }
                                        )
                                        "lookingForward" -> com.example.lampstand.journey.JourneyNextJourneyScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "completed" },
                                            // Web "Submit" → /support (local-only, as on web).
                                            onSubmit = { postCompleteFlow = "support" }
                                        )
                                        "support" -> com.example.lampstand.journey.JourneyDonationScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "lookingForward" },
                                            // Web Give = Flutterwave checkout via a Convex
                                            // action; no payment provider on Android yet,
                                            // so surface the chosen amount/tier instead.
                                            onGive = { req ->
                                                val detail = req.tierKey
                                                    ?: req.amountUsd?.let { "${it.toInt()} ${req.currency}" }
                                                    ?: req.currency
                                                Toast.makeText(context, "Payments aren't available yet — $detail", Toast.LENGTH_SHORT).show()
                                            },
                                            // Web "Maybe Later" → router.push("/") (home).
                                            onMaybeLater = {
                                                postCompleteFlow = null
                                                journeySection = 0
                                                selectedTab = 0
                                            }
                                        )
                                        else -> when (journeySection) {
                                        1 -> com.example.lampstand.journey.JourneyScreenTwo(
                                            onBack = { journeySection = 0 },
                                            onNext = { journeySection = 2 },
                                            onBackToPath = backToJourney,
                                            contextText = liveSession?.scriptureContext
                                        )
                                        2 -> com.example.lampstand.journey.JourneyScreenThree(
                                            onBack = { journeySection = 1 },
                                            onNext = { journeySection = 3 },
                                            onBackToPath = backToJourney,
                                            insightText = liveSession?.insightText
                                        )
                                        3 -> com.example.lampstand.journey.JourneyFormationScreen(
                                            onBack = { journeySection = 2 },
                                            onNext = { journeySection = 4 },
                                            onBackToPath = backToJourney,
                                            questions = liveSession?.reflectionQuestions
                                                ?: emptyList()
                                        )
                                        4 -> com.example.lampstand.journey.JourneyPrayerScreen(
                                            // Web S4 back → section 3 (Application).
                                            onBack = { journeySection = 3 },
                                            onBackToPath = backToJourney,
                                            onComplete = completeCurrentSession,
                                            isCompleting = isCompleting,
                                            sessionError = sessionError,
                                            prayerText = liveSession?.guidedPrayer
                                        )
                                        else -> JourneyScreen(
                                            // Web S0 back → the Journey list.
                                            onBack = backToJourney,
                                            onNext = { journeySection = 1 },
                                            sessionTitle = liveSession?.title ?: "Finding Stillness",
                                            scriptureVerse = liveSession?.scriptureVerse
                                                ?: "“Be still, and know that I am God; I will be exalted among the nations, I will be exalted in the earth.”",
                                            scriptureReference = liveSession?.scriptureReference ?: "Psalm 46:10 (NIV)",
                                            introText = liveSession?.applicationText
                                                ?: "Finding Stillness invites you to quiet your heart, slow your mind, and become aware of God’s steady presence in the midst of life’s noise.",
                                            // Web parity: section label is always TODAY'S JOURNEY.
                                            dayLabel = "TODAY’S JOURNEY",
                                            currentSection = journeySection,
                                            onDotClick = { journeySection = it }
                                        )
                                        }
                                    }
                                }
                                3 -> ProfileScreen(
                                    name = "$profileFirstName $profileLastName".trim()
                                        .ifBlank { profileEmail },
                                    email = profileEmail,
                                    initials = profileInitials,
                                    onItemSelected = onNavTab,
                                    // Root tab has nothing to pop — fall back to Home.
                                    onBack = { selectedTab = 0 },
                                    onPersonalInfoClick = { showProfileEdit = true },
                                    onLogOutClick = {
                                        scope.launch {
                                            sessionLoading = true
                                            try {
                                                // Web parity: auth:signOut invalidates the Convex
                                                // session first, then clears local tokens + caches.
                                                // Network failures are swallowed there, so a user is
                                                // never left signed in.
                                                ServiceLocator.signOut()
                                            } finally {
                                                formationVm.reset()
                                                googleSignedIn = false
                                                onboardingDone = null
                                                onboardError = null
                                                route = AuthRoute.SignedOut
                                                sessionLoading = false
                                            }
                                        }
                                    }
                                )
                                else -> DashboardScreen(
                                    onItemSelected = onNavTab,
                                    // Web: me?.firstName || "there" (Dashboard.tsx:23).
                                    userName = liveFirstName ?: "there",
                                    // Web: isAuthenticated ? "Welcome Back" : "Welcome".
                                    welcomeText =
                                        if (ServiceLocator.hasConvexSession()) "Welcome Back" else "Welcome",
                                    pillText = pill,
                                    pathTitle = liveTitle,
                                    dayChipText = dayChip,
                                    primaryButtonText = primaryText,
                                    recommendationCount = liveCount,
                                    // Web: the recommendation card pushes /paths.
                                    onRecommendedClick = { selectedTab = 1 },
                                    onContinueClick = {
                                        // Web handlePrimaryAction (Dashboard.tsx:36):
                                        // active + sessionId → session; otherwise the
                                        // Journey list; no path → /paths.
                                        val sid = home?.sessionId
                                        when {
                                            home?.state == "active" && sid != null -> {
                                                selectedSessionId = sid
                                                showSession = true
                                                showJourneyDashboard = true
                                                journeyDashboardFromPaths = false
                                                selectedTab = 2
                                            }
                                            home?.state == "active" || home?.state == "complete" -> {
                                                onNavTab(2)
                                            }
                                            else -> selectedTab = 1
                                        }
                                    }
                                )
                            }
                        }
                        } // end onboarding gate
                    }
                }
            }

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MainActivityPreview() {
    // skipSplash = true so interactive preview starts on the
    // signed-out dashboard immediately instead of sitting on Loading.
    // Set to false to preview the 3-second splash.
    LampStandApp(skipSplash = true)
}
