# Road Survey Manager — Native Android project (Mubashir)

## Implemented in this revision
- Android Storage Access Framework `ACTION_OPEN_DOCUMENT` picker, including multi-select.
- Copies selected URI streams to app-private `filesDir/imported` using streaming IO (does not load the whole file into RAM).
- Stores a native file ID mapped to the app-private path in SharedPreferences; the web record keeps its original filename and a separate display name.
- FileProvider shares app-managed files through secure `content://` URIs with read permission and MIME types. `Open With` launches Android's app chooser; Share launches Android share sheet.
- No broad `MANAGE_EXTERNAL_STORAGE` permission. No fake demo records seeded at launch.
- Full JSON backup includes base64 file bytes available to the web layer; metadata-only backup excludes bytes. Restore creates new IDs to avoid overwriting current records and remaps project relationships.

## Build requirements
Android Studio, JDK 17, Android SDK platform 35, network access for Gradle dependency resolution. Open this folder as an Android Studio project and build `app` > Build APK(s).

## Known limitations / must test on device
- This project was not compiled or run on a physical Android device in the current environment. Therefore this ZIP is source code, not a verified APK.
- Web UI metadata and browser blobs still use IndexedDB. Native copies are additionally kept under app-private storage. If the web UI's IndexedDB quota is exceeded, JS import may still fail even if native copy succeeded; test large files.
- The full JSON backup serializes file bytes to base64 and can use substantial memory for very large files. A native streaming ZIP backup is recommended for production-scale CAD files.
- Opening a file requires an installed app that supports that MIME/type. Android will show an app chooser or an error if no handler exists. CAD rendering is not built into this app.
- Native app-private files are removed if the app is uninstalled or its data is cleared. Export backups to user-selected storage before that.
- No broad storage permission is needed for user-selected files; Android's system picker exposes locations permitted by the device.

## Build
1. Extract the ZIP.
2. Open the extracted project directory in Android Studio.
3. Wait for Gradle sync.
4. Choose **Build > Build APK(s)**.
5. Debug APK usually appears at `app/build/outputs/apk/debug/app-debug.apk`.
