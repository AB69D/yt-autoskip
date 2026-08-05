allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Build outputs are redirected off the project's exFAT external volume: that filesystem
// makes macOS spawn "._" AppleDouble sidecar files next to every directory Gradle creates,
// and AAPT2's resource-directory scan then trips over them (e.g. "._drawable-v21 is not a
// directory"). Building on the APFS-formatted home volume avoids that entirely.
//
// Skipped on CI (and any other Linux/non-exFAT runner): redirecting the build dir there
// makes `flutter build apk` unable to find its own output at the project-relative path it
// expects, which fails the build. GitHub Actions (and most CI systems) set CI=true.
if (System.getenv("CI") == null) {
    val newBuildDir = File("/Users/ab9d/.flutter-android-build/skipwise")
    rootProject.layout.buildDirectory.set(newBuildDir)

    subprojects {
        val newSubprojectBuildDir = newBuildDir.resolve(project.name)
        project.layout.buildDirectory.set(newSubprojectBuildDir)
    }
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
