# EutherTime

EutherTime is an original, offline-first Android clock for GrapheneOS and standard Android. It combines dependable native alarms with a neon cyberpunk interface and a dedicated **Egg Protocol**.

This project is implemented from scratch. It does not contain source code, visual assets, strings, or layouts copied from AOSP Clock or third-party clock applications.

## Current prototype

- Local clock with next-signal status
- Exact one-shot alarms through Android `AlarmManager.setAlarmClock`
- Quick countdown timers
- Dedicated Egg Protocol with consistency, size, and starting-temperature controls
- Stopwatch with lap markers
- Original low-volume interface signals with a persistent mute switch and automatic silent/vibrate suppression
- Alarm sound, vibration, full-screen alarm activity, dismiss and five-minute snooze
- Silent heads-up notice 30 minutes before alarms, with a one-tap disarm action
- Compact next-signal control in the app and persistent Snooze/Dismiss handling while an alarm is ringing
- Re-scheduling after boot, time changes, and timezone changes
- Direct-boot-aware alarm storage
- No network permission, analytics, accounts, ads, or telemetry

## Build

Requires Android SDK 36 and Java 17.

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Important test cases

An alarm clock is only trustworthy after device testing. Before relying on EutherTime, verify alarms while the app is closed, while the screen is locked, after reboot, before first unlock, with battery saver enabled, and after notification/full-screen permissions have been changed.

## License

Apache License 2.0. EutherTime branding and original visual assets remain trademarks/assets of ApothicTECH unless stated otherwise.
