# Camera Wake

TELESIN P4-WTP-06 controls for **Galaxy S22+ (SM-S906B), Android 16** and **Xiaomi 17 Ultra (2512BPNDAG), Android 17 / HyperOS OS3.0.336.0.XPAEUXM**.

- **Long M1:** open Camera; hold again in Camera to turn the screen off. Allow about 3 seconds for the remote's gesture sequence to finish.
- **Short M1 / M2:** previous / next camera mode.
- **W / T:** zoom out / in. **Capture:** native photo/video control.

Samsung keeps its native controls; Xiaomi gets the mode/zoom compatibility fix. No root or firmware changes.

## Use

1. Extract **Camera-Wake-2.1.zip** from [Releases](https://github.com/shon3l/camera-wake/releases/latest).
2. Pair **TELESIN_SDC**, connect USB debugging, then run `./start-s22.sh` or `./start-xiaomi.sh` (`ADB=/path/to/adb` if needed).
3. Open Camera Wake and allow unrestricted battery use. When Ready, unplug USB.

**Activate again after reboot or stopping the helper.** Accessibility is not required. Reset to defaults and Stop are in the app. The remote connects to one phone at a time.

Screen-off camera launch and mode/zoom controls were user-tested on both phones with the preceding wake behavior. Xiaomi compatibility also worked with USB unplugged. The S22+ device log confirms the new long-M1 open / screen-off / open sequence; short controls after this change await user confirmation. Other camera layouts and long-term background survival are unverified.

<img src="docs/screenshots/camera-wake-main.png" alt="Camera Wake app, earlier Xiaomi screen" width="320">

## Build

JDK 17, Android SDK 35: `./gradlew assembleDebug lintDebug`.

MIT for code/logo; product photo has separate rights: [third-party notices](THIRD_PARTY_NOTICES.md).
