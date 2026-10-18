# Historical Beta 5

Beta 5 (`org.telesin.camerawake.beta`, versionCode 35) is preserved at [v3.0.0-beta.5](https://github.com/shon3l/camera-wake/releases/tag/v3.0.0-beta.5). It contained Bluetooth diagnostics, key mapping, gesture injection and screen-reading capabilities for development. Those tools are removed from the polished 3.0 app. Its transition timing remains the baseline.

The polished app uses `org.telesin.camerawake` and a dedicated private release-signing key. It installs separately; enable its own Accessibility service and wake switch, then disable/remove beta after verifying the replacement. Settings do not migrate across these separate application IDs.
