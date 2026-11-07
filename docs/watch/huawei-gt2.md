# Huawei Watch GT 2: show and score a match on the watch

Status: **groundwork only.** The phone side is ready for a watch link (`WatchBridge`, protocol in
`shared/.../WatchProtocol.kt`, tested). The watch app and the Huawei Wear Engine link are not written
yet because they need a Huawei developer account and tools that cannot be used from the cloud sandbox.

## Why this is different from Wear OS
The GT 2 runs Huawei's LiteOS, not Wear OS, so Google's Wear libraries do not apply. Huawei offers
the **Wear Engine** SDK so an Android phone app can exchange messages with a watch app. Phone apps talk to
the **Huawei Health** app, which relays over Bluetooth to the watch.

Per Huawei's documentation, the GT 2 is listed for Wear Engine, but the sources disagree on firmware
minimums (one says lite wearable OS 11.0.14 or later for the GT 2e, 10.1 or later for the GT 2 Pro), and
phone Android support is given as 6.0 to 13 or 14. Check the current release notes and your watch's firmware
before building. Sources: Huawei's
[Wear Engine overview](https://developer.huawei.com/consumer/en/hms/huawei-wearengine) and
[service introduction](https://developer.huawei.com.cn/consumer/en/doc/development/connectivity-Guides/service-introduction-0000000000018585).

## What the phone sends and receives
- **Phone to watch:** a JSON `WatchSnapshot` (names, points, games, sets, tiebreak, over/winner, start time).
  The watch only displays it, so it needs no scoring rules.
- **Watch to phone:** one of the plain strings `POINT_A`, `POINT_B`, `UNDO`. The phone ignores anything else.

## What you need to provide
1. A **Huawei developer account** (free, with identity verification).
2. Register the phone app there with package name `com.tennisbuddy.android` and apply for **Wear Engine**
   permission (Huawei reviews this). It also asks for the app's signing certificate fingerprint.
3. **Huawei Health** installed and paired with the watch on the phone.
4. **DevEco Studio** on a computer, to build and install the watch app on the GT 2.
5. Your watch's firmware version (Huawei Health, device details).

## Steps once those exist
1. Add the Wear Engine SDK to `androidApp` and write `HuaweiWatchBridge : WatchBridge`. In
   `MatchViewModel`, replace `NoWatchBridge` with it.
2. Build the watch app: a score screen with two point buttons and undo that renders the snapshot and sends the commands.
3. Test with the watch paired. Expect to adjust for the GT 2's small screen and Huawei's review rules.

## Not yet verified
No part of the Huawei link has been built or run. Whether the GT 2 firmware currently allows
third-party apps in your region is also unconfirmed.
