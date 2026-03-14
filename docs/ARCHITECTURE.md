# Architecture

## Overview

ParentControl consists of two Android apps sharing a Firebase backend.

```
┌─────────────────────┐         ┌──────────────────────┐
│     Parent App      │         │      Child App        │
│  (parent's phone)   │◄──────►│  (child's phone)      │
│                     │Firebase │                       │
│ - Manage rules      │  Sync   │ - Enforce rules       │
│ - View usage stats  │         │ - Block apps          │
│ - Remote lock       │         │ - Report usage        │
└─────────────────────┘         └──────────────────────┘
            │                              │
            └──────────┬───────────────────┘
                       │
              ┌────────▼────────┐
              │    Firebase     │
              │                 │
              │ - Auth          │
              │ - Firestore     │
              │ - FCM (push)    │
              └─────────────────┘
```

---

## Child App — Internal Architecture

```
child-app/
├── service/
│   ├── MonitoringService.kt      # Foreground service — stays alive in background
│   └── BootReceiver.kt           # Restarts service after device reboot
├── accessibility/
│   └── AppBlockerAccessibility.kt # AccessibilityService — detects & blocks apps
├── admin/
│   └── DeviceAdminReceiver.kt    # Device Admin — prevents uninstallation
├── repository/
│   ├── RulesRepository.kt        # Fetches rules from Firestore, caches locally
│   └── UsageRepository.kt        # Reports usage stats to Firestore
├── ui/
│   ├── SetupActivity.kt          # Pairing flow (one-time setup)
│   └── BlockedScreen.kt          # Full-screen overlay shown when app is blocked
└── firebase/
    └── FirebaseSync.kt           # Listens for real-time rule changes from parent
```

### How App Blocking Works

1. `AppBlockerAccessibility` (AccessibilityService) listens for `TYPE_WINDOW_STATE_CHANGED` events.
2. When a new app comes to foreground, it checks the package name against the blocked list.
3. If blocked → immediately launch `BlockedScreen` activity on top.
4. `BlockedScreen` is a full-screen activity the child cannot dismiss (no back button, no recents).

### How Rules Are Synced

1. Parent changes a rule in the parent app → writes to Firestore.
2. `FirebaseSync` on the child device listens to Firestore in real-time → updates local cache immediately.
3. If offline → local cache is used (last known rules apply).

---

## Parent App — Internal Architecture

```
parent-app/
├── ui/
│   ├── DashboardActivity.kt      # Overview: child's current app, usage today
│   ├── AppListActivity.kt        # List of installed apps — toggle block on/off
│   ├── ScheduleActivity.kt       # Set time-based rules
│   └── PairingActivity.kt        # Generate pairing code for child device
├── repository/
│   ├── ChildRepository.kt        # Reads child's usage data from Firestore
│   └── RulesRepository.kt        # Writes rules to Firestore
└── firebase/
    └── FirebaseSync.kt           # Pushes commands, listens for usage updates
```

---

## Firebase Firestore Structure

```
/families/{parentUid}/
  ├── children/{childDeviceId}/
  │   ├── deviceInfo: { model, androidVersion, lastSeen }
  │   ├── rules/
  │   │   ├── blockedApps: ["com.example.game", ...]
  │   │   └── schedule: { startBlock: "21:00", endBlock: "07:00" }
  │   └── usageStats/
  │       └── {date}/
  │           └── {packageName}: { minutes: 45 }
```

---

## Key Android Components Used

| Component | Purpose |
|---|---|
| `AccessibilityService` | Detect foreground app changes, trigger blocking |
| `UsageStatsManager` | Query per-app usage time |
| `DevicePolicyManager` | Device admin to prevent uninstallation |
| `ForegroundService` | Keep monitoring alive, survive battery optimization |
| `BroadcastReceiver` (BOOT) | Restart monitoring service after reboot |
| Firebase Firestore | Real-time rule sync + usage data storage |
| Firebase Auth | Parent account authentication |
| Firebase FCM | Push commands (e.g., instant remote lock) |
