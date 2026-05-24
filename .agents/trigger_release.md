# Skill: Trigger New Release

**Description:** Use this skill to prepare and publish a new release of the application.

**Steps:**
1. Check the current `versionName` and `versionCode` in the app's `build.gradle.kts` (located inside the `android-app/app` directory).
2. Increment the version appropriately.
3. Build the release APK by running the Gradle task: `cd android-app && ./gradlew assembleRelease`
4. Commit the version changes and push them to the repository.
5. Create a new GitHub Release (e.g., using the `gh release create` command) and upload the generated `.apk` file from `android-app/app/build/outputs/apk/release/` as an asset to the release.
