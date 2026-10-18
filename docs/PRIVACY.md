# Privacy

Camera Wake 3.0 has no account, advertising, tracking, telemetry or network permission. It does not request camera, microphone, media, storage, location or Bluetooth access. It opens the phone’s camera app; that app owns any photos/videos and its own permissions.

## Accessibility

Accessibility lets Camera Wake open secure Camera after screen wake, show a short black input guard and request screen lock after the remote’s rapid wake bounce. During the guard it filters TELESIN key callbacks to absorb trailing macro keys. Native camera controls otherwise pass through.

The public app **cannot retrieve screen content**, does not perform injected gestures and does not log key histories. Its accessibility event callback is empty. It only enumerates Android input-device names locally to show whether TELESIN is connected; it does not save device identifiers.

## Local settings

The wake switch, your manual firmware/pairing confirmation and most recent generic error message are stored in private app preferences. Backup is disabled. Nothing is uploaded or shared automatically. Uninstalling removes these settings. Source-code and firmware-guide buttons open the system browser, whose normal privacy policies apply.

The current public app has no beta diagnostics or Share-report feature. Historical beta releases had broader diagnostic capabilities; their own documentation applies to those builds.

## Demo and artwork

The included walkthrough is generated animation, not a phone recording. It contains no personal images, device identifiers, notifications, audio or logs. The product reference image is attributed in THIRD_PARTY_NOTICES.md and is not a user photo. Local phone screenshots used for visual review are not uploaded with this release.
