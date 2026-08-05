allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Build outputs are redirected outside the project tree, into the current user's home
// directory. Locally this is required: the project lives on an exFAT external volume,
// which makes macOS spawn "._" AppleDouble sidecar files next to every directory Gradle
// creates, and AAPT2's resource-directory scan then trips over them (e.g. "._drawable-v21
// is not a directory"). It's also kept on CI: `flutter build apk` was unable to locate its
// own output ("Gradle build failed to produce an .apk file") when the build directory
// stayed at the default project-relative path on the GitHub Actions Ubuntu runner, for
// unrelated reasons (a "some/build/apk" watcher registered twice — the -v log showed
// "Unable to watch same file twice via different paths"). Redirecting unblocks both.
val newBuildDir = File(System.getProperty("user.home"), ".flutter-android-build/skipwise")
rootProject.layout.buildDirectory.set(newBuildDir)

subprojects {
    val newSubprojectBuildDir = newBuildDir.resolve(project.name)
    project.layout.buildDirectory.set(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
