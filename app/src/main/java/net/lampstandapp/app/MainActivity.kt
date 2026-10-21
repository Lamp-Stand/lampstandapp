package net.lampstandapp.app

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import net.lampstandapp.app.auth.CreateAccountScreen
import net.lampstandapp.app.auth.DashboardSignedOutScreen
import net.lampstandapp.app.auth.LoadingScreen
import net.lampstandapp.app.auth.LoginScreen
import net.lampstandapp.app.auth.OnboardingProfileScreen
import net.lampstandapp.app.dashboard.DashboardScreen
import net.lampstandapp.app.data.ServiceLocator
import net.lampstandapp.app.journey.JourneyDashboardScreen
import net.lampstandapp.app.journey.JourneyDashboardSession
import net.lampstandapp.app.journey.JourneyListScreen
import net.lampstandapp.app.journey.JourneyScreen
import net.lampstandapp.app.journey.SessionUnavailableScreen
import net.lampstandapp.app.notification.ReminderScheduler
import net.lampstandapp.app.paths.FandAPathScreen
import net.lampstandapp.app.paths.PathsScreen
import net.lampstandapp.app.profile.PrivacyPolicyScreen
import net.lampstandapp.app.profile.ProfileEditScreen
import net.lampstandapp.app.profile.ProfileScreen
import net.lampstandapp.app.profile.TermsOfServiceScreen
import net.lampstandapp.app.ui.ScreenStatusBanner
import net.lampstandapp.app.ui.theme.LampStandTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthRoute { SignedOut, SignUp, LogIn }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4101)
            }
        }

        // Cold start via OAuth deep link (app was not running).
        intent?.data?.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
            oauthCode.value = it
        }
        intent?.data?.takeIf { it.host == "donation-return" }?.let {
            donationReturn.value = it
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
        intent.data?.takeIf { it.host == "donation-return" }?.let {
            donationReturn.value = it
        }
    }

    override fun onResume() {
        super.onResume()
        ServiceLocator.init(this)
        // Re-arm after returning from notification/exact-alarm settings.
        ReminderScheduler.sync(this)
    }

    internal fun requestReminderPermissions() {
        val notificationsGranted =
            android.os.Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                4101
            )
        } else {
            ReminderScheduler.requestExactAlarmAccess(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4101) {
            ServiceLocator.init(this)
            ReminderScheduler.sync(this)
            val granted = grantResults.firstOrNull() ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted && ServiceLocator.reminderSettings() != null) {
                ReminderScheduler.requestExactAlarmAccess(this)
            }
        }
    }

    companion object {
        /** ?code= captured from the OAuth deep link; consumed once by LampStandApp. */
        val oauthCode = androidx.compose.runtime.mutableStateOf<String?>(null)
        /** Flutterwave return deep link; consumed once by LampStandApp. */
        val donationReturn = androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null)
    }
}

private fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return current as? MainActivity
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
                val pendingDonationReturn by MainActivity.donationReturn
                LaunchedEffect(pendingDonationReturn) {
                    val uri = pendingDonationReturn ?: return@LaunchedEffect
                    val status = uri.getQueryParameter("status")
                    val transactionId = uri.getQueryParameter("transaction_id")
                    val paid = status == "successful" || status == "completed"
                    if (paid && !transactionId.isNullOrBlank()) {
                        ServiceLocator.verifyDonation(transactionId)
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    "Thank you - your gift was received.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    "Payment received. We'll confirm it shortly.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else if (status != null) {
                        Toast.makeText(context, "Checkout cancelled.", Toast.LENGTH_SHORT).show()
                    }
                    MainActivity.donationReturn.value = null
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
                                                ReminderScheduler.sync(context)
                                                context.findMainActivity()?.requestReminderPermissions()
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
                        val fallbackUser = net.lampstandapp.app.data.LampUser(
                            firstName = "", lastName = "", email = "", phone = ""
                        )
                        fallbackUser.let { user ->
                            var selectedTab by rememberSaveable { mutableStateOf(0) }
                            var showFandA by rememberSaveable { mutableStateOf(false) }
                            // Web: the Journey tab always lands on /my-journey (the
                            // path list). The 5-section session is only entered from
                            // the dashboard's primary action or a journey card tap.
                            var showSession by rememberSaveable { mutableStateOf(false) }
                            var showJourneyDashboard by rememberSaveable { mutableStateOf(false) }
                            var journeyDashboardFromPaths by rememberSaveable { mutableStateOf(false) }
                            var showProfileEdit by rememberSaveable { mutableStateOf(false) }
                            var showPrivacy by rememberSaveable { mutableStateOf(false) }
                            var showTerms by rememberSaveable { mutableStateOf(false) }
                            // Path the dashboard was opened for (used by refresh + retry).
                            var openedPathId by rememberSaveable { mutableStateOf<String?>(null) }
                            var autoStartedPath by rememberSaveable { mutableStateOf<String?>(null) }
                            // Session opened from the path detail (web's
                            // /my-journey/[id]/session?sessionId=...). Null = fall back to
                            // whatever the backend says is today's session.
                            var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                            // Web 5-section session pager: 0 Today's Journey -> 1 Context
                            // -> 2 Insight -> 3 Application -> 4 Guided Prayer.
                            var journeySection by rememberSaveable { mutableStateOf(0) }
                            // Web post-completion chain (pathComplete -> /feedback):
                            // feedback -> completed -> looking-forward -> support.
                            // Null = the normal 5-section session pager.
                            var postCompleteFlow by rememberSaveable { mutableStateOf<String?>(null) }
                            val sessionPager = rememberPagerState(
                                initialPage = journeySection.coerceIn(0, 4)
                            ) { 5 }
                            // viewModel() ties the VM to the Activity's ViewModelStore so its
                            // viewModelScope is cancelled cleanly — remember() leaked coroutines
                            // past composition (the "coroutine scope left the composition" crash).
                            val formationVm: net.lampstandapp.app.formation.FormationViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel()
                            val discoveryUi by formationVm.discovery.collectAsState()
                            val overviewUi by formationVm.overview.collectAsState()
                            val homeUi by formationVm.home.collectAsState()
                            val sessionUi by formationVm.session.collectAsState()
                            val me by formationVm.me.collectAsState()
                            var isCompleting by remember { mutableStateOf(false) }
                            var donationLoading by remember { mutableStateOf(false) }
                            var sessionError by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(googleSignedIn, onboardingDone) { formationVm.loadDiscovery() }
                            // Web parity: the dashboard's Convex useQuery is live and
                            // / remounts on every navigation, so Home always shows the
                            // latest state. Android fetches once — reload on entry.
                            LaunchedEffect(selectedTab, googleSignedIn, onboardingDone) {
                                if (selectedTab == 0) formationVm.loadHome()
                            }
                            // Swipe pager and the section dots/back/next buttons are
                            // two views of the same journeySection state.
                            LaunchedEffect(sessionPager) {
                                snapshotFlow { sessionPager.settledPage }.collect {
                                    if (journeySection != it) journeySection = it
                                }
                            }
                            LaunchedEffect(journeySection) {
                                if (sessionPager.currentPage != journeySection) {
                                    sessionPager.animateScrollToPage(journeySection)
                                }
                            }
                            // Web's Convex useQuery keeps Home / Paths / the open path
                            // fresh. Poll while the app is in the foreground.
                            val lifecycleOwner = LocalLifecycleOwner.current
                            LaunchedEffect(lifecycleOwner, googleSignedIn, onboardingDone) {
                                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                    while (true) {
                                        formationVm.loadHome()
                                        formationVm.loadDiscovery()
                                        openedPathId?.let { formationVm.loadOverview(it, silent = true) }
                                        delay(15_000)
                                    }
                                }
                            }
                            // Reminders only exist while a path is active (web parity).
                            LaunchedEffect(homeUi) { ReminderScheduler.sync(context) }
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
                                    openedPathId = null
                                }
                                selectedTab = tab
                            }
                            val discoveryItems = when (val d = discoveryUi) {
                                is net.lampstandapp.app.formation.DiscoveryUi.Content -> d.items
                                else -> emptyList()
                            }
                            // Web parity: users:me firstName + paths:getHomeState branches.
                            val homeContent = (homeUi as? net.lampstandapp.app.formation.HomeUi.Content)
                            val home = homeContent?.home
                            // The Home CTA must only open a session Convex has
                            // actually marked available. Load the overview for the
                            // active path so the CTA can check that lock status.
                            LaunchedEffect(home?.pathId, home?.state) {
                                if (home?.state == "active" || home?.state == "complete") {
                                    home?.pathId?.let { formationVm.loadOverview(it, silent = true) }
                                }
                            }
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
                                "active" -> "ACTIVE PATH . DAY ${home.currentDay?.toInt() ?: 1} OF ${home.totalDays?.toInt() ?: 7}"
                                "complete" -> "COMPLETED PATH"
                                else -> "NO ACTIVE PATH"
                            }
                            val liveTitle = home?.pathTitle ?: "Trust in Uncertainty"
                            // Web: the pill wording never changes — only the day
                            // number, which is 1 unless the path is active (the
                            // backend only sets currentDay for active paths).
                            val dayChip =
                                "Day ${home?.currentDay?.toInt() ?: 1}: Naming the Struggle . 7 min"
                            val primaryText = when (home?.state) {
                                "active" -> "Continue Today’s Session"
                                "complete" -> "Review Journey"
                                else -> "Explore Formation Paths"
                            }
                            // Web session page: session?sessionId= first (tapped row or
                            // Continue Reading), else home.sessionId, else the overview's
                            // available day.
                            val overviewForSession = (overviewUi as? net.lampstandapp.app.formation.OverviewUi.Content)?.overview
                            val effectiveSessionId = selectedSessionId
                                ?: home?.sessionId
                                ?: overviewForSession?.sessions?.firstOrNull { it.status == "available" }?.sessionId
                            LaunchedEffect(effectiveSessionId) {
                                if (effectiveSessionId != null) formationVm.loadSession(effectiveSessionId)
                            }
                            val liveSession = (sessionUi as? net.lampstandapp.app.formation.SessionUi.Content)?.session
                            val sessionLoadError =
                                (sessionUi as? net.lampstandapp.app.formation.SessionUi.Error)?.message
                            if (showPrivacy) {
                                PrivacyPolicyScreen(onBack = { showPrivacy = false })
                            } else if (showTerms) {
                                TermsOfServiceScreen(onBack = { showTerms = false })
                            } else if (showProfileEdit) {
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
                                                    } else {
                                                        ServiceLocator.clearReminderTime()
                                                    }
                                                    ReminderScheduler.sync(context)
                                                    if (reminderEnabled) {
                                                        context.findMainActivity()?.requestReminderPermissions()
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
                                        home?.pathId?.let {
                                            openedPathId = it
                                            formationVm.loadOverview(it)
                                        }
                                    },
                                    onPathClick = { pathId ->
                                        showFandA = false
                                        selectedTab = 2
                                        journeyDashboardFromPaths = true
                                        showJourneyDashboard = true
                                        openedPathId = pathId
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
                                    val listOverview = (overviewUi as? net.lampstandapp.app.formation.OverviewUi.Content)?.overview
                                    // Web PathDetail auto-start: opening a path with no
                                    // userPathId calls paths:startPath once, then reloads.
                                    LaunchedEffect(listOverview?.pathId, listOverview?.userPathId) {
                                        val overview = listOverview ?: return@LaunchedEffect
                                        if (overview.userPathId == null && autoStartedPath != overview.pathId) {
                                            autoStartedPath = overview.pathId
                                            formationVm.startPath(overview.pathId)
                                        }
                                    }
                                    val dashboardDescription = listOverview?.description
                                        ?: discoveryItems.firstOrNull { it.pathId == home?.pathId }?.description
                                        ?: "Learn to rest in God\u2019s unchanging character even when the path ahead is unclear or unsettling."
                                    if (showJourneyDashboard) {
                                        val dashboardSessions = listOverview?.sessions?.map {
                                            JourneyDashboardSession(
                                                sessionId = it.sessionId,
                                                dayNumber = it.dayNumber.toInt(),
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
                                            totalDays = (listOverview?.totalDays ?: home?.totalDays)?.toInt() ?: 7,
                                            sessions = dashboardSessions,
                                            currentDay = (availableSession?.dayNumber
                                                ?: lastCompletedSession?.dayNumber)?.toInt(),
                                            isReviewing = availableSession == null && lastCompletedSession != null,
                                            selectedIndex = if (journeyDashboardFromPaths) 1 else 2,
                                            isLoading = overviewUi is net.lampstandapp.app.formation.OverviewUi.Loading,
                                            loadError = (overviewUi as? net.lampstandapp.app.formation.OverviewUi.Error)?.message,
                                            onRetry = {
                                                (openedPathId ?: home?.pathId)?.let { formationVm.loadOverview(it) }
                                            }
                                        )
                                    } else {
                                        // Web /my-journey: the Journey tab's entry screen —
                                        // one card per active path, or the empty state.
                                        JourneyListScreen(
                                            onItemSelected = onNavTab,
                                            onPathClick = {
                                                home?.pathId?.let {
                                                    openedPathId = it
                                                    formationVm.loadOverview(it)
                                                }
                                                journeyDashboardFromPaths = false
                                                showJourneyDashboard = true
                                            },
                                            onGoToPaths = { selectedTab = 1 },
                                            pathId = home?.pathId,
                                            pathTitle = home?.pathTitle ?: "",
                                            pathDescription = listOverview?.description
                                                ?: discoveryItems.firstOrNull { it.pathId == home?.pathId }?.description
                                                ?: "Grow in understanding God's character through these curated paths",
                                            durationDays = home?.totalDays?.toInt() ?: 7,
                                            isLoading = overviewUi is net.lampstandapp.app.formation.OverviewUi.Loading,
                                            loadError = (overviewUi as? net.lampstandapp.app.formation.OverviewUi.Error)?.message,
                                            onRetry = {
                                                (openedPathId ?: home?.pathId)?.let { formationVm.loadOverview(it) }
                                            }
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
                                                        "Day ${(liveSession?.dayNumber ?: home?.currentDay)?.toInt() ?: ""} Completed".trim(),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    ServiceLocator.recordSessionCompletedNow()
                                                    ReminderScheduler.sync(context)
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
                                        "feedback" -> net.lampstandapp.app.journey.JourneyReviewScreen(
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
                                        "completed" -> net.lampstandapp.app.journey.JourneyCompletionReviewScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "feedback" },
                                            // Web "Finish" → /looking-forward (local-only, as on web).
                                            onSubmit = { _, _, _, _ -> postCompleteFlow = "lookingForward" }
                                        )
                                        "lookingForward" -> net.lampstandapp.app.journey.JourneyNextJourneyScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "completed" },
                                            // Web "Submit" → /support (local-only, as on web).
                                            onSubmit = { postCompleteFlow = "support" }
                                        )
                                        "support" -> net.lampstandapp.app.journey.JourneyDonationScreen(
                                            onItemSelected = leaveJourneyFlow,
                                            onBack = { postCompleteFlow = "lookingForward" },
                                            isProcessing = donationLoading,
                                            // Web Give = Flutterwave checkout via the same
                                            // Convex action; open the hosted page in a tab.
                                            onGive = { req ->
                                                scope.launch {
                                                    donationLoading = true
                                                    try {
                                                        ServiceLocator.initializeDonation(
                                                            kind = req.kind,
                                                            currency = req.currency,
                                                            amountMinor = req.amountMinor,
                                                            tierKey = req.tierKey
                                                        ).onSuccess { checkout ->
                                                            try {
                                                                val tabs =
                                                                    androidx.browser.customtabs.CustomTabsIntent.Builder()
                                                                        .setShowTitle(true)
                                                                        .build()
                                                                val activity = context as? android.app.Activity
                                                                tabs.launchUrl(
                                                                    activity ?: context,
                                                                    android.net.Uri.parse(checkout.checkoutUrl)
                                                                )
                                                            } catch (e: Exception) {
                                                                Toast.makeText(
                                                                    context,
                                                                    e.message ?: "Couldn't open checkout",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }.onFailure {
                                                            Toast.makeText(
                                                                context,
                                                                it.message ?: "Couldn't start checkout",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    } finally {
                                                        donationLoading = false
                                                    }
                                                }
                                            },
                                            // Web "Maybe Later" → router.push("/") (home).
                                            onMaybeLater = {
                                                postCompleteFlow = null
                                                journeySection = 0
                                                selectedTab = 0
                                            }
                                        )
                                        else -> Column(modifier = Modifier.fillMaxSize()) {
                                            ScreenStatusBanner(
                                                message = sessionError,
                                                loading = sessionUi is net.lampstandapp.app.formation.SessionUi.Loading
                                            )
                                            when {
                                                sessionLoadError != null -> SessionUnavailableScreen(
                                                    message = sessionLoadError,
                                                    onBack = backToJourney,
                                                    onRetry = effectiveSessionId?.let { id ->
                                                        { formationVm.loadSession(id) }
                                                    }
                                                )
                                                sessionUi is net.lampstandapp.app.formation.SessionUi.Loading -> Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    androidx.compose.material3.CircularProgressIndicator(
                                                        color = androidx.compose.ui.graphics.Color(0xFF335E78)
                                                    )
                                                }
                                                else -> HorizontalPager(
                                                    state = sessionPager,
                                                    modifier = Modifier.weight(1f)
                                                ) { page ->
                                                    when (page) {
                                                        1 -> net.lampstandapp.app.journey.JourneyScreenTwo(
                                                            onBack = { journeySection = 0 },
                                                            onNext = { journeySection = 2 },
                                                            onBackToPath = backToJourney,
                                                            contextText = liveSession?.scriptureContext
                                                        )
                                                        2 -> net.lampstandapp.app.journey.JourneyScreenThree(
                                                            onBack = { journeySection = 1 },
                                                            onNext = { journeySection = 3 },
                                                            onBackToPath = backToJourney,
                                                            insightText = liveSession?.insightText
                                                        )
                                                        3 -> net.lampstandapp.app.journey.JourneyFormationScreen(
                                                            onBack = { journeySection = 2 },
                                                            onNext = { journeySection = 4 },
                                                            onBackToPath = backToJourney,
                                                            questions = liveSession?.reflectionQuestions
                                                                ?: emptyList()
                                                        )
                                                        4 -> net.lampstandapp.app.journey.JourneyPrayerScreen(
                                                            // Web S4 back -> section 3 (Application).
                                                            onBack = { journeySection = 3 },
                                                            onBackToPath = backToJourney,
                                                            onComplete = completeCurrentSession,
                                                            isCompleting = isCompleting,
                                                            sessionError = sessionError,
                                                            prayerText = liveSession?.guidedPrayer
                                                        )
                                                        else -> JourneyScreen(
                                                            // Web S0 back -> the Journey list.
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
                                                            currentSection = page,
                                                            onDotClick = { journeySection = it }
                                                        )
                                                    }
                                                }
                                            }
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
                                    onPrivacyClick = { showPrivacy = true },
                                    onTermsClick = { showTerms = true },
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
                                    isLoading = homeUi is net.lampstandapp.app.formation.HomeUi.Loading,
                                    loadError = (homeUi as? net.lampstandapp.app.formation.HomeUi.Error)?.message,
                                    onRetry = { formationVm.loadHome() },
                                    // Web: the recommendation card pushes /paths.
                                    onRecommendedClick = { selectedTab = 1 },
                                    onContinueClick = {
                                        // Convex locks future days until their
                                        // calendar day. home.sessionId can point at
                                        // tomorrow's locked session immediately
                                        // after completing today, so only open a
                                        // session the overview marks available.
                                        val homeOverview = overviewForSession
                                            ?.takeIf { it.pathId == home?.pathId }
                                        val sid = homeOverview?.sessions
                                            ?.firstOrNull { it.status == "available" }
                                            ?.sessionId
                                        when {
                                            home?.state == "active" && sid != null -> {
                                                selectedSessionId = sid
                                                showSession = true
                                                showJourneyDashboard = true
                                                journeyDashboardFromPaths = false
                                                selectedTab = 2
                                            }
                                            home?.state == "active" || home?.state == "complete" -> {
                                                // No session is unlocked yet — show
                                                // the path's schedule/lock state
                                                // instead of opening a locked day.
                                                onNavTab(2)
                                                home?.pathId?.let { pathId ->
                                                    openedPathId = pathId
                                                    formationVm.loadOverview(pathId)
                                                    showJourneyDashboard = true
                                                    journeyDashboardFromPaths = false
                                                }
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
