# Signing and release process

## Public identity

Public package: `org.telesin.camerawake`. Beta and legacy packages are separate. Public release APKs are signed with a dedicated RSA release key, not the Android debug key. Future public updates must keep this application ID and signing certificate, with increasing versionCode.

Release builds use these environment variables: `CAMERA_WAKE_KEYSTORE`, `CAMERA_WAKE_STORE_PASSWORD`, `CAMERA_WAKE_KEY_ALIAS`, `CAMERA_WAKE_KEY_PASSWORD`. They are never stored in the repository. Without them Gradle produces an unsigned release for CI validation; **do not publish that APK**. CI debug artifacts are for development, not public updates.

The maintainer’s private key and credentials are kept outside the checkout in a permission-restricted local signing directory. Back them up securely and offline before relying on long-term release continuity. Never upload them as release assets or attach them to issues.

## Checks

1. `python3 scripts/test-policy.py`.
2. `./gradlew assembleDebug assembleRelease lintDebug lintRelease` using JDK 17 / SDK 35 and release signing environment.
3. Verify the APK signature using Android `apksigner verify --verbose --print-certs`; record the public certificate fingerprint and SHA-256 asset checksums.
4. Inspect the merged manifest: no debuggable release flag, no instrumentation, no window-content retrieval, no gestures, no Internet/recording/Bluetooth permissions.
5. Install the exact signed APK, test user-consented setup, switch-off cancellation, M1 wake/sleep, every native button and status. Reboot, first PIN unlock, disconnect USB, and repeat without script/app reopening.
6. Record results per build in COMPATIBILITY.md. Use a prerelease tag until physical acceptance is complete. Do not imply Samsung or other-device support without testing.
7. Publish APK, checksums and accurate notes; preserve older releases. Ensure main README matches the released build, and include artwork attribution.

The product photo is not MIT-licensed; preserve its notice and obtain permission or replace it for distribution channels requiring independently verified artwork rights. No Play Store publication or policy approval is claimed by a GitHub release.

## Public signing certificate

SHA-256 certificate fingerprint for the public release key:

`4873a7b6e68ebc7a57e4378f4340eab5eae91b092761b058eedfdd85ad996842`

This is public verification information, not the private key or an APK checksum.
