# Xiaomi compatibility notes

Version 2.0 uses an ADB-started instrumentation session with session-scoped Bluetooth shell permissions. It reads existing HID notifications without writing remote characteristics. Report 4 supplies M1/M2 swipe direction; report 3 supplies W/T pinch direction. The Xiaomi report map describes IDs 5, 4 and 2, omitting report 3; the same remote worked natively on the tested Samsung.

M1 camera launch uses the secure-camera intent while locked. Remote wake signals are correlated with screen-on events; mode commands open Camera when M1 is pressed outside it. Gestures run only when Xiaomi Camera is focused. Battery optimization exemption is required. Activation after reboot is still necessary.

Validated by build/lint and user tests on Xiaomi 17 Ultra, model 2512BPNDAG, Android 17, HyperOS OS3.0.336.0.XPAEUXM: screen-off M1 camera launch, M1/M2 modes, W/T zoom. USB-disconnected operation was verified with the preceding compatibility-only build. No root, firmware flashing or protected system-file edits were used. Reconnection, endurance, and other camera layouts remain unverified.
