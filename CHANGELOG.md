# Changelog

## 3.0.0 — public release

- Free signed public release with the underwater single-screen interface and realistic rounded physical-button guide.
- Beta 5 wake/sleep behavior retained; diagnostic tools and broad permissions removed.
- Live setup status indicators and explicitly manual firmware confirmation.
- Public-package controls/wake/sleep confirmed with Beta disabled. Reboot acceptance on the public package was not repeated; earlier Beta 3.1 reboot testing remains the established baseline.
- App-only screenshots replace the illustrative video; video removed from current repository and release assets.


## 3.0.0-rc3 — underwater, one-screen layout

- Navy/teal water-light backdrop with background-only blur and translucent blue panels.
- Main page fits the tested Xiaomi portrait screen without scrolling; setup opens separately.
- Full physical-button photo with cool display tint and fitted rounded corners; no center crop.
- Setup shows live labelled color states for service, battery and TELESIN connection; firmware confirmation is explicitly manual.
- Runtime unchanged from physically tested RC1; public-package reboot acceptance still pending.


## 3.0.0-rc2 — visual refinement

- Ocean-toned background and surfaces, pill buttons with pressed ripple states, camera icon, status chip and stronger switch states.
- Preserve the full-size physical-button photo and compact setup layout.
- Runtime unchanged from RC1.

## 3.0.0-rc1 — public release candidate

- Compact light interface with the original realistic physical-button reference, larger photo and expandable setup.
- Dedicated public package and private release signing key.
- Retain Beta 5 wake/sleep timings; remove all mapping and Bluetooth diagnostics.
- Remove screen-content retrieval, gesture injection and Bluetooth permission.
- Pause immediately cancels pending transitions; clearer setup status and generic errors.
- Add wake-policy regression checks, public upgrade/privacy/release documentation.
- User confirmed the new public service works with Beta disabled; reboot acceptance remains pending.


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
