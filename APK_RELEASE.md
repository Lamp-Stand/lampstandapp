# LampStand Android APK

## Download

- [Download LampStand 1.0.1 APK](https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.1.apk)
- [Stable latest link (same URL as before)](https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.apk)
- [Release page](https://github.com/xynydomega/lampstandapp/releases/tag/v1.0.1)
- [SHA-256 checksum](dist/SHA256.txt)
- Local file: `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand\dist\LampStand-1.0.apk`

## APK details

| Field | Value |
|---|---|
| App name | LampStand |
| Package ID | `net.lampstandapp.app` |
| Version | `1.0.1` |
| Version code | `2` |
| Build type | Release |
| Minimum Android | Android 7.0 (API 24) |
| Target Android | API 37 |
| File size | 9,355,815 bytes (8.92 MB) |
| Build date | 2026-10-25 |
| Signing | Release-signed, verified |
| Signer | `CN=LampStand, OU=Mobile, O=LampStand, L=Lagos, ST=Lagos, C=NG` |
| Signer certificate SHA-256 | `5bf2df206106215a981e486c51abd2bd95116af20b75f5627ef758ba1795edc9` |
| APK SHA-256 | `87E8609E7B3CF3AEABBBC68CFB98537BF77170FA256040FC4743704339CFF3E6` |
| Backend | Convex `exciting-dodo-656` |

## Install

1. Transfer `LampStand-1.0.apk` to the Android device.
2. Open the file.
3. If prompted, allow the file manager/browser to install unknown apps.
4. Tap **Install**.

This package is separate from the old `com.example.lampstand` build. Both can be installed side by side; uninstall the old app if only one is wanted.

## Hosting notes for PM

1. Upload `dist/LampStand-1.0.apk` to the website or file host.
2. Link the download button to that uploaded file.
3. Optionally publish the SHA-256 checksum next to the download link.
4. Do not rename the package or re-sign the APK.

## Signing / update key

The release keystore is kept outside version control:

- `lampstand-release.jks`
- `keystore.properties`

Back these up securely. Losing them prevents publishing future updates under the same app identity. Do not commit or share the keystore password.

## What changed in this build

- Fixed the session error shown after completing or re-reading a session.
- Session content now loads only while the session screen is open, so the next locked day can no longer overwrite the screen during completion.
- Re-reading a completed day and tapping **Complete Session** now shows when the next session becomes available, with only **Back to Journey** (no retry).
- Stale session responses can no longer replace the session being read.
- Convex request errors are shown as readable messages instead of raw request IDs.

## Is this change in the APK link?

**Yes.** The latest release is `v1.0.1` (`versionCode 2`), and both links above serve the new APK.

- Current SHA-256: `87E8609E7B3CF3AEABBBC68CFB98537BF77170FA256040FC4743704339CFF3E6`
- Previous SHA-256: `20B57930B6A9046F49F6CEA86924CF8172648E11FC3E5833E3092CC47D9AAE6D`

`versionCode` is now `2`, so this build installs as an update over the older `1.0` build without uninstalling it.

## Notes

- Future sessions are intentionally locked by Convex until their calendar day. This build no longer opens a locked next-day session or shows the `Context coming soon.` / `Insight coming soon.` fallbacks for it; it shows the path schedule or a clear locked message instead.
- Google and donation deep links still use the existing `com.example.lampstand://` scheme so the current Convex backend allowlist keeps working. Renaming that scheme requires a backend allowlist update first.
- No physical device was attached during the build, so alarm delivery and Google login were not runtime-tested.
