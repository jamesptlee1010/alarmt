# GitHub Actions build-root fix

The earlier workflow assumed that `settings.gradle.kts` was located directly at the repository root. The error:

```text
Directory '.../latestalarm/latestalarm' does not contain a Gradle build.
```

means GitHub checked out a repository whose root did not contain the extracted Android project.

The replacement workflow now:

1. Looks for `settings.gradle`, `settings.gradle.kts`, or `settings.gradle.dcl` at the root or inside a nested project folder.
2. If no project is visible, looks for a committed TAZALARM ZIP and extracts it automatically.
3. Runs Gradle with `-p <detected-project-directory>`.
4. Uses the detected directory for tests, APK creation, and signature verification.
5. Prints a useful repository tree if no Android project can be found.

## Fastest repair in the existing repository

Replace this file:

```text
.github/workflows/build-tazalarm.yml
```

with the version in this package, commit the change, then run **Actions → Build TAZALARM APK → Run workflow**.

The cleanest repository structure remains:

```text
.github/
app/
gradle/
build.gradle.kts
gradle.properties
settings.gradle.kts
personal-release.keystore
```

Do not upload the containing folder as one nested directory unless you use the repaired workflow.
