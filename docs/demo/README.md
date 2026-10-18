# Privacy-safe video walkthrough

[Download/play the MP4](camera-wake-beta5-demo.mp4)

This 36-second silent animation is **illustrative, not a screen recording or evidence of a live test**. The Camera screen is synthetic. No phone footage, photographs, private identifiers, raw logs or audio were used. Reproduce it with `python3 scripts/render-demo.py` (Pillow, ffmpeg and DejaVu Sans required).

## Transcript

1. Camera Wake Beta 5: TELESIN P4-WTP-06 with Xiaomi 17 Ultra, Android 17 / HyperOS. No ADB activation.
2. Official ReefColor app: Diving Phone Case; tested firmware upgrade V5.7 → V5.9. Verify the installed version.
3. Forget only TELESIN_SDC, power-cycle the remote, pair again. Fresh pairing loads the new input layout.
4. Install beta; enable Accessibility, unrestricted battery/autostart and camera-on-wake.
5. Hold M1 to wake Camera; short M1/M2 switch modes, W/T zoom, Capture controls photo/video.
6. Hold M1 to sleep. Temporary transition guard absorbs the trailing remote gestures. No startup script during use. Disable wake mode after case use.

Firmware upgrade prompts and exact UI vary. This is not a frame-accurate recreation of ReefColor. Read [firmware guidance](../FIRMWARE.md) before updating. Reboot testing was completed on Beta 3.1, not repeated on Beta 5; see the [test matrix](../COMPATIBILITY.md).
