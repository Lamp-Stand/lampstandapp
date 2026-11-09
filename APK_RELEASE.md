# LampStand Android APK

## Download

- [Download LampStand 1.0.3 APK](https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.3.apk)
- [Release page](https://github.com/xynydomega/lampstandapp/releases)
- [SHA-256 checksum](dist/SHA256.txt)
- Local file: `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand\dist\LampStand-1.0.3.apk`

## APK details

| Field | Value |
|---|---|
| App name | LampStand |
| Package ID | `net.lampstandapp.app` |
| Version | `1.0.3` |
| Version code | `4` |
| Build type | Release |
| Minimum Android | Android 7.0 (API 24) |
| Target Android | API 37 |
| File size | 9,691,026 bytes (9.24 MB) |
| Build date | 2026-11-09 |
| Signing | Release-signed, verified |
| Signer | `CN=LampStand, OU=Mobile, O=LampStand, L=Lagos, ST=Lagos, C=NG` |
| Signer certificate SHA-256 | `5bf2df206106215a981e486c51abd2bd95116af20b75f5627ef758ba1795edc9` |
| APK SHA-256 | `3997853FC903E36FC9BA7A0E01F7DA3D7976A9803060E8277DF802ACAD1F944E` |
| Backend | Convex `exciting-dodo-656` |

## Install

1. Transfer `LampStand-1.0.3.apk` to the Android device.
2. Open the file.
3. If prompted, allow the file manager/browser to install unknown apps.
4. Tap **Install**.

This package is separate from the old `com.example.lampstand` build. Both can be installed side by side; uninstall the old app if only one is wanted.

## Hosting notes for PM

1. Upload `dist/LampStand-1.0.3.apk` to the website or file host.
2. Link the download button to that uploaded file.
3. Optionally publish the SHA-256 checksum next to the download link.
4. Do not rename the package or re-sign the APK.

## Signing / update key

The release keystore is kept outside version control:

- `lampstand-release.jks`
- `keystore.properties`

Back these up securely. Losing them prevents publishing future updates under the same app identity. Do not commit or share the keystore password.

## What changed in this build

- Fixed **stale "Session unavailable" after restarting**: the saved "come back tomorrow" note is now re-checked against the real unlock time and dropped once the wait is over, so it can no longer block today's session after an app restart or after the reminder fires.
- **Restart Session dialog** no longer dead-ends: it clears the stale note/old error and falls back to the backend session id after a process restart, so the session actually loads again.
- **Restart Journey** now wipes leftover session/availability state on success, so the reset Day 1 opens clean.

### 1.0.2 (previous)

- **Restart Journey** now calls Convex `users:resetUserJourney` (server-side path progress reset). The Revert control was removed — restart is one-way.
- **Notification icon** reverted to the bell (status bar + signed-out header no longer show a solid blob).
- **Daily reminders rebuilt:** four independent alarms (Morning / Noon / Evening / Night) are armed once for their next occurrence and left alone. Each slot re-arms itself only after it fires. Alarms are cancelled only when today's session is complete, reminders are off, or the path is finished.
- Fixed **9pm / background reminders never firing unless the app was open**: arming no longer requires exact-alarm permission (falls back to inexact), and a missed slot is re-filled by `sync()` instead of waiting for the next app open.
- Boot / timezone / app-update and reminder-time edits trigger a full `reschedule()` rebuild.
- **Brighter dialogs:** Restart Journey and Session unavailable now use a white surface, bold title, and stronger button colors.

## Is this change in the APK link?

**Yes, once published.** This build is `v1.0.3` (`versionCode 4`) and installs as an update over 1.0 / 1.0.1 / 1.0.2 without uninstalling.

- Current SHA-256 (1.0.3): `3997853FC903E36FC9BA7A0E01F7DA3D7976A9803060E8277DF802ACAD1F944E`
- Previous SHA-256 (1.0.2): `A034E8988BA3E5687343C7FDDEFE796874FDF220122FB0208FE0414944B6BE5E`

## Notes

- Future sessions are intentionally locked by Convex until their calendar day.
- Google and donation deep links still use the existing `com.example.lampstand://` scheme so the current Convex backend allowlist keeps working. Renaming that scheme requires a backend allowlist update first.
- No physical device was attached during the build, so alarm delivery and Google login were not runtime-tested. After install, open the app once so the four daily alarms arm; force-stop from Settings clears them until the next open.
