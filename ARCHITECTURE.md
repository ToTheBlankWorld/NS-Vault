# NS Vault — Architecture & Build Plan

A private, fully offline audio vault. Recordings are encrypted the moment they
stop, unlocked only by PIN or fingerprint, and never leave the device.

---

## 1. Product invariants

These are non-negotiable and every phase is checked against them:

1. **Offline by construction.** The manifest declares no `INTERNET` permission.
   The OS enforces what a privacy policy can only promise.
2. **Only ciphertext at rest.** A plaintext `.m4a` exists only as a transient
   artifact during capture or playback, inside app-private storage, and is
   securely deleted immediately after use.
3. **Keys never leave hardware.** AES-256-GCM data keys live in the Android
   Keystore (StrongBox where available). No key material in code, prefs, or DB.
4. **Nothing leaks to the system.** `FLAG_SECURE` blocks screenshots/recents
   thumbnails; backup and device-transfer are disabled via
   `data_extraction_rules.xml`; files live in internal storage invisible to
   gallery, music apps, and file browsers.

## 2. Tech stack (pinned)

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.2.21, Jetpack Compose (BOM 2025.12.01), Material 3 |
| Build | AGP 8.13.2, Gradle 8.14.3, JDK 17 bytecode, KSP |
| Architecture | MVVM + Clean Architecture, single `:app` module |
| DI | Hilt 2.57.2 |
| Persistence | Room 2.8.4 (metadata), DataStore (settings) |
| Navigation | Navigation Compose 2.9.8, type-safe `@Serializable` routes |
| Audio capture | `MediaRecorder` → AAC in `.m4a`, foreground service |
| Playback | Media3 ExoPlayer 1.8.0 with a custom decrypting DataSource |
| Crypto | Chunked AES-256-GCM, envelope DEK sealed by Android Keystore (StrongBox where available) — our own `VaultCipher`, no third-party crypto lib |
| Auth | `BiometricPrompt` + PIN (salted, hardware-bound verification) |
| SDK | min 29 (Android 10), target/compile 36 |
| Typography | Inter (bundled — no downloadable fonts, we're offline) |

**Why a single Gradle module:** module boundaries pay for themselves on team
scale and CI caching, neither of which applies here. We keep clean-architecture
*layers* as package boundaries (`feature → domain ← data`), which preserves the
discipline without the build overhead. If the app grows, packages lift into
modules mechanically.

## 3. Package layout

```
com.nsvault.app
├── NSVaultApplication.kt        Hilt entry point
├── MainActivity.kt              Edge-to-edge, FLAG_SECURE, splash
├── core/
│   ├── common/                  Result types, time/format utilities
│   └── di/                      Dispatcher qualifiers + modules
├── data/                        (Phases 2-5)
│   ├── audio/                   MediaRecorder engine, foreground service
│   ├── crypto/                  Keystore key manager, streaming AES-GCM
│   ├── database/                Room: entities, DAOs, database
│   ├── vault/                   Encrypted file store, year/month layout
│   └── settings/                DataStore-backed preferences
├── domain/                      (Phases 2-6)
│   ├── model/                   Recording, VaultStats, AudioQuality…
│   └── usecase/                 One class per user intention
├── designsystem/
│   ├── theme/                   Color, Gradient, Type, Shape, Dimens, Motion, Theme
│   └── component/               AuroraBackground, GlassCard, VaultTopBar…
├── navigation/                  Route (sealed, @Serializable), NSVaultNavHost
└── feature/
    ├── auth/                    (Phase 2) PIN setup, lock screen, biometrics
    ├── home/                    Dashboard
    ├── recorder/                (Phase 3) capture UI
    ├── player/                  (Phase 6) playback UI
    ├── library/                 (Phase 5) list, grouping, search
    └── settings/                (Phases 2-5) preferences
```

Dependency rule: `feature → domain ← data`; `designsystem` and `core` are leaf
utilities; nothing depends on `feature`.

## 4. Security design (implemented across Phases 2 & 4)

**Key hierarchy (envelope encryption).** Every file gets a fresh random
256-bit data key (DEK). The DEK is sealed by a non-exportable AES-256-GCM key
in the Android Keystore (StrongBox where available) and carried in the file
header. The fast software cipher does the bulk work; the hardware key wraps and
unwraps DEKs only and never leaves the Keystore.

**File format** (`.enc` v1):
`[magic "NSV1"][version][chunkSize][wrappedKeyLen][wrappedKey][noncePrefix 8B]`
then framed chunks `[len][AES-256-GCM(chunk)]`, 512KB each, IV =
noncePrefix‖counter, AAD = [version, isFinalChunk]. Reordering breaks the
counter, truncation breaks the missing final flag, any bit flip breaks the GCM
tag. Chunking keeps memory constant for multi-hour files and gives playback
chunk-level random access for seeking. Every save is round-trip verified
(SHA-256 of plaintext during encrypt vs. full decrypt) before plaintext temps
are zero-overwritten and deleted.

**PIN.** Never stored. A random 32-byte salt + PBKDF2-HMAC-SHA256 hash go into
EncryptedSharedPreferences (Keystore-wrapped). Verification is constant-time.
Biometric unlock wraps the same session gate through `BiometricPrompt` with
`setAllowedAuthenticators(BIOMETRIC_STRONG)`.

**Capture pipeline.** Record AAC-ADTS to a temp file in `filesDir/capture/` —
ADTS is a streaming format, so a capture truncated by a crash stays decodable,
which is what makes "Recover Recording" honest (a crashed MPEG-4 would be
corrupt). On stop: losslessly remux to `.m4a` → (Phase 4) encrypt-stream to
`filesDir/vault/yyyy/MM/` → verify → securely delete the temp → insert Room
metadata row. Interrupted sessions found in `capture/` at next launch are
offered for recovery — never silently deleted.

**Playback.** A custom Media3 `DataSource` decrypts on the fly; when seeking
demands it, a temp decrypted file lives only for the playback session and is
zeroed + deleted on stop/dispose. Share requires re-authentication and exports
a decrypted copy through `FileProvider` only after explicit confirmation.

## 5. Design system (implemented in Phase 1)

Dark-only by deliberate choice — a vault is a night-time object and one theme
keeps the brand exact on every device.

- **Canvas:** obsidian `#0B0D12` with two faint radial "aurora" glows
  (violet top-left, cyan bottom-right) — `AuroraBackground`.
- **Accent ramp:** violet `#7C5CFF` → indigo `#5B8CFF` → cyan `#4ADEDE`
  (`VaultGradients.Aurora`); gold `#E8C468` reserved for favorites; red
  `#FF5C6C` exclusively means "recording".
- **Surfaces:** glassmorphism kept subtle — 4% white fill, gradient hairline
  border that catches light at the top edge (`GlassCard`).
- **Type:** Inter, four bundled weights, tightened tracking on display sizes.
- **Shape:** 8/12/16/20/28 dp radius scale.
- **Motion:** single source of truth in `VaultMotion` (emphasized/decelerate/
  accelerate easings, 200/350/500 ms). All screen transitions and component
  animations must draw from it — this is what makes the app move as one system.

## 6. Phase plan

| Phase | Scope | Definition of done |
|---|---|---|
| **1 · Foundation** ✅ | Gradle + version catalog, package skeleton, design system, type-safe navigation, launcher icon, splash, manifest hardening | `assembleDebug` green; app runs showing themed dashboard shell |
| **2 · Authentication** | Keystore bootstrap, PIN setup (4/6), lock screen, BiometricPrompt, change-PIN in settings, session lock on background | Cold start lands on PIN setup → lock → home; biometrics toggle works |
| **3 · Recording engine** | Foreground service + `MediaRecorder`, pause/resume, live amplitude waveform, timer, call/focus interruption handling, crash recovery of temp captures | 40-min recording survives screen-off, calls, and process death |
| **4 · Encryption engine** | Streaming AES-GCM encrypt/decrypt, file format, round-trip verification, secure delete, recovery of interrupted encrypts | Stopping a recording yields only a verified `.enc`; no plaintext remains |
| **5 · Vault management** | Room schema, repository, year/month organization, library UI, search, rename/delete/favorite, import `.m4a`, storage stats on home | 1000-recording library scrolls at 60fps; import encrypts and verifies |
| **6 · Playback** | Decrypting DataSource, ExoPlayer session, waveform seek, speed control, share-after-auth | Seek/speed/background behavior correct; temp plaintext provably cleaned |
| **7 · Polish** | Micro-interactions, haptics, transition choreography, empty/error states, fingerprint success animation, save celebration | Every interaction animates on motion tokens; no janky frame in profile |
| **8 · Hardening** | Unit + instrumentation tests, leak checks, baseline profile, R8 release config, edge-case error handling audit | Release build passes full manual test script + automated suite |

## 7. Error-handling contract (applies from Phase 3 on)

Every failure path maps to a typed `VaultError` and a designed UI state — no
raw exceptions reach a screen: permission denied (rationale + settings
deep-link), storage full (pre-flight check before recording), encryption
failure (temp retained, retry offered — capture is never silently lost),
interruption (auto-pause, notification, resume offer), process death
(foreground service + on-launch recovery scan).

## 8. Build environment notes

- Command-line builds need JDK 17–21. A Temurin JDK 21 lives at
  `%USERPROFILE%\.jdks\jdk-21.0.11+10`; set `JAVA_HOME` to it for `gradlew`.
  (The system JDK 26 is too new for Gradle 8.14.) Android Studio uses its own
  embedded JBR and needs no configuration.
- `local.properties` points at the SDK and stays untracked.
