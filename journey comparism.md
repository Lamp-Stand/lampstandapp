# Journey Comparison — Android vs Web

Date: 2026-06-14
Android: `LampStand\app\src\main\java\com\example\lampstand\journey\` (11 files)
Web: `C:\Users\XYNYDOMEGA\projectLampStand` (Next.js)

## Screen mapping (11 = 11)

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

## Per-screen verdict

| # | Screen | Verdict | Notes |
|---|---|---|---|
| 1 | JourneyListScreen | ✅ Match | Heading, card (7 days / Trust in Uncertainty), empty state ("No path selected" / "Sorry you do not have an active path selected." / "Go to Paths") |
| 2 | ContinuePathScreen | ✅ Match | Hero, Continue Reading / Review Session CTA, Reading Schedule chips, Overview copy, Sessions list, loading spinner, "Path not found." |
| 3 | JourneyScreen (S0) | ✅ Match | TODAY'S JOURNEY eyebrow, CORE SCRIPTURE card, verse + reference, back → path detail, dots + next arrow |
| 4 | JourneyScreenTwo (S1) | ✅ Match | CONTEXT, "Historical & Biblical Background", "Context coming soon." fallback, prev/next nav, 2nd dot active |
| 5 | JourneyScreenThree (S2) | ✅ Match | "What this Reveals About God", Core Insight callout ("God is Compassionate & Gracious"), first-sentence split logic, "Insight coming soon." |
| 6 | JourneyFormationScreen (S3) | ✅ Match | "Formation Questions", 3 fallback questions verbatim, "Your thoughts.." placeholder, "Write your thoughts (optional)", "Continue to Prayer" |
| 7 | JourneyPrayerScreen (S4) | ✅ Match | "Talk to God", Guided Prayer callout, prayer fallback text, "Complete Session" / "Completing..."; toast "Day X Completed" + 2s delay + feedback-or-path-detail nav wired in MainActivity.kt:635-659 |
| 8 | JourneyReviewScreen | ✅ Match | HOW DID IT GO?, 3 emoji options + sublabels, conditional thoughts block, "Your thoughts..", "Continue to Completed", no bottom nav |
| 9 | JourneyCompletionReviewScreen | ✅ Match | JOURNEY COMPLETE badge, DAY 7/7, Q1–Q4 verbatim (Q1's "Much deeper…" string also exists on web), "Share my thoughts...", Finish, bottom nav |
| 10 | JourneyNextJourneyScreen | ✅ Match | LOOKING FORWARD, h1 with "!", 4 themes, multi-select with empty allowed, Submit |
| 11 | JourneyDonationScreen | ✅ Match | 7 currencies, USD/NGN presets + tiers, dynamic button labels, EXCHANGE_RATES mirror, notes, Maybe Later, currency-change reset, preset/custom mutual exclusion |

Bottom-nav visibility matches web: hidden on session + feedback screens only; shown on list, path detail, completed, looking-forward, support.

## Divergences

1. **Session dots not tappable on S1–S5** — web dots jump to any section; Android dots are visual-only on sections 1–5 (only S0's dots are clickable; others navigate via prev/next arrows). Files: `JourneyScreenTwo/Three/Four/Five.kt`
2. **Error display** — Android shows `sessionError` in red on the prayer screen; web only console.logs. Android behavior is arguably better.
3. **Cosmetic** — S0 eyebrow uses curly `TODAY’S JOURNEY` vs web straight `TODAY'S JOURNEY`.
4. **Preview-only** — ContinuePath preview session titles differ from web session titles (no runtime impact).
5. **Intentional** — no Stripe checkout on Android yet (`onGive` callback; documented in `JourneyDonationScreen.kt`).
