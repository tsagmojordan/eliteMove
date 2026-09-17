# Implementation Plan - Fix Build and Export APK

The project is currently failing to build because the Kotlin Kapt plugin cannot parse the Java version (25.0.3) being used by the Gradle daemon. This is a compatibility issue between older versions of Kotlin/IntelliJ tools and Java 25.

I will fix this by upgrading the Kotlin version to a version that supports Java 25 (or by configuring a Java Toolchain if necessary). After the fix, I will build the APK and move it to your downloads folder as requested.

## User Review Required

> [!IMPORTANT]
> I will be upgrading Kotlin from `2.0.21` to `2.1.0`. This might require some adjustments if there are breaking changes, though for a project of this size, it should be smooth.
> I will build a **Debug APK** for sharing, as there is no Release signing configuration defined in the project yet.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///home/jordan-tsagmo/Bureau/PROJET/2026-2027/FREELANCE/LLR/eliteMove/gradle/libs.versions.toml)
- Update `kotlin` version to `2.1.0`.
- (Optional) Update other related dependencies if needed.

#### [MODIFY] [build.gradle.kts](file:///home/jordan-tsagmo/Bureau/PROJET/2026-2027/FREELANCE/LLR/eliteMove/app/build.gradle.kts)
- If Kotlin upgrade isn't enough, I will add a Java Toolchain configuration to force the use of Java 17 for the build.

## Execution Steps

1. **Update Kotlin Version**: Modify `libs.versions.toml` to use Kotlin `2.1.0`.
2. **Sync and Verify**: Run a Gradle sync and try to build the project.
3. **Handle Toolchain (if needed)**: If the error persists, configure the Java Toolchain in `app/build.gradle.kts` to use JDK 17.
4. **Build APK**: Execute `./gradlew :app:assembleDebug`.
5. **Export APK**: Copy the generated APK from `app/build/outputs/apk/debug/app-debug.apk` to `/home/jordan-tsagmo/Téléchargements/eliteMove.apk`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the build succeeds.

### Manual Verification
- Verify the existence of `eliteMove.apk` in the `/home/jordan-tsagmo/Téléchargements/` directory.
