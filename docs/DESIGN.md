# Interface direction

The public interface keeps the realistic physical-button reference from 2.1. The home screen is fixed-height: header, status, proportionally fitted photo, wake switch, Open Camera, setup action and footer. It has no scrolling on the tested Xiaomi portrait display. Setup opens in a separate dialog; its longer instructions can scroll independently.

## Underwater theme (RC3)

- Original static navy/teal water-light background, softly blurred on a separate background-only layer. Text and controls are never blurred.
- Translucent blue panels, cyan accents, rounded pill actions and native ripple feedback.
- Cool display tint on the photo; the source image is retained unchanged. The outline follows the fitted image with 18 dp corners, preserving all button labels without center cropping.
- Setup status uses text plus color: green ✓ for directly checked active settings; amber ! for inactive or manual checks. Firmware confirmation is explicitly user-reported, and Xiaomi autostart is not claimed to be automatically verified.
- At least 48 dp primary/setup button height. White primary-button text has over 5:1 contrast against both gradient endpoints.
- Bright system-bar icons on the dark background. No moving wallpaper, network assets or animation loop.

This is an original native Views implementation, not an imported UI kit or a claim that Material Components/Compose are integrated. The only added rendering effect is Android’s background blur. Runtime wake/sleep code is unchanged from physically tested RC1.

## References reviewed

- [Material color roles](https://m3.material.io/styles/color/the-color-system): tonal surfaces and readable content/action hierarchy.
- [Material Android button guidance](https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md): action shapes and states.
- [Glass Ocean UI examples](https://zorc.se/en/ui-kits/glass-ocean): visual inspiration only; no assets or code copied.

The original product-image attribution remains in THIRD_PARTY_NOTICES.md. Local phone screenshots are not published, to avoid sharing notification/status information.
