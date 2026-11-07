# LampStand Android APK

## Download

- [Download LampStand 1.0.2 APK](https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.2.apk)
- [Release page](https://github.com/xynydomega/lampstandapp/releases)
- [SHA-256 checksum](dist/SHA256.txt)
- Local file: `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand\dist\LampStand-1.0.2.apk`

## APK details

| Field | Value |
|---|---|
| App name | LampStand |
| Package ID | `net.lampstandapp.app` |
| Version | `1.0.2` |
| Version code | `3` |
| Build type | Release |
| Minimum Android | Android 7.0 (API 24) |
| Target Android | API 37 |
| File size | 9,691,026 bytes (9.24 MB) |
| Build date | 2026-11-07 |
| Signing | Release-signed, verified |
| Signer | `CN=LampStand, OU=Mobile, O=LampStand, L=Lagos, ST=Lagos, C=NG` |
| Signer certificate SHA-256 | `5bf2df206106215a981e486c51abd2bd95116af20b75f5627ef758ba1795edc9` |
| APK SHA-256 | `2D9299CDC8F33FBFE21F3D62BCF4DEF1841542E81D7D0725F3D8D6110C24D83F` |
| Backend | Convex `exciting-dodo-656` |

## Install

1. Transfer `LampStand-1.0.2.apk` to the Android device.
2. Open the file.
3. If prompted, allow the file manager/browser to install unknown apps.
4. Tap **Install**.

This package is separate from the old `com.example.lampstand` build. Both can be installed side by side; uninstall the old app if only one is wanted.

## Hosting notes for PM

1. Upload `dist/LampStand-1.0.2.apk` to the website or file host.
2. Link the download button to that uploaded file.
3. Optionally publish the SHA-256 checksum next to the download link.
4. Do not rename the package or re-sign the APK.

## Signing / update key

The release keystore is kept outside version control:

- `lampstand-release.jks`
- `keystore.properties`

Back these up securely. Losing them prevents publishing future updates under the same app identity. Do not commit or share the keystore password.

## What changed in this build

- **Restart Journey** now calls Convex `users:resetUserJourney` (server-side path progress reset). The Revert control was removed — restart is one-way.
- **Notification icon** reverted to the bell (status bar + signed-out header no longer show a solid blob).
- **Daily reminders rebuilt:** four independent alarms (Morning / Noon / Evening / Night) are armed once for their next occurrence and left alone. Each slot re-arms itself only after it fires. Alarms are cancelled only when today's session is complete, reminders are off, or the path is finished.
- Fixed **9pm / background reminders never firing unless the app was open**: arming no longer requires exact-alarm permission (falls back to inexact), and a missed slot is re-filled by `sync()` instead of waiting for the next app open.
- Boot / timezone / app-update and reminder-time edits trigger a full `reschedule()` rebuild.

## Is this change in the APK link?

**Yes, once published.** This build is `v1.0.2` (`versionCode 3`) and installs as an update over 1.0 / 1.0.1 without uninstalling.

- Current SHA-256: `2D9299CDC8F33FBFE21F3D62BCF4DEF1841542E81D7D0725F3D8D6110C24D83F`
- Previous SHA-256 (1.0.1): `87E8609E7B3CF3AEABBBC68CFB98537BF77170FA256040FC4743704339CFF3E6`

## Notes

- Future sessions are intentionally locked by Convex until their calendar day.
- Google and donation deep links still use the existing `com.example.lampstand://` scheme so the current Convex backend allowlist keeps working. Renaming that scheme requires a backend allowlist update first.
- No physical device was attached during the build, so alarm delivery and Google login were not runtime-tested. After install, open the app once so the four daily alarms arm; force-stop from Settings clears them until the next open.
