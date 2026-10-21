# Android vs Web Flow Parity Audit

Audit date: 2026-10-20

Repositories compared:

- Android: `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand`
- Web/backend: `C:\Users\XYNYDOMEGA\projectLampStand`

No application files were modified during this audit. This document is the comparison and pre-push checklist.

---

## 1. Verdict

The Android app follows the same broad journey as the web app:

```text
Auth -> Onboarding -> Home -> Paths -> Category -> Journey dashboard
     -> 5-section session -> Completion -> Feedback/Debrief -> Support -> Home
```

However, it is **not full parity** and should not be pushed as-is if starting a path and normal navigation are expected to work.

Blocking issues:

1. Android never calls `paths:startPath`, so a new user cannot start a path and completing a session fails with `NO_ACTIVE_PATH`.
2. Android has no `BackHandler` / navigation stack; hardware or gesture back exits the activity from every screen.
3. The Journey dashboard opened from Paths highlights the Paths tab, while web highlights My Journey.
4. Home routes to the Journey list for a completed path or an already-completed day, while web goes to the path dashboard.
5. Donations are UI-only on Android; web performs real Flutterwave checkout.

---

## 2. Route-to-screen mapping

### Web routes

```text
/
/dashboard                         (redirect shim to /)
/auth
/onboarding
/paths
/paths/[id]
/paths/[id]/support                (orphaned alternate support page)
/my-journey
/my-journey/[id]
/my-journey/[id]/session
/my-journey/[id]/feedback
/my-journey/[id]/completed
/my-journey/[id]/looking-forward
/my-journey/[id]/support
/profile
/profile/personal-information
/profile/manage-password
/admin
/admin/users
/privacy
/terms
```

### Mapping table

| Web route | Purpose | Android screen/state | Parity |
|---|---|---|---|
| `/` | Signed-in/out dashboard | `DashboardScreen` / `DashboardSignedOutScreen` | Mostly |
| `/auth` | Google auth; OTP handlers unreachable | `DashboardSignedOutScreen` -> `LoginScreen` | Both effectively Google-only |
| `/onboarding` | Two-step profile/preferences | `OnboardingProfileScreen` | Partial |
| `/paths` | Category list | `PathsScreen` | Matches |
| `/paths/1` | Paths in Fear & Anxiety | `FandAPathScreen` | Matches |
| `/my-journey` | Journey list | `JourneyListScreen` | Matches in practice |
| `/my-journey/1` | Path detail/dashboard | `JourneyDashboardScreen` | **Missing auto-start** |
| `/my-journey/1/session` | Five-section pager | `JourneyScreen` .. `JourneyScreenFive` | Content/order matches |
| `/my-journey/1/feedback` | Session pulse | `JourneyReviewScreen` | Matches |
| `/my-journey/1/completed` | Path debrief | `JourneyCompletionReviewScreen` | Matches |
| `/my-journey/1/looking-forward` | Future themes | `JourneyNextJourneyScreen` | Matches |
| `/my-journey/1/support` | Post-debrief donation | `JourneyDonationScreen` | **No checkout** |
| `/profile` | Profile summary | `ProfileScreen` | Matches |
| `/profile/personal-information` | Edit name/phone | `ProfileEditScreen` | Matches |
| `/profile/manage-password` | Fake form, unlinked | `ProfilePasswordScreen`, unlinked | Parity (dead on both) |
| Notifications | No route | No screen | Parity (no-op on both) |
| `/admin*` | Web-only admin | None | Intentional |
| `/privacy`, `/terms` | Public static pages | None | Missing on Android |

---

## 3. End-to-end flow comparison

### Web flow

```text
Google sign-in
  -> /onboarding
  -> /
  -> /paths
  -> /paths/1
  -> /my-journey/{pathId}       (auto-start if no userPath)
  -> /my-journey/{pathId}/session?sessionId=...
  -> completeSession
       -> day 7: /feedback -> /completed -> /looking-forward -> /support -> /
       -> otherwise: /my-journey/{pathId}
```

### Android flow

```text
Google sign-in
  -> OnboardingProfileScreen
  -> Home
  -> Paths -> Fear & Anxiety
  -> JourneyDashboardScreen       (NO startPath)
  -> 5 session screens
  -> completeSession
       -> day 7: feedback -> completed -> looking-forward -> support -> Home
       -> otherwise: Journey dashboard
```

The screen order matches. The missing path start is the main functional break.

---

## 4. Priority findings

### P0 - blockers

#### P0.1 Android never starts a path

- `paths:startPath` is implemented in the repository and ViewModel:
  - `data/ConvexFormationRepository.kt:150`
  - `data/FormationRepository.kt:23-24`
  - `formation/FormationViewModel.kt:141-148`
