# Camera Wake

Use a TELESIN P4-WTP-06 diving case remote to wake Xiaomi Camera, switch modes, zoom and capture — **without ADB activation or root**.

## Recommended Xiaomi beta

[Download Camera Wake 3.0 Beta 5](https://github.com/shon3l/camera-wake/releases/tag/v3.0.0-beta.5). Tested on **Xiaomi 17 Ultra, Android 17, HyperOS OS3.0.336.0.XPAEUXM**, with remote firmware **V5.9**.

1. Update the remote **V5.7 → V5.9** using the official ReefColor app. This is the exact tested upgrade; other starting versions are unverified. [Firmware instructions](docs/FIRMWARE.md).
2. **Forget TELESIN_SDC and pair it again** in Android Bluetooth settings after updating. Android otherwise retains the old input description.
3. Install the beta APK. It is a separate app from Camera Wake 2.1. Stop the old helper and disable any other Camera Wake accessibility service before testing. Uninstall the old app if you no longer need it.
4. Enable **Camera Wake Beta** in Accessibility, allow unrestricted battery use, and allow Xiaomi autostart/background activity as offered. Enable **camera on screen wake** in the app. [Full setup](docs/ACTIVATION.md).
5. No button learning or script is needed. Hold M1 to open Camera; hold again to sleep. Short M1/M2 switch modes, W/T zoom, Capture takes a photo or controls video.

The app briefly covers wake/sleep transitions with a black input guard to absorb the remote’s built-in gesture sequence. Allow about two seconds before the next control. Camera-on-wake also reacts to the phone Power button and other screen wakes; turn it off after case use.

## What is verified

All five regular controls and long-M1 wake/sleep were confirmed on Beta 5. Reboot, first PIN unlock and USB-disconnected operation without launching the app or running a script were confirmed on Beta 3.1. Beta 5 keeps that activation model but **has not repeated the reboot test**. This is a device-tested beta, not a universal compatibility claim. [Test matrix and limitations](docs/COMPATIBILITY.md).

## Video walkthrough

[![Illustrative demo — no phone footage](docs/demo/poster.png)](docs/demo/camera-wake-beta5-demo.mp4)

[Watch/download the privacy-safe animated walkthrough](docs/demo/camera-wake-beta5-demo.mp4) · [Transcript and provenance](docs/demo/README.md).

The video is an illustration of setup and tested controls, **not footage of a phone or a live test**. It contains no camera preview, gallery, notifications, device identifiers or user recordings.

## Older firmware and Samsung

[Camera Wake 2.1](https://github.com/shon3l/camera-wake/releases/tag/v2.1.0) remains available with its ADB-started helper. Samsung Galaxy S22+ was tested on that release; the V5.9/Beta 5 combination has not been tested on Samsung. [Legacy activation](docs/LEGACY-ACTIVATION.md).

## Build and development

JDK 17 and Android SDK 35: `./gradlew assembleDebug lintDebug`. The beta uses application ID `org.telesin.camerawake.beta`; the old app uses `org.telesin.buttonlab`. CI builds are debug-signed and may need an uninstall before replacing a build signed by a different key. Release APK signing details and known limitations are in [technical notes](docs/DETAILS.md).

[Changelog](CHANGELOG.md) · [Privacy](docs/PRIVACY.md) · [Third-party notices](THIRD_PARTY_NOTICES.md). Code and generated video artwork: MIT. Unaffiliated with TELESIN or Xiaomi.
