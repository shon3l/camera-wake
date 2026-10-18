# Beta setup without ADB activation

First complete [V5.7 → V5.9 firmware setup and fresh pairing](FIRMWARE.md). The tested phone is the Xiaomi 17 Ultra; see [compatibility](COMPATIBILITY.md).

1. Install the Beta 5 APK from the tagged GitHub prerelease. Existing beta installations with the same signing key keep settings. It installs separately from Camera Wake 2.1.
2. Stop the old 2.1 helper using its Stop control. Do not run `start.sh`, `start-xiaomi.sh` or `start-s22.sh` for this beta. Uninstall 2.1 if it is no longer needed; do not leave multiple Camera Wake services enabled.
3. Open Camera Wake Beta → Enable Accessibility. Enable **Camera Wake Beta — remote controls**. If Android blocks a sideloaded service, use Settings → Apps → Camera Wake Beta → menu → Allow restricted settings, then return to Accessibility.
4. Allow unrestricted battery use. On Xiaomi allow autostart and background pop-up/activity permissions if offered. Labels vary by OS build.
5. Enable **camera on screen wake**. No learning or mapping is needed with V5.9’s native controls. Diagnostic and mapping controls remain available in this experimental beta; avoid learning native working buttons.
6. Open Camera and test M1/M2/W/T/Capture. From screen-off hold M1 to wake Camera, release and wait for the transition; hold again to sleep.

After reboot, unlock once with the PIN and let the remote reconnect. The user confirmed no script/app reopening was required on Beta 3.1; repeat that check when testing Beta 5. Operation before the first unlock is unverified. The remote connects to one phone at a time.

## Troubleshooting

- **Ordinary controls fail after firmware update:** forget and re-pair TELESIN_SDC. Do not clear all Bluetooth data or other pairings.
- **Native controls work but wake fails:** check the wake switch, accessibility connection and Xiaomi background restrictions. Open the beta once after installation.
- **Brief Camera flash or unexpected mode during transition:** ensure Beta 5 is installed; its temporary guard covers the remote’s trailing gestures. Allow roughly two seconds between actions.
- **Camera opens on phone Power or another wake:** expected while camera-on-wake is enabled. Disable it after using the case.
- **Intentional very quick sleep/wake is ignored:** wakes within 800 ms are classified as bounce. Wait before waking again.
- **Need the original setup:** use the separate [2.1 release and legacy instructions](LEGACY-ACTIVATION.md). Do not treat that helper as Beta 5 activation.
