# Interface direction

The public interface preserves the realistic physical-button reference from 2.1 while using a compact, photo-led hierarchy: status, photo, wake switch, Open Camera, expandable setup. A tonal ocean background and quiet surface borders support the diving-case context. Primary and secondary pill actions have explicit ripple feedback, a 48 dp minimum touch height and readable text. System bars use dark icons on light surfaces. The photo keeps its original proportions and labels; it is not cropped.

References reviewed for the RC2 visual pass:

- [Material Design color roles](https://m3.material.io/styles/color/the-color-system): tonal surfaces, on-surface text and action hierarchy.
- [Material Android button guidance](https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md): shape and action-state guidance.

This is a lightweight native Views implementation inspired by those principles, not a claim that Material Components or Compose libraries are integrated. No new network or analytics dependency is introduced. The original image attribution remains in THIRD_PARTY_NOTICES.md.
