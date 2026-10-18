> Historical Camera Wake 2.1 documentation. For the no-ADB beta, see [current setup](LEGACY-ACTIVATION.md).

# Compatibility notes

An ADB-started instrumentation session reads existing HID notifications using session-scoped Bluetooth permissions, without writing remote characteristics. Battery exemption is required; activation must be repeated after reboot or stopping the app.

Xiaomi reports M1/M2 in report 4 and W/T in report 3, although its report map omits report 3. The helper translates these into Camera gestures. Samsung describes touchscreen report 3 correctly and retains native mode/zoom controls.

Long M1 sends a Consumer Power report (Samsung 0100; Xiaomi 3000). The helper remembers the screen state around that report and opens secure Camera when locked, or sends KEYCODE_SLEEP when Camera was active. It waits three seconds for the remote's native power/gesture macro to finish before enforcing the chosen state. Short mode gestures do not launch Camera.

Test phones: Xiaomi 17 Ultra 2512BPNDAG, Android 17, HyperOS OS3.0.336.0.XPAEUXM; Galaxy S22+ SM-S906B, Android 16. Both passed earlier camera-wake and mode/zoom user tests; the user confirmed the S22+ long-M1 camera launch / screen-off toggle and short M1/M2 and W/T controls. The native remote macro briefly shows screen transitions before the final action. A visible unlock screen does not establish that keyguard authentication was bypassed. USB-disconnected compatibility was confirmed on Xiaomi. Reconnection, endurance, and other camera layouts remain unverified.
