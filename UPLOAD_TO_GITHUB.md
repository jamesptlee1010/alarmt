# Upload TAZLARM v2.2.6 and build the APK

1. Extract `TAZLARM_v2.2.6_FULL_PROJECT.zip`.
2. Replace the contents of your local cloned repository with the extracted project contents, preserving the repository's hidden `.git` folder.
3. Confirm `.github`, `app`, `gradle`, `build.gradle.kts`, `settings.gradle.kts` and `personal-release.keystore` are at the repository root.
4. Commit and push using GitHub Desktop.
5. Open GitHub → Actions → Build TAZLARM APK → Run workflow.
6. Download `TAZLARM-v2.2.6-installable-APK` from the successful run.
