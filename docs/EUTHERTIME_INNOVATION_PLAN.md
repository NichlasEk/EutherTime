# EutherTime Innovation Plan

## Product direction

EutherTime should become an offline-first alarm system built around how people actually wake up: several related signals, uncertain half-awake decisions, and a need to trust that tomorrow's alarm will still work. The app remains original, network-free by default, Direct Boot capable, and compatible with Android's exact alarm and lock-screen alarm model.

## Guiding rules

- A normal alarm must never depend on cloud access, an account, Health Connect, NFC, QR, calendar access, or a widget.
- Optional features must fail open: the exact alarm still rings if an accessory feature is unavailable.
- “Dismiss” skips the current occurrence; “Delete” removes the schedule.
- Destructive morning actions must say exactly what they affect.
- Saved alarms and existing 0.1.x data must migrate without being erased.
- Every release must pass unit tests, `assembleDebug`, and `lintDebug`, followed by a real APK metadata/hash check.

## Cyberpunk signal language

Every new surface should feel like the same instrument rather than a collection of Android dialogs.

- Visual hierarchy: toxic green means ready/confirmed, amber means pending/attention, magenta means destructive or exceptional, and ice text carries neutral data.
- Motion: short scan-line reveals, restrained signal pulses, and state transitions that remain readable with animations disabled.
- Copy: compact operational language such as `SIGNAL ARMED`, `MORNING LINK`, `FINAL STAGE`, and `INTEGRITY NOMINAL`; clarity wins over lore when an action changes alarms.
- Haptics: distinct signatures for gentle, primary, final, confirmed-awake, and integrity-failure states.
- Accessibility: high contrast, large sleepy-morning hit targets, no meaning communicated only by color, and a reduced-motion mode.

### Original alarm audio

- Produce an original EutherTime signal family with the local ACE-Step 1.5 runtime under `/home/nichlas/ai/eutherstudio`.
- Generate separate gentle, primary, final, integrity-test, and awake-confirmed motifs from one shared cyberpunk sonic identity.
- Prefer instrumental, loop-safe masters without speech or borrowed melodies.
- Keep prompts, generation parameters, source WAV masters, edit notes, loudness measurements, and hashes in a reproducible asset manifest.
- Trim and normalize application copies deliberately; validate loop seams, clipping, silence, and actual encoded file type with `ffprobe`/`ffmpeg`.
- Never make generated audio a delivery dependency: Android's selected/default alarm tone remains the fallback.

## Phase 1 — Wake Sets

Turn the current proximity-based wake grouping into an explicit model.

- A Wake Set has a title, weekdays, and two or more independently timed stages.
- Stages have roles: `GENTLE`, `PRIMARY`, and `FINAL`.
- The editor can add, remove, reorder, rename, and retime stages.
- Existing standalone alarms remain supported and can be converted into a set.
- Ringing controls:
  - `SNOOZE` affects only the ringing stage.
  - `DISMISS` skips only the ringing stage.
  - `NEXT SIGNAL` ends the current stage and keeps later stages.
  - `I'M UP · CLEAR SET` skips the remaining occurrences in the current set, not future weeks.
- Pre-alarm controls offer `THIS ALARM`, `SKIP NEXT`, and `CLEAR WAKE SET` where applicable.
- `0.4.0-beta4` keeps the next signal actionable on the clock screen, promotes the silent pre-alarm through a fresh public lock-screen channel, and restores the active ringing surface whenever the app is opened before Snooze/Dismiss.
- A one-tap `SKIP TOMORROW` action advances only the next occurrence while retaining its weekly schedule.

## Phase 2 — Progressive wake and safety check

- `GENTLE`: vibration-first and reduced alarm volume.
- `PRIMARY`: normal alarm signal and vibration.
- `FINAL`: full alarm volume, strong vibration, and lock-screen emphasis.
- `0.4.0-beta2` adds selectable `SYSTEM`, `NEON DAWN`, `PULSE GRID`, and `RED SHIFT` profiles to standalone alarms and Morning Links.
- Generated profiles use compressed 48 kHz Ogg/Vorbis application copies and keep the Android system alarm as playback fallback.
- Every stage ramps smoothly rather than jumping to a fixed level: GENTLE 8–32% over 60 seconds, PRIMARY 22–72% over 45 seconds, and FINAL 50–100% over 25 seconds.
- The editor previews profiles at a deliberately reduced level before an alarm is armed.
- `0.4.0-beta3` adds a compact procedural interface-audio family for taps, selections, confirmations, and errors. It has a persistent switch on the clock screen, follows normal ringer mode, and never uses the alarm stream.
- Optional “Are you really awake?” guard after clearing a set:
  - Show a quiet confirmation after five minutes.
  - If confirmed, finish the morning.
  - If ignored for a configurable grace period, restore or fire the final safety stage.
  - The guard is opt-in per Wake Set and visibly armed before sleep.

