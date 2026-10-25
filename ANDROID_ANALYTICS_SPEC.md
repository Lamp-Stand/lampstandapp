# Android Analytics - Backend Quickstart

Build three things:

1. An `analyticsEvents` table.
2. One `analytics:ingest` mutation.
3. A small web admin dashboard filtered to `platform = "android"`.

The Android app sends the events. The backend stores them, validates them, and
shows them. Identity comes from the existing `users` table.

## MVP scope (aim for 1-2 days)

| Must have | Can come later |
| --- | --- |
| Event table + ingest mutation | Funnels |
| Admin auth check | Retention cohorts |
| Overview metrics | Session section drop-off |
| Event explorer | Push/notification deep analytics |
| User list joined to `users` | Scheduled daily aggregates |

## 1. Schema

```ts
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  analyticsEvents: defineTable({
    eventName: v.string(),
    eventId: v.string(),
    userId: v.optional(v.id("users")),
    anonymousId: v.string(),
    sessionId: v.string(),
    occurredAt: v.number(),
    receivedAt: v.number(),
    platform: v.literal("android"),
    appVersion: v.string(),
    versionCode: v.number(),
    osVersion: v.string(),
    deviceModel: v.string(),
    manufacturer: v.string(),
    locale: v.string(),
    timezone: v.string(),
    properties: v.any(),
  })
    .index("by_event_time", ["eventName", "occurredAt"])
    .index("by_user_time", ["userId", "occurredAt"])
    .index("by_time", ["occurredAt"])
    .index("by_event_id", ["eventId"]),
});
```

Convex indexes are not unique. Before inserting, query `by_event_id`; if it
exists, skip it. A rare duplicate during a race is acceptable for MVP.

## 2. Ingest mutation

```ts
import { mutation } from "./_generated/server";
import { v } from "convex/values";

const ALLOWED_EVENTS = new Set([
  "app_opened",
  "screen_viewed",
  "sign_in_started",
  "sign_in_succeeded",
  "sign_in_failed",
  "sign_out",
  "onboarding_started",
  "onboarding_completed",
  "paths_viewed",
  "path_opened",
  "path_started",
  "session_started",
  "session_section_viewed",
  "session_completed",
  "session_abandoned",
  "reminder_toggled",
  "notification_shown",
  "notification_opened",
  "profile_updated",
  "api_error",
]);

export const ingest = mutation({
  args: {
    events: v.array(v.any()),
  },
  handler: async (ctx, { events }) => {
    const identity = await ctx.auth.getUserIdentity();
    const userId = identity ? await lookupUserId(ctx, identity.tokenIdentifier) : undefined;

    let accepted = 0;
    let rejected = 0;

    for (const e of events.slice(0, 50)) {
      if (!isValidEvent(e)) {
        rejected++;
        continue;
      }

      const duplicate = await ctx.db
        .query("analyticsEvents")
        .withIndex("by_event_id", (q) => q.eq("eventId", e.eventId))
        .first();
      if (duplicate) {
        rejected++;
        continue;
      }

      await ctx.db.insert("analyticsEvents", {
        eventName: e.eventName,
        eventId: e.eventId,
        userId,
        anonymousId: String(e.anonymousId ?? ""),
        sessionId: String(e.sessionId ?? ""),
        occurredAt: Number(e.occurredAt ?? Date.now()),
        receivedAt: Date.now(),
        platform: "android",
        appVersion: String(e.appVersion ?? ""),
        versionCode: Number(e.versionCode ?? 0),
        osVersion: String(e.osVersion ?? ""),
        deviceModel: String(e.deviceModel ?? ""),
        manufacturer: String(e.manufacturer ?? ""),
        locale: String(e.locale ?? ""),
        timezone: String(e.timezone ?? ""),
        properties: e.properties ?? {},
      });
      accepted++;
    }

    return { accepted, rejected };
  },
});
```

`lookupUserId` is a placeholder for your existing identity mapping. Use the
same lookup the rest of your backend already uses to turn a Convex identity into
a `users` row. Never trust a `userId` sent by the app.

`isValidEvent` only needs to check:

- `eventName` is in `ALLOWED_EVENTS`
- `eventId` is a non-empty string
- payload is under 4 KB
- no forbidden keys anywhere in `properties`: `email`, `name`, `phone`,
  `firstName`, `lastName`, `password`, `token`, `content`, `notes`

