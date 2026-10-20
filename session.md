# Session: Journey Screens — Android vs Web Content Audit

Date: 2026-06-14
Status: **Research complete, comparison pending final deliverable**

---

## 1. Goal

User asked to verify that the 11 screens in the Android `journey` package match the contents of the Next.js web app (screen-by-screen content parity).

---

## 2. Codebases Involved

| Role | Path |
|---|---|
| Android app (Jetpack Compose) | `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand` |
| Android journey package | `LampStand\app\src\main\java\com\example\lampstand\journey\` (11 `.kt` files) |
| Web app (Next.js, separate repo) | `C:\Users\XYNYDOMEGA\projectLampStand` |

> The Android repo has **no web/wasm target**; "Web …" comments in the Kotlin files are hand-port references to the separate Next.js app.

---

## 3. Screen Count & Mapping (11 = 11 ✅ count matches)

| # | Android file (composable) | Web route / file |
|---|---|---|
| 1 | `JourneyListScreen.kt` (`JourneyListScreen`) | `/my-journey` — `app/my-journey/page.tsx` |
| 2 | `ContinuePathScreen.kt` (`ContinuePathScreen`) | `/my-journey/[id]` — `app/my-journey/[id]/page.tsx` |
| 3 | `JourneyScreen.kt` (`JourneyScreen`, section 0) | `TodaysJourneySection.tsx` (in `[id]/session/page.tsx`) |
| 4 | `JourneyScreenTwo.kt` (`JourneyScreenTwo`, section 1) | `ContextSection.tsx` |
| 5 | `JourneyScreenThree.kt` (`JourneyScreenThree`, section 2) | `InsightSection.tsx` |
| 6 | `JourneyScreenFour.kt` (`JourneyFormationScreen`, section 3) | `ApplicationSection.tsx` |
| 7 | `JourneyScreenFive.kt` (`JourneyPrayerScreen`, section 4) | `GuidedPrayerSection.tsx` |
| 8 | `JourneyReviewOne.kt` (`JourneyReviewScreen`) | `/my-journey/[id]/feedback` — `feedback/page.tsx` |
| 9 | `JourneyCompletionReview.kt` (`JourneyCompletionReviewScreen`) | `/my-journey/[id]/completed` — `completed/page.tsx` |
| 10 | `JourneyNextJourneyScreen.kt` (`JourneyNextJourneyScreen`) | `/my-journey/[id]/looking-forward` — `looking-forward/page.tsx` |
| 11 | `JourneyDonationScreen.kt` (`JourneyDonationScreen`) | `/my-journey/[id]/support` — `support/page.tsx` |

Web has 7 routes; the session route hosts 5 section components → 11 screen-equivalents total.

---

## 4. Full Content Extraction (done by two parallel agents)

### Web extraction (agent ses_ea65bbbefffegtGVsgzijopFsH) — key findings

- **Live data source:** Convex (`getSessionContent`, `getPathOverview`); `mockData.ts` supplies fallbacks/shapes.
- **My Journey list:** `My Journey` h1; path card = `7 days` badge + `Trust in Uncertainty` + description; empty state `No path selected` / `Sorry you do not have an active path selected.` / `Go to Paths`.
- **Path detail:** hero `Trust in Uncertainty` + hard-coded subtitle `Learn to rest in God's unchanging character…`; CTA `Continue Reading`/`Review Session`; sections `Reading Schedule`, `Overview`, `Sessions`; auto `startPath` mutation on mount; loading spinner; `Path not found.` error.
- **Session pager:** 5 horizontal snap sections; hard-coded section titles: `Historical & Biblical Background`, `What this Reveals About God` (+ `Core Insight` callout fallback `God is Compassionate & Gracious`), `Formation Questions` (3 fallback questions), `Talk to God` (+ `Guided Prayer` callout); S0 has `CORE SCRIPTURE` card; footer = 5 dots + arrows; `Continue to Prayer` CTA on S3; `Complete Session`/`Completing...` on S4 with toast `Day {n} Completed` then navigate to feedback (path complete) or path detail.
- **Feedback:** eyebrow `HOW DID IT GO?`; h1 `How did today's session land for you?`; 3 options (Didn't connect / It was helpful / It landed deeply with sublabels); conditional `Is there anything else…(Optional)` + `Your thoughts..` textarea; CTA `Continue to Completed`. **No bottom nav** (layout hides nav on `/session` and `/feedback` only).
- **Completed:** badge `JOURNEY COMPLETE`, `DAY 7/7` hard-coded; 3 radio question groups + thoughts (`Share my thoughts...`); `Finish` → console.log then `/looking-forward`. Bottom nav visible.
- **Looking Forward:** eyebrow `LOOKING FORWARD`; h1 `What themes would you like to explore next!` (exclamation); `Please select as many as you would like`; 4 multi-select themes (Loss & Grief, Identity & Worth, Pressure & Endurance, Relationships & Community); `Submit` → `/support`.
- **Support:** currency dropdown (USD/NGN/GBP/EUR/CAD/AUD/JPY); `1.) One-Time Gift` ($5/$15/$30; NGN ₦3,000/₦7,000/₦15,000) + `2.) Monthly Support` ($3/$7/$15; NGN ₦2,500/₦5,000/₦10,000); dynamic button labels (`Give $X.XX`, `Subscribe for…`, approx conversions); Stripe checkout redirect; `Maybe Later` → skip + `/`. EXCHANGE_RATES: USD 1.0, NGN 1600.0, GBP 0.79, EUR 0.93, CAD 1.37, AUD 1.50, JPY 161.0.

### Android extraction (agent ses_ea65b9daeffeshcOuXFsFAF4XA) — key findings

- All 11 files ported with extensive "Web …" parity comments; composable-name mismatches: `JourneyScreenFour.kt` → `JourneyFormationScreen`, `JourneyScreenFive.kt` → `JourneyPrayerScreen`, `JourneyReviewOne.kt` → `JourneyReviewScreen`.
- BottomNavBar (`selectedIndex = 2`) on: List, ContinuePath, CompletionReview, NextJourney, Donation. Absent on 5 session screens + ReviewOne — **matches web** (hidden on `/session` and `/feedback`).
- Copy matches web closely (same headings, fallbacks, button labels, placeholders incl. `Your thoughts..` two-dot and `Share my thoughts...` three-dot).
- Exchange rates, currency symbols, presets/tiers, button-label logic, currency-change reset, preset/custom mutual exclusion all mirrored.

---

## 5. Mismatches / Flags Identified (to verify & report)

1. **Q1 duplicate option (possible bug):** `JourneyCompletionReview.kt` Q1 option 2 is `Much deeper than anything I've used before` — same string as Q1 web option 2? Web Q1 options: `I'm more grounded`, `Much deeper than anything I've used before`, `I understand God more`, `I'm not sure`, `I don't notice a difference`. **Actually matches web** (web Q1 also has that string) — verify not a real bug.
2. **Eyebrow punctuation:** Android S0 uses curly `TODAY’S JOURNEY`; web uses `TODAY'S JOURNEY` (straight) — minor.
3. **H1 punctuation parity:** Looking Forward both use `next!` (exclamation) — matches web; KDoc quotes `next?` — comment only.
4. **Dots interactivity:** Web dots are clickable on all sections (aria-labels); Android: dots interactive only on section 0, static on sections 1–4 — **potential UX divergence** (footer arrows still work).
5. **Session completion toast:** Web shows toast `Day {n} Completed` (2s) then navigates; Android KDoc says "straight to completeSession — web has no delay or popup" — **verify Android implements the toast/navigation timing** or diverges.
6. **Feedback CTA naming:** Android `onBackToHome()` callback for `Continue to Completed` — naming oddity only; verify wiring pushes completed screen.
7. **Thoughts persistence:** Both web and Android capture thoughts/notes only locally (not submitted) — parity ✅.
8. **Hard-coded web values:** `DAY 7/7`, `After 7 days…`, hero subtitle, session links to `/my-journey/1/…` — Android mirrors the hard-coding; fine for parity.
9. **Typographic characters:** curly quotes in scripture/defaults vs straight apostrophes elsewhere — spot-check each screen's exact characters if pixel-perfect copy matters.
10. **ContinuePath preview session titles** differ from web session titles (different mock data) — preview-only, not runtime.

---

## 6. Next Steps (when resuming)

- [ ] Produce the final side-by-side parity report for the user (per-screen ✅/⚠️ table with the flags from §5).
- [ ] Decide with user whether to fix divergences (dots interactivity, completion toast timing, eyebrow punctuation) or accept as intentional.
- [ ] If fixes wanted: edit the flagged Kotlin files and run lint/build (`./gradlew :app:assembleDebug` or Android Studio build).
- [ ] Optionally wire remaining live-data params through `MainActivity.kt` journeySection/postCompleteFlow to confirm runtime behavior matches the static extraction.

---

## 7. Key File Paths Cheat-Sheet

**Android:** `app\src\main\java\com\example\lampstand\journey\` — JourneyListScreen.kt, ContinuePathScreen.kt, JourneyScreen.kt, JourneyScreenTwo.kt, JourneyScreenThree.kt, JourneyScreenFour.kt, JourneyScreenFive.kt, JourneyReviewOne.kt, JourneyCompletionReview.kt, JourneyNextJourneyScreen.kt, JourneyDonationScreen.kt

**Web:** `projectLampStand\app\my-journey\` — page.tsx, layout.tsx, [id]/page.tsx, [id]/session/page.tsx, [id]/components/{TodaysJourney,Context,Insight,Application,GuidedPrayer}Section.tsx, [id]/feedback/page.tsx, [id]/completed/page.tsx, [id]/looking-forward/page.tsx, [id]/support/page.tsx

**Flow wiring (Android):** `MainActivity.kt` — journeySection ~lines 740–784, postCompleteFlow ~lines 695–739.
