# Setup and upgrades

## Firmware first

Follow [the tested V5.7 → V5.9 upgrade](FIRMWARE.md), then forget and re-pair **TELESIN_SDC**. Confirm M1/M2/W/T/Capture work natively in Xiaomi Camera before configuring wake behavior.

## Install Camera Wake 3.0

1. Download the signed release-candidate APK from [GitHub Releases](https://github.com/shon3l/camera-wake/releases/tag/v3.0.0-rc2). Android may ask to allow installations from your browser/file manager.
2. Open **Camera Wake** → **Setup & firmware** → **Enable Accessibility**. Read the explanation, then enable **Camera Wake** in Android’s accessibility services.
3. If Android blocks a sideloaded service, open Settings → Apps → Camera Wake → menu → **Allow restricted settings**, then return to Accessibility. These are user-consented settings, not ADB activation.
4. Allow **unrestricted battery use**. On Xiaomi enable autostart/background activity/pop-up permissions if offered. The button opens app settings; labels differ by OS.
5. Turn on **Wake with M1**. The status shows whether the service and remote input device are connected. No key learning is needed.
6. Open Camera and test all five controls. Lock the phone; hold M1 to wake Camera, release and wait about two seconds. Hold again to sleep.

After reboot, unlock once with the PIN and let the remote reconnect. No computer or startup script is part of use. Pre-first-unlock operation is unverified. See the [test matrix](COMPATIBILITY.md) for exactly which build passed reboot testing.

## Replace beta or 2.1

- Public app: `org.telesin.camerawake`, label **Camera Wake**.
- Beta: `org.telesin.camerawake.beta`, label **Camera Wake Beta**.
- Legacy 2.1: `org.telesin.buttonlab`, also labelled **Camera Wake**.

The public app uses a dedicated release key and installs separately. Settings do not migrate. Disable Beta’s accessibility service and stop/uninstall the legacy helper before testing the new service. Enable the public app’s own service and **Wake with M1** switch. Once verified, uninstall the two old apps. Keep ReefColor if you want future firmware updates; it is not needed during everyday camera control.

## Pause or remove

Turn **Wake with M1** off to stop automatic camera opening and transition interception immediately. Native remote controls still work. To disable the service entirely, use Android Accessibility settings; uninstalling also removes its private settings. It is not a device administrator.

## Troubleshooting

- Native buttons fail after update: forget and re-pair only TELESIN_SDC; do not clear all Bluetooth data.
- Wake fails: check that **the new app’s** service is enabled, not just Beta; then check its switch and background restrictions.
- Phone Power opens Camera: expected while wake mode is on. Switch off after case use.
- Very fast sleep/wake is ignored: wakes within 800 ms are treated as remote bounce.
- Short black transition: expected; it absorbs the remote’s trailing swipe/key sequence.
- Camera or overlay error: an actionable message appears in the app. A timed guard is removed even if launch fails.
- Native controls work without wake mode or Accessibility: expected; V5.9 supplies those through Android HID.