- There is no call site anywhere in the Android UI.
- Web auto-starts on path-detail mount:
  - `app/my-journey/[id]/page.tsx:78-87`
- Consequence:
  - A new mobile user opens a live path but no `userPaths` row is created.
  - The dashboard shows Day 1 as available only because the signed-out/not-started shape allows it.
  - Completing a session returns `NO_ACTIVE_PATH`.
- Required fix:
  - When `PathOverviewDto.userPathId == null`, call `startPath(pathId)`.
  - Reload overview and Home on success.
  - Ignore expected `PATH_ALREADY_ACTIVE` failures, matching web.

#### P0.2 Android hardware back exits the app

- No `BackHandler`, `NavHost`, or fragment back stack exists in the Android module.
- Every in-app back control is composition-local.
- Consequence:
  - System back from dashboards, session sections, feedback, completed, looking-forward, support, or profile closes the Activity.
- Required fix:
  - Add `BackHandler` per state or introduce Navigation Compose.
  - At minimum handle: onboarding, session, feedback, completed, looking-forward, support, dashboard, profile edit, F&A.

### P1 - behavior mismatches

#### P1.1 Wrong bottom tab on dashboard opened from Paths

- Android: `MainActivity.kt:569` sets `selectedIndex = 1` when `journeyDashboardFromPaths`.
- Web: `/my-journey/[id]` is inside `app/my-journey/layout.tsx` and highlights My Journey.
- Back behavior can still return to Paths, but the selected tab should be My Journey.

#### P1.2 Home CTA differs for `todayDone` and completed paths

- Web:
  - Active + `sessionId` -> session
  - Active + `todayDone` (no `sessionId`) -> path dashboard
  - Complete -> path dashboard ("Review Journey")
- Android:
  - Active + `sessionId` -> session
  - Active without `sessionId` or complete -> `onNavTab(2)`, landing on the Journey list
  - `MainActivity.kt:805-807`
- `HomeStateDto.todayDone` exists but is never read.

#### P1.3 Donations are not functional on Android

- Web starts Flutterwave checkout actions and redirects to `checkoutUrl`.
- Android builds a local donation shape and only shows a Toast:
  - `MainActivity.kt:689-694`
- Web "Maybe Later" records `donations:recordDonationSkip`; Android does not.
- If donation is part of this release, this is a functional gap. Otherwise document it as out of scope.

#### P1.4 Navigation state is lost on config change/process death

- Navigation is plain `remember`, not `rememberSaveable`:
  - `MainActivity.kt:321-340`
- Rotation replays splash and returns to Home.
- Web preserves the URL.

#### P1.5 Token refresh only runs at startup

- Android now refreshes an expiring JWT at startup (`ServiceLocator.kt:139`).
- There is no 401 interceptor or mid-session refresh.
- If the app stays open past JWT expiry, reads fall back to caches and writes fail while the UI still appears signed in.

### P2 - polish and smaller parity gaps

1. **No swipe pager on Android.** Web uses a horizontally scrollable snap pager (`session/page.tsx:110-157`). Android uses buttons/dots, and only section 0's dots are interactive.
2. **Onboarding differs.** Web has profile and preferences steps; Android is one page. Android hardcodes `contactPreference = "email"` and has no WhatsApp/Both phone flow.
3. **Onboarding forcing differs.** Android always forces incomplete users into onboarding. Web only checks when visiting `/onboarding`; incomplete users can still visit `/` and `/paths`.
4. **Reading schedule source differs.** Android computes dates from the device calendar and `currentDay`; web aligns dates using mount time/started data.
5. **Journey-list card can use another path's overview.** The card opens `home.pathId` but its description comes from the last loaded overview, which may have been loaded from F&A.
6. **Profile edit bypasses tab reset.** It sets `selectedTab` directly instead of `onNavTab`, so stale session/dashboard flags can reopen an old Journey state.
7. **Session content is fetched while not on the session screen.** `LaunchedEffect(effectiveSessionId)` runs regardless of `showSession`.
8. **Completed rows can be reopened and completed again.** Backend idempotency protects data, but the UI does not prevent it.
9. **Home/overview/session errors fall back silently.** Hardcoded copy can hide backend failures.
10. **No loading/error UI on several Android content screens.** Web has spinners and explicit not-found states.
11. **No privacy/terms screens on Android.**
12. **No notification scheduler despite reminder UI.** Manifest has no notification permissions or scheduling code.

---

## 5. What already matches

### Session structure

