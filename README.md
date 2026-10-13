# Camera Wake

TELESIN P4-WTP-06 camera controls for **Xiaomi 17 Ultra (2512BPNDAG), Android 17 / HyperOS OS3.0.336.0.XPAEUXM**.

- **M1:** open secure Camera from screen off; previous mode in Camera.
- **M2:** next mode.
- **W / T:** zoom out / in.
- **Capture:** native photo/video control.

All four controls and screen-off camera launch were confirmed on the test phone. The compatibility helper also worked with USB unplugged. No root or firmware changes.

## Use

1. Download and extract **Camera-Wake-2.0.zip** from [Releases](https://github.com/shon3l/camera-wake/releases/latest).
2. Pair **TELESIN_SDC**, connect USB debugging, then run `./start.sh` (set `ADB=/path/to/adb` if needed).
3. Open Camera Wake and allow unrestricted battery use. When Ready, unplug USB.

**Run start.sh again after reboot or stopping the helper.** Android requires this activation for direct HID access. Version 2 combines wake and compatibility in one app; Accessibility is no longer required. Reset to defaults and Stop are available in the app.

The fix translates reports Xiaomi mishandles; native cursor clicks may still appear. Other camera layouts and long-term background survival are unverified. Galaxy S22+ (SM-S906B), Android 16 worked natively and needs no helper.

<img src="docs/screenshots/camera-wake-main.png" alt="Camera Wake app" width="320">

## Build

JDK 17, Android SDK 35: `./gradlew assembleDebug lintDebug`.

MIT for code/logo; product photo has separate rights: [third-party notices](THIRD_PARTY_NOTICES.md).
