# Road Survey Manager — Android App (Offline / Local Storage Only)

Professional offline road survey file manager for Android.

## Important Storage Policy
- **100% local phone storage** — no cloud, no Firebase, no login, no internet required for core features.
- Metadata stored in Room (SQLite).
- Imported files copied into app-private storage when possible.
- Uses Android Storage Access Framework (SAF) / system file picker.

## How to build the APK

### Requirements
- Android Studio (Hedgehog or newer recommended)
- JDK 17+
- Android SDK 34+

### Steps
1. Unzip this project.
2. Open the folder in Android Studio (“Open an Existing Project”).
3. Let Gradle sync (first time may download dependencies).
4. Connect a phone or start an emulator (API 26+).
5. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. The APK will appear under `app/build/outputs/apk/debug/app-debug.apk`.

You can also run:
```bash
./gradlew assembleDebug
```

### Permissions used
- READ/WRITE only through the system file picker (SAF) — no broad storage permission on modern Android.
- Camera (optional, for taking photos).
- Internet is **not** required and is not used for data storage.

### Demo data
On first launch the app inserts a sample project “Murree Road Survey (MR-01)” with placeholder records so you can explore search, projects, favorites, etc. Real files must be imported by the user.

