# NS Vault — Release Guide

Everything needed to cut a signed, Play-ready build of NS Vault.

---

## 1. Build environment

- **Android Studio** (latest stable) — uses its bundled JDK; no setup needed.
- **Command line** — needs JDK 17–21. A Temurin JDK 21 is provisioned at
  `%USERPROFILE%\.jdks\jdk-21.0.11+10`; point `JAVA_HOME` at it. The system
  JDK 26 is too new for Gradle 8.14.
- `local.properties` must contain `sdk.dir` (already present, untracked).

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.11+10"
.\gradlew.bat :app:bundleRelease   # AAB for Play
.\gradlew.bat :app:assembleRelease # APK for sideload / testing
```

## 2. Create an upload keystore (one time)

```bash
keytool -genkeypair -v -keystore nsvault-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 -alias nsvault
```

Store `nsvault-upload.jks` **outside** the repo and back it up — losing it
means you can never update the app (unless enrolled in Play App Signing key
rotation). Keep it secret; anyone with it can sign as you.

## 3. Wire up signing

Create `keystore.properties` in the project root (already git-ignored):

```properties
storeFile=C:/keys/nsvault-upload.jks
storePassword=********
keyAlias=nsvault
keyPassword=********
```

`app/build.gradle.kts` reads this automatically. If the file is absent, the
release build still assembles **unsigned** (useful for CI/R8 checks). With it
present, `assembleRelease` / `bundleRelease` produce a signed artifact.

Enroll in **Play App Signing** when you first upload: Play holds the app
signing key, your `keystore.properties` key becomes the upload key.

## 4. Before every release

- Bump `versionCode` (must strictly increase) and `versionName` in
  `app/build.gradle.kts`.
- Build the **AAB**: `.\gradlew.bat :app:bundleRelease`
  → `app/build/outputs/bundle/release/app-release.aab`.
- Keep the R8 mapping file for crash deobfuscation:
  `app/build/outputs/mapping/release/mapping.txt` — upload it to the Play
  Console (Play does this automatically for AABs, but archive it too).

## 5. R8 / resource shrinking

Already enabled for `release`: `isMinifyEnabled = true`,
`isShrinkResources = true`, using `proguard-android-optimize.txt` +
`app/proguard-rules.pro`. Library consumer rules cover Hilt, Room, Media3,
DataStore, Compose and Coroutines; our file adds kotlinx.serialization keeps,
enum-by-name keeps, crash line tables, and a few defensive keeps. See
`proguard-rules.pro` for the annotated list.

## 6. Signing & release checklist

**Signing**
- [ ] Upload keystore generated (RSA 4096) and backed up in ≥2 safe places.
- [ ] `keystore.properties` present locally, absent from git.
- [ ] Enrolled in Play App Signing.
- [ ] `bundleRelease` produces a signed `.aab`; `mapping.txt` archived.

**Build integrity**
- [ ] `versionCode` incremented; `versionName` updated.
- [ ] Release build installs and launches on a physical device (min SDK 29
      and a recent SDK).
- [ ] Manifest declares **no INTERNET permission** (verify in the merged
      manifest / `aapt dump permissions`).

**Manual smoke test on a release build**
- [ ] First launch → PIN setup (4- and 6-digit), fingerprint enrollment.
- [ ] Lock/unlock: PIN, fingerprint, wrong-PIN lockout countdown.
- [ ] Record 40+ min; screen-off, incoming call, notification controls,
      app kill → recovery offer on relaunch.
- [ ] Stop → encrypt/verify/save; vault dir holds only `.enc` files.
- [ ] Library: search, rename, favorite, delete; import an external `.m4a`.
- [ ] Player: play, scrub a long file (seeks instant), speed, background =
      pause, share → confirm + fingerprint → chooser.
- [ ] Audio-quality setting changes new-recording bitrate.
- [ ] Rotate every screen; verify state survives.

**Store listing**
- [ ] Data safety form: "No data collected / no data shared" (app is offline).
- [ ] Screenshots captured on a device **with FLAG_SECURE temporarily
      disabled in a debug build** — release builds block screenshots by design.
- [ ] Content rating, privacy policy URL (state: fully on-device, no
      collection), target-audience declarations.
- [ ] Foreground-service `microphone` type is declared and justified in the
      Play Console (recording use case).

## 7. Known constraints (by design)

- **Screenshots are blocked** in-app (`FLAG_SECURE`) — this includes the
  recents thumbnail. Capture store screenshots from a debug build with the
  flag removed.
- **No cloud backup / device transfer** — recordings are excluded from
  Android backup. A factory reset or uninstall erases the vault permanently;
  there is intentionally no recovery path (see Known Limitations in the
  handoff summary).
- **Forgotten PIN = no access.** There is no reset/recovery by design; the
  data is only as recoverable as the PIN. Communicate this in the listing.
