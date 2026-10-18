# Technical notes: no-ADB beta

## Architecture

Application ID: `org.telesin.camerawake.beta`; namespace: `org.telesin.buttonlab`. A user-enabled accessibility service observes screen transitions, launches the secure Camera while locked, and requests the public accessibility screen-lock action to correct rapid wake bounce. V5.9 plus a fresh Bluetooth pairing provides the ordinary controls through Android’s native HID handling.

The current manifest registers **no instrumentation**. There is no system UID, adopted privileged permission, `INJECT_EVENTS`, root requirement or startup script. Bluetooth diagnostics request ordinary `BLUETOOTH_CONNECT`; they are not the runtime control path. The app includes diagnostic/mapping tools retained from testing, but working native buttons require no learning.

## Wake/sleep sequence

Long M1 was observed to emit a Power pulse, a touchscreen swipe about 650 ms later, and a consumer key around 1400 ms later. Android handles Power in system policy before normal app dispatch. A normal app cannot reliably consume it through accessibility on this phone.

- Screen-off cancels pending automatic Camera launches and shows a temporary black accessibility overlay.
- A wake within **800 ms** of sleep is treated as bounce and immediately corrected with `GLOBAL_ACTION_LOCK_SCREEN`.
- A genuine wake shows a temporary black input guard, waits **1600 ms**, launches Camera directly, then removes the guard **250 ms** later.
- The guard absorbs touches and TELESIN key callbacks while present. It has a timeout, and cleanup runs on service destruction/cancellation. It can briefly block intentional input during transition.
- Normal native controls pass through outside the guard. A brief residual system transition remains possible; this is reactive handling, not a firmware-level fix.

Camera-on-wake responds to any screen wake, not just TELESIN. Disable it outside case use. The app does not unlock the phone or bypass the PIN. No boot receiver is needed for the tested accessibility activation model; first unlock after reboot was part of the verified Beta 3.1 test.

## Build and release

JDK 17, SDK 35, Gradle 8.9. Run `./gradlew assembleDebug lintDebug`. Beta 5: versionCode 35, versionName `3.0-beta5`. The prerelease APK is the same locally debug-signed binary installed for user testing; it is not Play-distributed or production-signed. A CI debug APK may have a different signing key. No signing key or local SDK path is committed.

## Historical implementation

The `scripts/` directory and [legacy technical notes](LEGACY-DETAILS.md) describe 2.1. Those scripts are not compatible activation steps for the current beta; use the tagged 2.1 release if needed. The old helper used privileged instrumentation to read protected HID reports and translate the Xiaomi’s old layout.
