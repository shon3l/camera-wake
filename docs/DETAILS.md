# Camera Wake: detailed reference

## Tested phone

**Xiaomi 17 Ultra**, model **2512BPNDAG**, running **Android 17 / HyperOS OS3.0.336.0.XPAEUXM**, with a **TELESIN P4-WTP-06 / TELESIN_SDC** controller. Tested on 12 October 2026.

The owner confirmed this sequence works:

- With the screen off, **hold M1** to wake the screen and open the secure camera.
- **Press M1 again** to turn the screen off.

Other phones, firmware versions and controllers have not been validated. The app does not unlock the phone; existing photos and other protected phone functions remain behind the lock screen.

## Download and install

Download **Camera-Wake-1.1.0.apk** from this repository’s **Releases** page. It is free, with no subscription, ads, accounts or in-app purchases.

1. Transfer the APK to the phone and open it. Android may ask you to allow installation from the app you used to open the file.
2. Open **Camera Wake**.
3. Pair the case as **TELESIN_SDC** in the phone’s Bluetooth settings.
4. Under **Listener & setup**, tap **Open Accessibility settings** and enable the **TELESIN Button Lab** listener. If HyperOS blocks it, open Camera Wake’s system app-info page and allow restricted settings before returning to Accessibility settings.
5. Disable Android battery optimization with the setup button; on HyperOS also select Battery saver → No restrictions. Both setup checks must pass.
6. Turn on **Enable M1 camera wake** on the main card.
6. Turn the screen off. Hold M1, then press M1 again to turn the screen off.

**No key learning is required for the working M1 shortcut.** Turn camera wake off when you finish using the case: while enabled, **any screen wake** can request the camera, including the phone’s Power button or a notification.

For ADB installation:

```sh
adb install -r Camera-Wake-1.1.0.apk
adb shell am start -n org.telesin.buttonlab/.MainActivity
```

## How it works

The controller sends a brief hardware Power-key pulse for its wake function. A regular app cannot intercept that as an ordinary held key or emulate a protected double-Power gesture. The enabled Accessibility service observes the screen waking and requests a lock-screen activity, which launches `com.android.camera.Camera` with Android’s secure-camera action when the keyguard is locked.

There is no root, firmware modification, password bypass or private-gallery access. HyperOS must permit the launch. The M1 turn-off behavior comes from the remote itself.

## Button guide and advanced features

A product-photo guide identifies Capture, M1, M2, W and T. The app includes:

- Separate short-press and long-press actions for ordinary learned external keys.
- Adjustable hold duration, pass-through, ignore, calibrated camera taps and swipes.
- Per-button resets for learned key, actions and tap position, plus reset all settings.
- Local camera-wake diagnostics and a camera-launch test.

Expand **Advanced button actions**, **Camera & calibration settings**, or **Diagnostics & resets** to use these tools. Some controller buttons send touch gestures or Power pulses rather than ordinary keys; those cannot be learned by the key listener. The hold-duration selector affects ordinary mapped keys, not the native M1 wake function. Picking a tap position preserves your selected actions.

## Troubleshooting

- **M1 wakes the password screen, but no camera:** confirm both the Accessibility listener and M1 camera-wake switch are enabled. Expand diagnostics and check Camera wake. In HyperOS app permissions, check whether showing on the lock screen or opening windows from the background is blocked.
- **A button never learns:** it may send a Power pulse or touch gesture. Use the M1 camera-wake mode without learning.
- **Camera opens on other wake events:** disable M1 camera wake outside case use.
- **Tap misses a control:** recalibrate in the same screen orientation and camera layout you will use.
- **Different phone:** compatibility is unverified; secure-camera handling and background launch restrictions vary.

Test on dry land before placing the phone in the case or using it underwater.

## Privacy and permissions

No Internet, camera, Bluetooth-scan, location or storage permission. All preferences and diagnostics stay on the phone. The Accessibility service can observe window state, filter external keys and inject calibrated camera gestures. It is used to operate the camera on your behalf. Disable the listener when you no longer need it.

## Build from source

Requires JDK 17, Android SDK Platform 35, and Android SDK Build-Tools 34 (the Android plugin’s default). Minimum supported Android version: Android 10 / API 29. Gradle 8.9 wrapper and Android Gradle Plugin 8.7.3 are included.

```sh
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the app and uploads the APK as a free workflow artifact on pushes and pull requests. Workflow-built debug APKs have a different signing key from the downloadable release; they may require uninstalling the release first, which clears settings. The release APK uses the same signing key as the original tested installation.

## Validation

- Gradle build and Android lint pass.
- The APK was installed and the UI inspected on the tested Xiaomi.
- The owner verified the M1 screen-off → camera → screen-off flow.
- Version 1.0.2 restores the original product-photo button guide while preserving the working camera behavior.

## License and attribution

Code and the custom camera-and-wave logo are licensed under **MIT**. The third-party product photograph is excluded from the MIT license; its rights remain with its owner. The Gradle wrapper retains its upstream license notices. TELESIN and Xiaomi names identify compatible hardware; this project is independent and is not endorsed by either company.

Product photo source: [Kareem Stores TELESIN P4-WTP-06 listing](https://kareemstores.com/en/telesin-smartphone-diving-case-with-remote-control-p4-wtp-06-1773945903). The photograph is used as a hardware button reference.
