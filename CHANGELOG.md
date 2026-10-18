# Changelog

## 3.0 Beta 5 — 2026-10-18

- New separate beta app using public Android APIs without ADB activation.
- Document tested remote firmware upgrade V5.7 → V5.9 and required Bluetooth forget/re-pair step.
- Native mode, zoom and capture controls work on the tested Xiaomi after fresh pairing.
- User-enabled accessibility handles screen-wake Camera launch and sleep correction.
- Temporary black transition guard absorbs the remote’s trailing swipe/key sequence.
- Retain optional diagnostics and mapping tools; normal V5.9 controls need no learning.
- Add compatibility/test limits, privacy notes and generated video walkthrough.

Beta 3.1 passed reboot/USB-disconnected testing; Beta 5 passed physical control and transition tests but has not repeated reboot testing. Samsung support on this new path is unverified.

## 2.1 and earlier

Legacy ADB-activated instrumentation releases remain available as tags/releases. Their original activation and compatibility documentation is preserved under `docs/LEGACY-*.md`.
