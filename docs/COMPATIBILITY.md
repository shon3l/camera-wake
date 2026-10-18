# Compatibility and verification

| Configuration | Result |
| --- | --- |
| Xiaomi 17 Ultra / Android 17 / HyperOS OS3.0.336.0.XPAEUXM; TELESIN P4-WTP-06; firmware V5.7 | Previous ADB-based 2.1 helper worked; native Xiaomi mode/zoom compatibility was the original blocker. |
| Same Xiaomi; V5.9, old pairing retained | Controls were misinterpreted; updating alone was insufficient. |
| Same Xiaomi; V5.9, fresh Bluetooth pairing | Native M1, M2, W, T and Capture confirmed with old helpers stopped. |
| Beta 3.1, V5.9, fresh pairing | Long M1 wake/sleep and all controls confirmed after reboot, first PIN unlock, USB unplugged, no script or app reopening. |
| Beta 4 | Controls and improved transitions confirmed; trailing M1 gesture could briefly open a Camera option. |
| Beta 5 | Long M1 wake/sleep with black input guard confirmed; user reported improved/faster transition. All regular controls were confirmed during Beta 5 testing. Reboot not repeated on this version. |
| Galaxy S22+ / Android 16 | Legacy 2.1 tested. V5.9 plus Beta 5 not tested. |
| Polished 3.0 RC1 (`org.telesin.camerawake`) | Release build/lint and timing regression checks pass. User confirmed all controls and wake/sleep with the new service enabled and Beta disabled. Reboot test on the public package remains pending. |
| Public RC2 visual refinement | UI-only changes on RC1 runtime; debug/release build and lint pass. Public-package reboot/USB-unplugged test pending. |
| Other phones, remotes, firmware, camera apps | Unverified. |

Tests used the stock Xiaomi Camera. This does not establish behavior in every camera mode/orientation or long-term background survival. Before-first-unlock operation, interrupted firmware updates and firmware rollback are untested. The black transition guard is a workaround for a remote macro, not prevention of system Power events.