```ts
function isValidEvent(e: any): boolean {
  if (!e || typeof e !== "object") return false;
  if (!ALLOWED_EVENTS.has(e.eventName)) return false;
  if (typeof e.eventId !== "string" || !e.eventId) return false;
  if (JSON.stringify(e).length > 4096) return false;
  const props = JSON.stringify(e.properties ?? {});
  const forbidden = ["email", "phone", "firstName", "lastName", "password", "token", "content", "notes"];
  return !forbidden.some((key) => props.includes(`"${key}"`));
}
```

## 3. What the app will send

The app team implements these. The backend only needs to accept the names and
store the payload.

| Event | Properties |
| --- | --- |
| `app_opened` | `coldStart` |
| `screen_viewed` | `screen` |
| `sign_in_started` | `method` (`google` or `email_otp`) |
| `sign_in_succeeded` | `method` |
| `sign_in_failed` | `method`, `errorCode` |
| `sign_out` | none |
| `onboarding_started` | none |
| `onboarding_completed` | `firstPathStarted` |
| `paths_viewed` | `pathCount` |
| `path_opened` | `pathId` |
| `path_started` | `pathId` |
| `session_started` | `pathId`, `sessionId`, `dayNumber`, `source` |
| `session_section_viewed` | `section`, `sectionIndex` |
| `session_completed` | `pathId`, `sessionId`, `dayNumber`, `durationSeconds` |
| `session_abandoned` | `pathId`, `sessionId`, `lastSection`, `durationSeconds` |
| `reminder_toggled` | `enabled` |
| `notification_shown` | `slot`, `messagePool` |
| `notification_opened` | `slot`, `messagePool` |
| `profile_updated` | `changedFirstName`, `changedLastName`, `changedPhone` |
| `api_error` | `operation`, `statusCode`, `networkError` |

Every event also has the common fields listed in section 2.

## 4. Admin auth

Reuse your existing admin check. Every admin query should start with:

```ts
async function assertAdmin(ctx: any) {
  const identity = await ctx.auth.getUserIdentity();
  if (!identity) throw new Error("Not signed in");
  const user = await lookupUser(ctx, identity.tokenIdentifier);
  if (!user?.isAdmin) throw new Error("Not an admin");
  return user;
}
```

Add `isAdmin` to `users` if it does not exist, or use a dedicated admin table.
Never trust a client-side flag.

## 5. Dashboard (minimum)

Reuse the existing Lampstand web admin app. Create an "Android Analytics" page
with these tabs:

1. **Overview**
   - DAU today, WAU, MAU
   - sessions started, sessions completed, completion rate
   - reminders on/off count
   - notifications shown vs opened

2. **Users**
   - list of users with: name, email, last seen, sessions completed, reminders on/off
   - join `analyticsEvents.userId` to `users`
   - support search by name/email

3. **Events**
   - recent events with filters: date range, event name, app version, user
   - newest first, paginated

4. **Notifications**
   - counts by slot (`06:45`, `11:45`, `16:45`, `21:45`)
   - shown vs opened per slot

Suggested queries:

```ts
// Overview counts
const since = Date.now() - 24 * 60 * 60 * 1000;
const events = await ctx.db
  .query("analyticsEvents")
  .withIndex("by_time", (q) => q.gte("occurredAt", since))
  .take(5000);
```

For MVP, fetching up to 5,000 recent events per page and counting in memory is
fine. Add a scheduled daily aggregation table later if volume grows.

## 6. Privacy rules

- Store no names, emails, phones, prayer content, or spiritual details in events.
- Show names/emails only in the admin UI by joining `users` on `userId`.
- Do not let non-admins query `analyticsEvents`.
- Document retention; 12 months for raw events is reasonable.

## 7. Done checklist

- [ ] `analyticsEvents` table deployed
- [ ] `analytics:ingest` accepts valid batches and rejects invalid ones
- [ ] Backend derives `userId`; client `userId` is ignored
- [ ] Duplicate `eventId` is skipped
- [ ] Admin-only access enforced server-side
- [ ] Dashboard shows Android-only overview, users, events, notifications
- [ ] Users page joins name/email from `users`
- [ ] No PII stored in event documents