| Index | Web | Android | Source field |
|---|---|---|---|
| 0 | Today's Journey | `JourneyScreen` | `title`, `applicationText`, `scriptureVerse`, `scriptureReference` |
| 1 | Historical & Biblical Background | `JourneyScreenTwo` | `scriptureContext` |
| 2 | What this Reveals About God | `JourneyScreenThree` | `insightText` |
| 3 | Formation Questions | `JourneyFormationScreen` | `reflectionQuestions` |
| 4 | Talk to God | `JourneyPrayerScreen` | `guidedPrayer` |

Web mapping reference: `app/my-journey/[id]/session/page.tsx:70-108`.

### Completion

- Both call `paths:completeSession({ sessionId })`.
- Both show a "Day X Completed" toast.
- Both wait about 2 seconds.
- Both route to feedback when `pathComplete == true`, otherwise to the path dashboard.
- Both rely on backend lock/time rules.
- Both retry-safely because `completeSession` is idempotent server-side.

### Post-completion

- Both show feedback, completed, looking-forward, then support.
- Both allow skipping all questions; forward buttons are always enabled.
- Neither persists feedback, debrief answers, or selected future themes.
- Backend mutations `feedback:submitSessionPulse` and `feedback:submitPathDebrief` exist but are unused by both clients.

### Navigation visibility

- Session and feedback hide the bottom nav.
- Completed, looking-forward, and support show it.
- Session top back returns to the path dashboard.
- Dashboard back returns to the previous context: Paths if entered from F&A, otherwise My Journey.

### Auth

- Both use Convex Auth with Google and Resend OTP.
- Both save access and refresh tokens.
- Android now refreshes an expired access token at startup.
- Both clear the local session on sign-out.
- Email OTP is effectively unreachable on both:
  - Web: no email input is rendered.
  - Android: `CreateAccountScreen` exists but Login has no entry point.

### Data

Both clients use the same Convex backend and function names:

- `users:me`
- `users:updateProfile`
- `users:completeOnboarding`
- `users:updateNotificationPreferences`
- `paths:getHomeState`
- `paths:getPathOverview`
- `paths:getSessionContent`
- `paths:startPath`
- `paths:completeSession`
- `discovery:getPathsDiscoveryScreen`
- `discovery:recordTap`
- `auth:signIn`, `auth:signOut`

---

## 6. Data-layer differences

| Area | Web | Android |
|---|---|---|
| Query updates | Live Convex subscriptions | One-shot HTTP queries with refresh on entry/action |
| Discovery cache | None | SharedPreferences fallback |
| Home cache | None | SharedPreferences fallback |
| Overview cache | None | Per-path SharedPreferences fallback |
| Session cache | None | None |
| Profile identity | `users:me` | `users:me` with cache |
| Progress | Backend `userPaths` | Backend `userPaths` |
| Token refresh | Convex Auth client | Startup refresh only |
| Offline | Live query/error states | Cached discovery/home/overview/me |

Android repositories:

- `data/ConvexFormationRepository.kt`
- Cache keys: `cache_discovery_v1`, `cache_home_v1`, `cache_me_v1`, `cache_overview_v1_<pathId>`

---

## 7. Auth and onboarding findings

### Android

- Tokens are stored in `SharedPreferences("lampstand_formation_cache")`.
- Access token expiry is decoded from the JWT `exp` claim with a 5-minute leeway.
- Startup refresh calls `auth:signIn { refreshToken }` and rotates the token pair.
- Invalid refresh tokens clear the session and return to signed-out.
- Offline refresh failures keep the existing session.
- Onboarding completion is cached under `onboarding_done_v1`.
- Onboarding is gated by a loading screen while auth/onboarding is unresolved.
- Sign-out clears tokens, OAuth verifier, onboarding cache, reminder keys, and all formation caches.

### Web

- Middleware protects `/onboarding`, `/paths`, `/my-journey`, `/profile`, and `/admin`.
- `/` is public and renders a signed-out dashboard.
- Authenticated users visiting `/auth` are redirected to `/`.
- Protected signed-out visitors redirect to `/auth` without preserving the intended URL.
- Onboarding completion redirects to `/`, but incomplete onboarding is not globally enforced.
- Session duration is 365 days.

### Divergences

1. Android forces onboarding; web does not.
2. Android has a custom signed-out dashboard; web uses the public root dashboard.
3. Android's onboarding is single-page; web is two-step.
4. Android's contact preference is fixed to email; web supports email/WhatsApp/both.

---

## 8. Edge cases observed

### Android

- Locked dashboard rows are disabled, but the Continue fallback can select the first session even if it is locked.
- `todayDone` is unused.
- `PathOverviewDto.startedAt` and `userPathId` are unused.
- Offline first launch with a signed-in but missing onboarding cache can show onboarding.
- Mid-session JWT expiry has no refresh/retry.
- Rotation/process death resets navigation.
- A completed path Home CTA goes to the Journey list rather than the dashboard.
- Profile edit can leave stale Journey state when switching tabs.

