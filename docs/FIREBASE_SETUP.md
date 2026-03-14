# Firebase Setup Guide

## 1. Create a Firebase Project

1. Go to [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Click **Add project**
3. Name it `ParentControl` (or any name you prefer)
4. Disable Google Analytics if you don't need it → click **Create project**

---

## 2. Add Android Apps to the Project

You need to register **two** Android apps:

### Child App
- Click **Add app** → Android
- Package name: `com.parentcontrol.child`
- App nickname: `ParentControl - Child`
- Download `google-services.json` → place it in `child-app/app/`

### Parent App
- Click **Add app** → Android
- Package name: `com.parentcontrol.parent`
- App nickname: `ParentControl - Parent`
- Download `google-services.json` → place it in `parent-app/app/`

---

## 3. Enable Authentication

1. Go to **Authentication** → **Sign-in method**
2. Enable **Email/Password** (and optionally **Google**)
3. Only the parent needs an account — the child app links via pairing code, not login

---

## 4. Enable Firestore

1. Go to **Firestore Database** → **Create database**
2. Start in **production mode**
3. Choose a region close to you

### Security Rules

Replace the default rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Parent can read/write their own family document
    match /families/{parentUid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == parentUid;
    }

    // Child device can read its own rules (authenticated via custom token)
    match /families/{parentUid}/children/{childId}/{document=**} {
      allow read: if request.auth != null && request.auth.uid == childId;
      allow write: if request.auth != null && request.auth.uid == parentUid;
    }
  }
}
```

---

## 5. Enable Cloud Messaging (FCM)

1. Go to **Cloud Messaging** — it is enabled by default
2. No extra configuration needed at this stage
3. The child app will register an FCM token on first launch and save it to Firestore
4. The parent app uses this token to send push commands (e.g., instant lock)

---

## 6. Security Checklist

- [ ] `google-services.json` is listed in `.gitignore` — never commit it
- [ ] Firestore security rules are deployed (not in test mode)
- [ ] Firebase project has a budget alert configured to avoid surprise bills
