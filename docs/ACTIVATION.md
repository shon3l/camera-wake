# Camera Wake activation research — 14 October 2026

Target: Xiaomi 17 Ultra (2512BPNDAG), Android 17 / HyperOS OS3.0.336.0.XPAEUXM. No root or bootloader changes.

## Offline USB activation

Bugjaeger is installed on the Galaxy S22+ (SM-S906B, Android 16). A saved command named **Start Camera Wake** restarts the helper on the selected USB target:

```sh
am force-stop org.telesin.buttonlab; nohup am instrument -w org.telesin.buttonlab/.CompatibilityInstrumentation > /data/local/tmp/camera-wake-combined.log 2>&1 < /dev/null &
```

Connect Samsung as USB host to Xiaomi, approve USB access on Samsung and debugging authorization on Xiaomi, select Xiaomi in Bugjaeger, and run the command. Pair TELESIN_SDC to Xiaomi and confirm Ready in Camera Wake before unplugging. Samsung's Wi-Fi was enabled only for installation and then disabled. Xiaomi Wi-Fi was also disabled for the offline USB test. **The user confirmed phone-to-phone activation worked with Wi-Fi off.**

Bugjaeger's official connection guidance covers host/target authorization, USB roles and cable compatibility: https://sisik.eu/bugjaeger_faq

## Why a boot receiver alone is insufficient

The existing helper uses an instrumentation session to adopt Bluetooth shell permissions. Android restricts HID GATT characteristics to BLUETOOTH_PRIVILEGED. A normal app's Bluetooth permission, battery exemption, autostart setting or Accessibility access does not replace this permission.

Primary source: https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/heads/main/src/com/android/bluetooth/gatt/GattService.java

## Shizuku and experimental offline startup

Normal Shizuku wireless startup can avoid a computer, but needs reactivation after reboot and a network for Android wireless debugging. Another phone's hotspot need not provide Internet, but it still creates a Wi-Fi connection and does not satisfy the user's requirement to activate with no Wi-Fi.

Official guide: https://shizuku.rikka.app/guide/setup/

Shizuku-Next advertises experimental no-Wi-Fi startup. Its source at commit 51e3b1665dc81e0daf8c65dba218ff2c7c6747db documents a repeated-settings-write workaround and a temporary local-only hotspot. Its Android 17 transport code says the wireless server is refused without a wireless network and requires an already listening classic ADB port instead. This is a source finding, not a successful Xiaomi test of that fork.

- https://github.com/rushiranpise/Shizuku-Next/blob/51e3b1665dc81e0daf8c65dba218ff2c7c6747db/manager/src/main/java/moe/shizuku/manager/start/ForcedWirelessDebugging.kt
- https://github.com/rushiranpise/Shizuku-Next/blob/51e3b1665dc81e0daf8c65dba218ff2c7c6747db/manager/src/main/java/moe/shizuku/manager/start/StartTransport.kt

A one-off offline write to adb_wifi_enabled on Xiaomi read back as 1; that setting value alone does not prove a working listening port. The original wireless-debugging setting and Wi-Fi state were restored afterwards, before the separate offline USB test was prepared. No successful wireless activation was obtained.

## Persistent port workaround

Classic `adb tcpip` startup can allow local reconnects during the same boot, but its service property does not survive reboot. Shizuku-Next's persistent-port feature requires writing persist.adb.tcp.port, normally limited to root/system identity.

A safe test writing the disabled value 0 on this Xiaomi failed with **Failed to set property 'persist.adb.tcp.port' to '0'**. No persistent port was enabled or changed. This rules out that specific persistence workaround through the available shell identity on this phone.

Primary implementation: https://github.com/rushiranpise/Shizuku-Next/blob/51e3b1665dc81e0daf8c65dba218ff2c7c6747db/manager/src/main/java/moe/shizuku/manager/start/AdbPortPersistence.kt

## Current conclusion

USB activation from the S22+ is confirmed working offline. No confirmed Xiaomi-only method restores the full HID helper after reboot with no Wi-Fi, no external USB host and no root. Experimental claims should not be treated as working until demonstrated on this specific Android 17 build.

## Termux, Docker and virtual machines

Termux provides an `android-tools` package, including ADB. It runs as a normal Android app; running a shell command directly does not turn it into Android's privileged debugging shell. Connecting its ADB client to an authorized local ADB server can supply that shell identity, but the server still needs a reachable port. Termux:Boot can automate commands but cannot make that unavailable port appear after reboot.

Primary sources:
- https://github.com/termux/termux-packages/blob/master/packages/android-tools/build.sh
- https://github.com/termux/termux-packages/wiki/Termux-execution-environment

### Direct test on Xiaomi

The APK is debuggable, allowing a test using its own app UID 10586 rather than shell UID 2000:

```sh
run-as org.telesin.buttonlab /system/bin/am instrument --user 0 -w org.telesin.buttonlab/.CompatibilityInstrumentation
```

Android rejected it: **not allowed because it's not started from SHELL**. No helper restart occurred. The explicit user 0 avoids a separate cross-user check. This reproduces the important permission boundary a normal terminal app would face, without installing Termux.

Android's implementation restricts instrumentation startup on production builds to shell/root/system or already-active instrumentation:
https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/am/ActivityManagerService.java

### Containers and emulators

PRoot provides a Linux userland and simulates root; its project explicitly states that it has no real root and cannot perform operations needing kernel privileges. Rootless Docker runs inside user namespaces and requires host UID-mapping support; it does not grant Android shell identity or Bluetooth privileges. A VM can give a guest its own administrator account, while host hardware access remains mediated by the host. Android's Virtualization Framework is designed for stronger isolation, not host privilege elevation.

- https://github.com/termux/proot-distro#limitations
- https://docs.docker.com/engine/security/rootless/
- https://source.android.com/docs/core/virtualization

The Xiaomi exposes no /dev/kvm device in the shell view inspected. That observation alone does not rule out every virtualization implementation, but virtualization would not solve the host permission problem even if a guest ran successfully. No container or VM was installed, and no root method was used.

### What Termux could improve

After USB activation opens a classic ADB port once during a boot, a Termux ADB client can potentially reconnect locally without an external Wi-Fi network. This is useful for restarting a stopped helper during that same boot. It does not survive a real reboot unless the host port is re-enabled. This additional setup has not been tested or applied here; the phone's classic ADB port was empty when inspected.

The confirmed solution for an offline reboot remains the S22+ USB activation. There is currently no demonstrated standalone Xiaomi offline boot solution on this build.
