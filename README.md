# SkipWise

An app that automatically taps "Skip Ad" for you in the YouTube and Facebook Android apps, using Android's Accessibility Service — no root required.

It also ships a setup checklist that walks you through the extra permissions Xiaomi/MIUI devices need, since MIUI aggressively kills background services by default and will silently turn the skipper off if you don't configure it.

The Flutter UI itself also builds and runs on Windows, Linux, and macOS — see [Platform support](#platform-support) for what that does and doesn't mean.

## Download

Every push to `main` builds all four platforms and publishes them to the [Releases page](https://github.com/AB69D/yt-autoskip/releases/tag/latest):

| Platform | File | Run it |
|---|---|---|
| Android | `SkipWise.apk` | Open it on your phone (allow "install unknown apps" for whichever app you downloaded it with) |
| Windows | `SkipWise-windows.zip` | Extract, run `skipwise.exe` |
| Linux | `SkipWise-linux.tar.gz` | Extract, run `./skipwise` from inside the extracted folder |
| macOS | `SkipWise-macos.zip` | Extract, right-click `SkipWise.app` → Open (unsigned — no Apple Developer certificate — so Gatekeeper needs that once) |

## Platform support

**Only the Android build actually skips ads.** The mechanism this app is built around — an Accessibility Service that watches the YouTube/Facebook Android apps' screens and taps "Skip Ad" — only exists on Android; Windows, Linux, and macOS don't run those mobile apps, so there's nothing for it to act on there.

The Windows/Linux/macOS builds are the same Flutter UI shell, provided so the project can be built, run, and inspected on desktop. On those platforms the app detects it isn't on Android and shows an explanatory screen instead of the accessibility status card ([`lib/main.dart`](lib/main.dart), see `_isAndroid` / `_DesktopNoticeView`).

## Features

- **Automatic ad skipping** in the YouTube app, and the Facebook app's in-feed video ads.
- **Live status dashboard** — shows whether the accessibility service is running and a running count of ads skipped.
- **MIUI setup checklist** — one-tap shortcuts to:
  - Enable the accessibility permission
  - Exempt the app from battery optimization
  - Enable Xiaomi's Autostart permission
  - A reminder to lock the app in Recents

## How it works

The app installs an [`AccessibilityService`](android/app/src/main/kotlin/com/devconnectx/skipwise/AdSkipAccessibilityService.kt) that watches for screen-content-change events from the YouTube and Facebook packages. On each event it walks the current screen's accessibility node tree looking for a node whose resource id, label, or content description matches a "Skip Ad" control, then performs a click on the nearest clickable ancestor. Matching is throttled (every 250ms) to keep CPU/battery usage low.

The Flutter UI ([`lib/main.dart`](lib/main.dart)) talks to the native Android side over a `MethodChannel` (`com.devconnectx.skipwise/accessibility`) to check permission status, read the skip counter, and deep-link into the relevant system settings screens.

## Requirements

- To actually skip ads: an Android device, with the accessibility permission granted manually at runtime (there is no iOS implementation, and no equivalent on desktop — see [Platform support](#platform-support))
- [Flutter SDK](https://docs.flutter.dev/get-started/install) (Dart SDK `^3.10.4`, see [`pubspec.yaml`](pubspec.yaml))

## Getting started

```bash
flutter pub get
flutter run
```

## Release signing

Release builds are signed with an upload key, not the debug key. The keystore itself is **not** committed to this repo (it's public) — see [`android/key.properties.example`](android/key.properties.example) for the format.

**Local release builds:**

```bash
keytool -genkeypair -v -keystore android/keystore/skipwise-upload.jks \
  -alias skipwise-upload -keyalg RSA -keysize 2048 -validity 10950
cp android/key.properties.example android/key.properties
# edit android/key.properties with the passwords/alias you just used
flutter build apk --release
```

Without `android/key.properties` present, release builds silently fall back to the debug key (so `flutter run --release` still works for local testing) — real release builds always need the real keystore.

**Back up the keystore.** If it's ever lost, there is no way to publish an update under the same app identity — Play Store and any device that already has the app installed would reject a future update signed with a different key. Store a copy of `android/keystore/skipwise-upload.jks` and its passwords somewhere durable (a password manager, encrypted backup) outside of this machine.

## CI/CD

- [`.github/workflows/ci.yml`](.github/workflows/ci.yml) — on every push and pull request to `main`: `flutter analyze`, `flutter test`, an Android debug build, and debug build sanity checks on `windows-latest`, `ubuntu-latest` (Linux desktop), and `macos-latest`.
- [`.github/workflows/release.yml`](.github/workflows/release.yml) — on every push to `main`, builds all four platforms in parallel jobs and publishes/updates the [`latest` GitHub Release](https://github.com/AB69D/yt-autoskip/releases/tag/latest) with all four artifacts attached. Only the Android job signs anything — its keystore and passwords are stored as encrypted [repository secrets](https://github.com/AB69D/yt-autoskip/settings/secrets/actions) (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`), decoded fresh on each run — they are never written to the repo itself. The Windows/Linux/macOS builds are unsigned (no paid certificates for those platforms).

## Hardening / reverse-engineering resistance

Release builds enable R8 code shrinking, obfuscation, and resource shrinking (`isMinifyEnabled` / `isShrinkResources` in [`android/app/build.gradle.kts`](android/app/build.gradle.kts)), strip `Log.*` calls ([`android/app/proguard-rules.pro`](android/app/proguard-rules.pro)), and disable `android:allowBackup` so `adb backup` can't pull app data off a device.

**Important caveat:** this repo is public, so the Kotlin/Dart source is fully readable on GitHub regardless of what the compiled APK looks like. The R8/ProGuard hardening above only raises the bar for someone who *only* has the APK (no source access) — it does not, and cannot, hide anything from someone reading this repository directly.

## First-time setup on the device

1. Install and open the app.
2. Tap **Open Accessibility Settings** and enable "SkipWise".
3. Tap **Allow unrestricted battery use** so the OS doesn't freeze the service in the background.
4. On Xiaomi/MIUI devices, tap **Open Autostart settings** and enable autostart for the app.
5. Open Recents, find SkipWise, and tap the lock icon on its card so MIUI can't kill it when swiped away.

## Project structure

```
lib/main.dart                                                          Flutter UI (status card + setup checklist + desktop notice)
android/app/src/main/kotlin/.../MainActivity.kt                        Platform channel: settings deep-links, permission checks
android/app/src/main/kotlin/.../AdSkipAccessibilityService.kt          Accessibility service: detects and taps "Skip Ad"
android/app/src/main/res/xml/accessibility_service_config.xml          Accessibility service configuration
windows/, linux/, macos/                                               Desktop runner shells (UI only — no ad-skip capability there)
assets/icon/                                                           Source images for the app icon (regenerate via flutter_launcher_icons)
.github/workflows/                                                     CI (analyze/test/build) and release (all 4 platforms) pipelines
```

## Play Store status

This app is **not yet published to Google Play.** Before submitting, be aware that Google Play restricts the Accessibility API to genuine accessibility use cases, and apps that use it to automate interactions with another app's ads (as this one does) are a common rejection/suspension category — doubly so since Google also owns YouTube. A privacy policy, an in-app permission-rationale screen, and a completed Data Safety form are all required for submission regardless. Until/unless it's live on Play, the [Releases page](#download) is the supported distribution channel.

## Disclaimer

This app works by programmatically interacting with the UI of third-party apps (YouTube, Facebook) via Android's Accessibility API. This may be against those apps' Terms of Service. It is provided for personal and educational use; use at your own risk.

## License

Released under the [MIT License](LICENSE).
