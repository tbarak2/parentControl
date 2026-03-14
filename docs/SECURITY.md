# Security Model

## Threat Model

The main adversary is **the child trying to bypass controls**. We are not defending
against a sophisticated external attacker — we are defending against a motivated
teenager with physical access to their own device.

---

## Controls in Place

### 1. Prevent Uninstallation
- Child app registers as a **Device Administrator**
- Android prevents uninstalling apps with active Device Admin
- To uninstall, Device Admin must be revoked first — which triggers a warning/delay we can use to alert the parent

### 2. Prevent Disabling Accessibility Service
- We cannot technically prevent this, but:
  - `MonitoringService` detects when Accessibility is revoked and sends an alert to the parent via FCM
  - Parent receives a push notification: "Accessibility permission was disabled on [child's device]"

### 3. Prevent Killing the Background Service
- App runs as a **Foreground Service** with a persistent notification (required by Android)
- Registered as a Device Admin so it cannot be force-stopped via Settings easily
- `BootReceiver` restarts it after reboot

### 4. Firebase Authentication
- Only the authenticated parent (by Firebase UID) can write rules to Firestore
- Child device authenticates with a Firebase custom token (scoped read-only to its own document)
- Pairing codes are one-time use and expire after 15 minutes

### 5. Firestore Security Rules
- Parent UID is the top-level namespace — a parent can only access their own children
- Child can only read its own rules document — cannot read other children or write anything

---

## Known Limitations

| Limitation | Notes |
|---|---|
| Factory reset bypass | Child could factory reset — Device Admin does NOT survive factory reset |
| Safe mode bypass | In safe mode, third-party apps (including this one) don't run — child could use safe mode to play blocked apps |
| App sideloading | Child could sideload a different version of a blocked app (different package name) |
| Root | A rooted device can bypass all of this |

### Mitigations for Known Limitations
- **Factory reset**: Enable "Factory Reset Protection" (FRP) separately — tie device to parent's Google account
- **Safe mode**: Inform parent via persistent notification if monitoring hasn't reported in >30 minutes
- **Sideloading**: Block "Install unknown apps" permission via Device Admin policies

---

## Data Privacy

- No data leaves Firebase except for usage stats and rule updates
- Usage stats contain only package names and duration — no content, no messages, no photos
- Firebase project is owned by the parent — they control and can delete all data
