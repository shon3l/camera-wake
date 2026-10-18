# Remote firmware: tested V5.7 → V5.9 upgrade

This concerns the **TELESIN P4-WTP-06 Diving Phone Case remote**, Bluetooth name **TELESIN_SDC**. It is not a phone firmware update. Only **V5.7 → V5.9** was tested here. Do not assume a different TELESIN product uses the same firmware.

## Official updater

1. Install the official [ReefColor Android app from Google Play](https://play.google.com/store/apps/details?id=com.telesin.reefcolor), package `com.telesin.reefcolor`, developer Telesin R&D.
2. Power on the remote near the phone, connect it in ReefColor and open its device details. Our remote was identified as **Diving Phone Case**, current version **v5.7**.
3. Open the firmware update offered for that device. Our review screen offered **V5.9**. Confirm the product and versions before proceeding. If the app offers a different version, it is outside this test record; use the manufacturer’s guidance.
4. Keep both devices powered and nearby throughout the update. Do not disconnect or interrupt it. Follow the updater’s prompts.
5. Return to device details and verify **v5.9** is installed.
6. In Android Settings → Bluetooth → TELESIN_SDC → device settings, choose **Unpair/Forget**. Power the remote off and on, then pair it again in Bluetooth settings. Accept the pairing prompt.
7. Test short M1, M2, W, T and Capture in Xiaomi Camera before enabling app wake behavior.

**Re-pairing is essential in the tested case.** With the old bond, Android continued to interpret the previous HID layout and controls failed. A fresh pairing loaded the new touchscreen and consumer-control layout; the user then confirmed all ordinary controls worked with the instrumentation helper stopped.

## What the update fixes: evidence versus release notes

ReefColor’s official update notice only said that older firmware may affect normal use and recommended updating. It did **not** provide a detailed V5.9 changelog or promise a Xiaomi fix. We did not find a public detailed firmware changelog.

Our measurements showed the HID Report Map changed from 150 to 141 bytes. The new map includes a touchscreen report and a different consumer-control bitfield. Before re-pairing, Android still exposed mouse-style input; after re-pairing it exposed touchscreen input and proper zoom keys. These are project observations, not manufacturer release notes.

Firmware updates change the remote itself. Rollback availability is unknown; no rollback or unofficial firmware is provided here. We do not redistribute TELESIN firmware or the ReefColor APK.
