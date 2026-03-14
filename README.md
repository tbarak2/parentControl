# ParentControl

An Android parental control application that lets parents monitor and restrict app usage on their child's device — even when the child uses a regular Gmail account (not a Google Kids/Family Link account).

---

## Features (Planned)

- Block specific apps remotely from the parent's phone
- Set time windows (e.g., no games after 9pm)
- Daily usage limits per app
- Remote screen lock from parent's phone
- Real-time app usage reporting
- Prevent uninstallation by the child

---

## Architecture

| Component | Description |
|---|---|
| `child-app` | Installed on the child's device. Monitors and enforces rules. |
| `parent-app` | Installed on the parent's device. Manages rules remotely. |
| Firebase | Real-time sync between parent and child. Auth + Firestore + FCM. |

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for full details.

---

## Requirements

- Android 8.0 (API 26) or higher on both devices
- Internet connection on both devices
- A Google/Firebase account for the parent

---

## Installation & Setup

### Step 1 — Build the apps

```bash
# Clone the repo
git clone <repo-url>
cd parentControl

# Open in Android Studio and build both modules:
# - child-app
# - parent-app
```

### Step 2 — Firebase setup

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Add two Android apps to the project:
   - `com.parentcontrol.child`
   - `com.parentcontrol.parent`
3. Download each `google-services.json` and place them in the respective `app/` folders.
4. Enable **Firestore**, **Authentication**, and **Cloud Messaging** in the Firebase console.

### Step 3 — Install on parent's device

1. Install `parent-app` on your phone.
2. Sign in with your Google/email account.
3. You will receive a **pairing code** to link your child's device.

### Step 4 — Install on child's device (do this yourself)

1. Install `child-app` on your child's phone.
2. Enter the pairing code from Step 3.
3. Grant the following permissions when prompted:
   - **Accessibility Service** — required for app blocking
   - **Usage Access** — required for monitoring app usage
   - **Device Administrator** — required to prevent uninstallation
4. The app will run silently in the background from this point.

> **Note:** Steps 3 and 4 of the child setup must be done by the parent physically on the child's device.

---

## Permissions Used

| Permission | Why |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Detects when a blocked app opens and closes it |
| `PACKAGE_USAGE_STATS` | Monitors how long each app is used |
| `RECEIVE_BOOT_COMPLETED` | Restarts the monitoring service after reboot |
| `FOREGROUND_SERVICE` | Keeps the monitoring service alive |
| Device Admin | Prevents the child from uninstalling the app |

---

## Docs

- [Architecture](docs/ARCHITECTURE.md)
- [Phase Plan](docs/PHASES.md)
- [Firebase Setup](docs/FIREBASE_SETUP.md)
- [Security Model](docs/SECURITY.md)

---

## Project Status

Currently in **Phase 1** — building the child-side app (app blocking core).

See [`docs/PHASES.md`](docs/PHASES.md) for the full roadmap.
