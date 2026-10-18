# 3.0 implementation and limitations

The public app uses application ID `org.telesin.camerawake`. Ordinary mode/zoom/capture commands travel directly from firmware V5.9 through Android’s HID handling. The app has no Bluetooth runtime permission, GATT client, instrumentation, root path or input-injection permission.

A user-enabled `WakeService` listens for screen on/off. It retains Beta 5’s measured timing:

- Screen-off cancels a pending launch and displays a temporary black accessibility overlay, timed out after 1600 ms.
- Screen-on within 800 ms of sleep is treated as bounce and corrected immediately with the public accessibility lock action.
- Genuine screen-on displays the input guard, waits 1600 ms, opens secure Camera if locked, then removes the guard after another 250 ms.
- Guard fallback timeout is 2200 ms. Pause, interrupt and service destruction cancel pending work and remove the guard. A window-add failure cannot leave an indefinite blocking overlay.
- While guarded, only TELESIN key callbacks are consumed; other keys pass through. The full-screen overlay briefly absorbs touch. Normal native controls pass outside transitions.

The recorded long-M1 sequence was a Power pulse, a swipe around 650 ms later and another consumer key around 1400 ms later. Power is handled by Android system policy; the app reacts to its screen transition, rather than preventing the wake. A perfectly flash-free result is not guaranteed on every OS.

## Reduced permission surface

Accessibility cannot retrieve window content and has no gesture-injection capability. Its only optional capability is key filtering for the short guard. `onAccessibilityEvent` does not inspect events. There are no remote key logs, mapping tools, pointer diagnostics, vendor Bluetooth probes, camera/microphone/storage/location/Internet permissions, analytics SDKs or ads.

The normal Android camera app owns capture and storage. Camera Wake never unlocks the phone. On the tested Xiaomi, the main Camera component is explicit to avoid a secure-camera chooser. Other manufacturers use the standard intent and are unverified.

## Boundaries

Wake mode reacts to **all** screen wakes, not exclusively remote Power. It can open Camera on phone Power or another wake source and can treat an intentional sub-800-ms wake as bounce. Turn it off after case use. The app is not an underwater safety device. Firmware rollback, interrupted update recovery, other camera layouts and long-term background reliability have not been established.

## Regression checks

`python3 scripts/test-policy.py` validates the recorded bounce timings, 800-ms boundary, disabled behavior, reset, fresh service start and clock boundaries. Android build/lint run for debug and release in CI. Physical wake/sleep and reboot behavior must also be tested; a pure timing test cannot establish OS/service behavior.

Legacy documentation and tags remain separate; activation scripts in the current tree intentionally refuse to start the removed instrumentation helper.
