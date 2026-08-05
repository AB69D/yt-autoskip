# YT AutoSkip

An Android app that automatically taps "Skip Ad" for you in the YouTube and Facebook apps, using Android's Accessibility Service — no root required.

It also ships a setup checklist that walks you through the extra permissions Xiaomi/MIUI devices need, since MIUI aggressively kills background services by default and will silently turn the skipper off if you don't configure it.

## Features

- **Automatic ad skipping** in the YouTube app, and the Facebook app's in-feed video ads.
- **Live status dashboard** — shows whether the accessibility service is running and a running count of ads skipped.
- **MIUI setup checklist** — one-tap shortcuts to:
  - Enable the accessibility permission
  - Exempt the app from battery optimization
  - Enable Xiaomi's Autostart permission
  - A reminder to lock the app in Recents

## How it works

The app installs an [`AccessibilityService`](android/app/src/main/kotlin/com/devconnectx/ytautoskip/YoutubeAdSkipService.kt) that watches for screen-content-change events from the YouTube and Facebook packages. On each event it walks the current screen's accessibility node tree looking for a node whose resource id, label, or content description matches a "Skip Ad" control, then performs a click on the nearest clickable ancestor. Matching is throttled (every 250ms) to keep CPU/battery usage low.

The Flutter UI ([`lib/main.dart`](lib/main.dart)) talks to the native Android side over a `MethodChannel` (`com.devconnectx.ytautoskip/accessibility`) to check permission status, read the skip counter, and deep-link into the relevant system settings screens.

## Requirements

- Android device (the accessibility service is Android-only; there is no iOS implementation)
- [Flutter SDK](https://docs.flutter.dev/get-started/install) (Dart SDK `^3.10.4`, see [`pubspec.yaml`](pubspec.yaml))
- Android accessibility permission, granted manually by the user at runtime

## Getting started

```bash
flutter pub get
flutter run
```

To build a release APK:

```bash
flutter build apk --release
```

## First-time setup on the device

1. Install and open the app.
2. Tap **Open Accessibility Settings** and enable "YT AutoSkip".
3. Tap **Allow unrestricted battery use** so the OS doesn't freeze the service in the background.
4. On Xiaomi/MIUI devices, tap **Open Autostart settings** and enable autostart for the app.
5. Open Recents, find YT AutoSkip, and tap the lock icon on its card so MIUI can't kill it when swiped away.

## Project structure

```
lib/main.dart                                                        Flutter UI (status card + setup checklist)
android/app/src/main/kotlin/.../MainActivity.kt                      Platform channel: settings deep-links, permission checks
android/app/src/main/kotlin/.../YoutubeAdSkipService.kt              Accessibility service: detects and taps "Skip Ad"
android/app/src/main/res/xml/accessibility_service_config.xml        Accessibility service configuration
```

## Disclaimer

This app works by programmatically interacting with the UI of third-party apps (YouTube, Facebook) via Android's Accessibility API. This may be against those apps' Terms of Service. It is provided for personal and educational use; use at your own risk.

## License

Released under the [MIT License](LICENSE).
