# LampStand Android APK

## Download

- [Download LampStand 1.0 APK](https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.apk)
- [SHA-256 checksum](dist/SHA256.txt)
- Local file: `C:\Users\XYNYDOMEGA\AndroidStudioProjects\LampStand\dist\LampStand-1.0.apk`
- Public direct link: https://github.com/xynydomega/lampstandapp/releases/latest/download/LampStand-1.0.apk

## APK details

| Field | Value |
|---|---|
| App name | LampStand |
| Package ID | `net.lampstandapp.app` |
| Version | `1.0` |
| Version code | `1` |
| Build type | Release |
| Minimum Android | Android 7.0 (API 24) |
| Target Android | API 37 |
| File size | 8.92 MB |
| Build date | 2026-10-21 |
| Signing | Release-signed, verified |
| Signer | `CN=LampStand, OU=Mobile, O=LampStand, L=Lagos, ST=Lagos, C=NG` |
| Signer certificate SHA-256 | `5bf2df206106215a981e486c51abd2bd95116af20b75f5627ef758ba1795edc9` |
| APK SHA-256 | `20B57930B6A9046F49F6CEA86924CF8172648E11FC3E5833E3092CC47D9AAE6D` |
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

- Fixed the Home button so it no longer opens a locked next-day session from `home.sessionId`.
- If the next session is locked, it now opens the path schedule with the lock state instead.
- Replaced the misleading `Context coming soon.` / `Insight coming soon.` fallback with a clear “Session unavailable” screen.
- Made Convex errors readable instead of raw JSON.
- Rebuilt debug and release APKs; lint passes.

## Is this change in the APK link?

**Local link: yes.** The file at `dist/LampStand-1.0.apk` was replaced with the fixed build, so the link above now serves the new APK.

**Website/public link: no, not automatically.** If the PM already uploaded the old APK to the website, that hosted copy is still the old one. The PM must re-upload the new file.

- New SHA-256: `20B57930B6A9046F49F6CEA86924CF8172648E11FC3E5833E3092CC47D9AAE6D`
- Old SHA-256: `70F66EA8D15BCB438C4D6B130FC73786CC7CDFF2047D7E3DAB008C2900FF2375`

If the website file still has the old hash, it is the old build. The backend/web side was not changed, only the Android app.

## Notes

- Future sessions are intentionally locked by Convex until their calendar day. This build no longer opens a locked next-day session or shows the `Context coming soon.` / `Insight coming soon.` fallbacks for it; it shows the path schedule or a clear locked message instead.
- Google and donation deep links still use the existing `com.example.lampstand://` scheme so the current Convex backend allowlist keeps working. Renaming that scheme requires a backend allowlist update first.
- No physical device was attached during the build, so alarm delivery and Google login were not runtime-tested.
