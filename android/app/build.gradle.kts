import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing lives in android/key.properties, which is gitignored (this repo is public).
// Locally, run `keytool -genkeypair ...` and copy key.properties.example -> key.properties.
// In CI, both files are written from GitHub Actions secrets before the build runs.
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
val hasKeystoreProperties = keystorePropertiesFile.exists()
if (hasKeystoreProperties) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.devconnectx.skipwise"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "com.devconnectx.skipwise"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (hasKeystoreProperties) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Falls back to the debug key when key.properties is absent (e.g. a fresh
            // checkout without the release keystore), so `flutter run --release` still
            // works locally. CI and real release builds always provide key.properties.
            signingConfig = if (hasKeystoreProperties) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // R8 minification/obfuscation was tried in the initial rebrand, but Google
            // Play Protect blocks obfuscated Accessibility-Service APKs from unknown
            // (non-Play-Store) sources much more aggressively — obfuscation is one of
            // the signals malware uses to evade static analysis, and this app trips it
            // even though it's not malicious. The Kotlin glue here is tiny (Flutter's
            // engine dominates APK size), so shrinking buys negligible size savings but
            // was making the release APK unable to install at all. Not worth the tradeoff
            // for an app distributed via GitHub Releases rather than the Play Store.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

flutter {
    source = "../.."
}
