# ParentControl — Project Rules for Claude

## Project Overview
Android parental control app (similar to Google Family Link) targeting devices
where the child uses a regular Gmail account (not a Kids/Family Link account).

## Documentation Rules
- **ALL decisions, architecture changes, and new features must be documented** in the `docs/` folder before or alongside implementation.
- Keep `docs/ARCHITECTURE.md` up to date whenever structure changes.
- Keep `README.md` at the root up to date with install/setup steps.
- Never leave a feature undocumented.

## Code Rules
- Language: **Kotlin** (Android native). No Java.
- Min SDK: **26** (Android 8.0). Target SDK: latest stable.
- Use **Firebase** for real-time sync between parent and child devices.
- All background services must handle Android battery optimization (foreground service with notification).
- Never request permissions that are not strictly necessary.
- Every screen must handle the case where the child device is offline.

## Architecture Rules
- Two apps: `child-app` and `parent-app` (separate Android modules or separate projects).
- Shared Firebase project for both apps.
- Business logic must be separated from Android framework code (use ViewModels, repositories).
- No hardcoded strings — use `strings.xml`.

## Security Rules
- Parent authentication via Firebase Auth (email/password or Google Sign-In).
- Child device must be linked to a parent account before receiving any commands.
- All Firestore rules must enforce that only the linked parent can write to a child's document.

## Git Rules
- Commit messages must be descriptive and reference the feature/fix.
- Never commit API keys, `google-services.json`, or secrets. Use `.gitignore`.

## Phase Plan
- **Phase 1**: Child-side app — app blocking via AccessibilityService + Device Admin
- **Phase 2**: Parent-side app — remote rule management via Firebase
- **Phase 3**: Usage stats, time limits, reporting dashboard