### Web

- Missing `sessionId` leaves the session page on a spinner forever.
- Locked session URLs surface a raw query error.
- `completeSession` does not re-check the 24-hour time lock; only day order.
- Only one active path is allowed.
- Post-session support passes a route segment such as `"1"` to `recordDonationSkip`, which expects a Convex path ID; "Maybe Later" may throw.
- The completed-path Home state omits `currentDay`/`totalDays`.
- Feedback option IDs use hyphens while the unused backend enums use underscores.
- `/my-journey` uses static mock data, so its empty state is unreachable.
- Path detail and post-session links are hardcoded to `/my-journey/1/...`.
- Custom monthly donation amounts silently fall back to the preset tier.

---

## 9. Pre-push checklist

### Must fix

- [ ] Call `paths:startPath` when opening a live path without `userPathId`.
- [ ] Add hardware/gesture back handling for every in-app screen.
- [ ] Highlight My Journey on the dashboard even when entered from Paths.
- [ ] Route active-without-session and complete Home states to the journey dashboard.

### Should fix

- [ ] Add mid-session token refresh or 401 retry.
- [ ] Preserve navigation state across rotation with `rememberSaveable`.
- [ ] Decide whether donation checkout is in scope; if not, label it clearly.
- [ ] Reset Journey flags when leaving Profile edit through the bottom nav.
- [ ] Avoid loading session content until `showSession` is true.
- [ ] Handle locked/continue fallback consistently with web.

### Nice to have

- [ ] Swipeable session pager.
- [ ] Two-step onboarding parity and contact preference choice.
- [ ] Privacy/terms screens.
- [ ] Real notification scheduling or removal of the reminder promise.
- [ ] Persist feedback/debrief only if the web starts doing so.
- [ ] Replace static journey list/path IDs with backend data on Android and web.

---

## 10. Key file references

### Android

- `app/src/main/java/com/example/lampstand/MainActivity.kt`
- `app/src/main/java/com/example/lampstand/auth/LoginScreen.kt`
- `app/src/main/java/com/example/lampstand/auth/CreateAccountScreen.kt`
- `app/src/main/java/com/example/lampstand/auth/OnboardingProfileScreen.kt`
- `app/src/main/java/com/example/lampstand/auth/LoadingScreen.kt`
- `app/src/main/java/com/example/lampstand/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/example/lampstand/dashboard/BottomNavBar.kt`
- `app/src/main/java/com/example/lampstand/paths/PathsScreen.kt`
- `app/src/main/java/com/example/lampstand/paths/FandAPathScreen.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyListScreen.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyDashboard.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyScreen.kt` through `JourneyScreenFive.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyReviewOne.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyCompletionReview.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyNextJourneyScreen.kt`
- `app/src/main/java/com/example/lampstand/journey/JourneyDonationScreen.kt`
- `app/src/main/java/com/example/lampstand/data/FormationRepository.kt`
- `app/src/main/java/com/example/lampstand/data/ConvexFormationRepository.kt`
- `app/src/main/java/com/example/lampstand/data/ServiceLocator.kt`
- `app/src/main/java/com/example/lampstand/data/remote/ConvexTokenStore.kt`
- `app/src/main/java/com/example/lampstand/data/remote/GoogleOAuth.kt`
- `app/src/main/java/com/example/lampstand/data/remote/MobileDtos.kt`
- `app/src/main/java/com/example/lampstand/formation/FormationViewModel.kt`

### Web/backend

- `app/page.tsx`
- `app/auth/page.tsx`
- `app/onboarding/page.tsx`
- `app/paths/page.tsx`
- `app/paths/[id]/page.tsx`
- `app/my-journey/layout.tsx`
- `app/my-journey/page.tsx`
- `app/my-journey/[id]/page.tsx`
- `app/my-journey/[id]/session/page.tsx`
- `app/my-journey/[id]/feedback/page.tsx`
- `app/my-journey/[id]/completed/page.tsx`
- `app/my-journey/[id]/looking-forward/page.tsx`
- `app/my-journey/[id]/support/page.tsx`
- `app/profile/page.tsx`
- `app/profile/personal-information/page.tsx`
- `components/Dashboard.tsx`
- `components/shared/BottomNav.tsx`
- `middleware.ts`
- `convex/auth.ts`
- `convex/users.ts`
- `convex/paths.ts`
- `convex/discovery.ts`
- `convex/feedback.ts`
- `convex/donations.ts`
- `convex/schema.ts`
- `convex/seed.ts`