## Phase 3 — Alarm Integrity Check

Add a deterministic status panel rather than pretending Android delivery is guaranteed.

- Exact alarm capability.
- Notification permission.
- Full-screen alarm permission on Android 14+.
- Alarm notification channel state.
- Selected alarm tone availability.
- Next platform alarm time versus EutherTime's stored next occurrence.
- Boot/timezone receiver readiness.
- `TEST IN 60 SECONDS` creates a clearly labelled disposable test alarm.

The panel must link directly to the relevant Android setting when the user can fix an issue.

## Phase 4 — Home-screen widget

Create an optional Glance widget with no network dependency.

- Next alarm time, title, Wake Set stage count, and repeat summary.
- `SKIP NEXT` with an explicit confirmation state.
- Shortcut to the alarm editor.
- Compact and expanded cyberpunk layouts.
- Widget state reads the same device-protected alarm store as the app.

## Phase 5 — Local morning journal

- After a completed morning, offer `DEAD`, `OKAY`, or `SHARP`.
- Store only timestamp, selected rating, scheduled wake time, and optional Wake Set ID locally.
- Show simple local trends by weekday and alarm time.
- Allow export/delete; never require an account.
- Do not claim medical or sleep-stage conclusions from subjective ratings.

## Phase 6 — Optional Health Connect

- Entirely opt-in and isolated behind a provider interface.
- Read sleep sessions only after a clear permission explanation.
- Use sleep data for retrospective summaries, not as a hard dependency for alarm delivery.
- Never upload health data.
- The core APK behavior remains useful when Health Connect is absent or permission is revoked.

## Phase 7 — NFC/QR Hard Mode

- Optional per alarm or Wake Set final stage.
- NFC mode accepts a user-enrolled tag stored as a one-way identifier.
- QR mode accepts a locally generated challenge or enrolled code.
- The alarm keeps ringing while the challenge screen is active.
- Always provide a documented emergency fallback after a deliberate hold/countdown so a lost tag cannot create an unstoppable alarm.
- Camera and NFC permissions are requested only when the corresponding mode is enabled.

### NFC beta implemented in `0.4.0-beta1`

- One passive NFC tag can be enrolled or replaced from the Alarms screen without writing to the tag.
- EutherTime stores only a salted SHA-256 fingerprint in device-protected local storage.
- NFC release is opt-in per Morning Link and gates the whole-set `I'M UP` action; `Snooze` and `Next Signal` remain available.
- The ringing lock-screen activity runs Android NFC reader mode and accepts the enrolled tag without unlocking or leaving the alarm screen.
- Pre-alarm and ringing notifications omit their clear-set shortcut while NFC release is armed, preventing an unintended bypass.
- A two-step 30-second emergency release remains available for a lost tag, disabled NFC radio, or unavailable reader.
- Forgetting the enrolled tag also disables NFC release on existing scheduled Morning Links.
- QR mode remains planned; physical NFC and locked-screen behavior still requires testing on the target GrapheneOS phone.

## Data model direction

The existing `ScheduledAlarm` stays as the exact scheduled occurrence. New durable records are layered above it:

- `WakeSet`: stable ID, title, weekdays, enabled state, awake-guard settings.
- `WakeStage`: stable ID, Wake Set ID, local time or offset, title, role, challenge mode.
- `ScheduledAlarm`: occurrence ID, source schedule/stage IDs, trigger time, snooze state.
- `MorningCheck`: temporary guard state with expiry and fallback stage.
- `WakeJournalEntry`: local subjective outcome.

Storage remains device-protected JSON initially. Schema versions and tolerant readers provide migration from existing alarm entries.

## Release sequence

1. `0.2.0`: Wake Set model/editor, skip-next semantics, stage roles.
2. `0.2.1`: progressive signals, ACE-Step alarm family, and awake guard.
3. `0.2.2`: integrity dashboard and 60-second test.
4. `0.3.0`: Glance widget and morning journal.
5. `0.3.1`: optional Health Connect provider.
6. `0.4.0`: NFC/QR Hard Mode.

Each release is independently usable and deployable through EutherOxide Apps while preserving older versioned APK routes.

## Validation matrix

- One-shot alarm, recurring alarm, and every Wake Set stage across process death.
- Dismiss, snooze, skip-next, and clear-set without deleting future weekly occurrences.
- Boot, locked boot, package replacement, manual time change, timezone change, and daylight-saving transitions.
- Notification denied, full-screen access denied, exact alarm access unavailable, and alarm tone missing.
- Locked-screen behavior on a physical GrapheneOS device.
- NFC tag absent/lost and QR camera permission denied.
- Upgrade from 0.1.4 and 0.1.5 with existing alarms intact.
