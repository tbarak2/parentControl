# Development Phases

## Phase 1 — Child App Core (App Blocking)

**Goal:** A working app that can block specified apps on the child's device.

### Deliverables
- [x] Android project setup (`child-app` module, Kotlin, min SDK 26)
- [x] `AccessibilityService` that detects foreground app changes
- [x] Hardcoded blocked app list (for testing, before Firebase) — SharedPreferences-backed `RulesRepository`
- [x] `BlockedScreen` — full-screen overlay when blocked app detected
- [x] `DeviceAdminReceiver` — prevent uninstallation
- [x] `ForegroundService` + `BootReceiver` — keep service alive
- [x] Basic `SetupActivity` — enable required permissions with guided UI

### Success Criteria
- Open a blocked app → blocked screen appears immediately
- Reboot device → monitoring resumes automatically
- Cannot uninstall app without disabling Device Admin first

---

## Phase 2 — Firebase Integration + Parent App

**Goal:** Parent can manage rules remotely in real-time.

### Deliverables
- [x] Firebase project setup (Auth, Firestore, FCM) — see docs/FIREBASE_SETUP.md
- [x] Pairing flow: parent generates 6-digit code → child enters it → devices linked
- [x] Child app reads blocked app list from Firestore in real-time (`FirebaseSync.kt`)
- [x] `parent-app` with:
  - Login/register screen (Firebase email auth)
  - Dashboard listing linked child devices
  - App list screen with block toggle per app (real-time Firestore sync)
  - Pairing code generator
- [ ] Firestore security rules (deploy from docs/FIREBASE_SETUP.md)

### Success Criteria
- Parent toggles app block → child device enforces it within 5 seconds
- Works when child device was offline and comes back online

---

## Phase 3 — Usage Stats + Time Limits

**Goal:** Time-based controls and usage visibility for the parent.

### Deliverables
- [x] Child app reports per-app usage stats to Firestore every 15 min (WorkManager `UsageWorker`)
- [x] Parent app: today's usage per app (`UsageActivity`)
- [x] Daily time limit per app — parent sets limit, child enforces it (`TimeLimitChecker` via `UsageWorker`)
- [x] Schedule-based blocking — `ScheduleActivity` in parent, `ScheduleChecker` in child (60s polling)
- [x] Blocked attempt logging — child writes to Firestore `blockedAttempts` collection on each block

### Success Criteria
- Parent sees accurate usage within 1 minute of activity
- App auto-blocks when daily limit is reached
- Schedule rules apply even if parent app is offline

---

## Phase 4 — Polish & Production

**Goal:** App is stable, user-friendly, and ready for real use.

### Deliverables
- [x] Child can request access to blocked app → parent gets real-time notification to approve/deny
- [x] Parent approves with 30 min or 1 hr — child device temporarily unblocks the app
- [x] Emergency contacts — phone numbers always callable even during schedule lock
- [x] Dialer apps always bypassed (never blocked)
- [x] Offline-safe — all rules cached in SharedPreferences, last known rules apply if no internet
- [ ] App icons, branding
- [ ] Play Store listing preparation

---

## Current Status

**Active Phase: Phase 4**
