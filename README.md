<div align="center">

# NS Vault

**A private, fully offline encrypted audio vault for Android.**

Recordings are encrypted the moment they stop, unlocked only by PIN or fingerprint, and never leave the device.

`Kotlin` · `Jetpack Compose` · `Material 3` · `Hilt` · `Room` · `Media3` · `AES-256-GCM`

[Features](#features) · [Security Model](#security-model) · [Tech Stack](#tech-stack) · [Architecture](#architecture) · [Getting Started](#getting-started) · [Roadmap](#roadmap)

</div>

---

## Pitch

Every other voice recorder uploads, syncs, or quietly phones home. NS Vault is the opposite: it is **offline by construction**. The manifest declares no `INTERNET` permission, so the OS enforces what a privacy policy can only promise.

Your recordings live as **ciphertext at rest**, wrapped in a hardware-backed key hierarchy. A plaintext `.m4a` exists only as a transient artifact during capture or playback — inside app-private storage, securely zero-overwritten and deleted immediately after use.

No account. No cloud. No recovery path. If you forgot your PIN, the data is gone — that is the feature.

## Features

- **End-to-end local encryption** — every file is encrypted with a fresh AES-256-GCM data key, sealed by a non-exportable Android Keystore key (StrongBox where available). Custom chunked `.enc` format with round-trip verification after every save.
- **PIN + biometric unlock** — salted PBKDF2-HMAC-SHA256 PIN verification (constant-time), or `BIOMETRIC_STRONG` fingerprint via `BiometricPrompt`. Forgot your PIN? There is no reset. By design.
- **Foreground-service recording** — `MediaRecorder` → AAC in `.m4a`, with pause/resume, live amplitude waveform, call/focus interruption handling, and crash recovery of interrupted captures. A 40-minute recording survives screen-off, phone calls, and process death.
- **Encrypted playback with random access** — a custom Media3 `DataSource` decrypts on the fly; seeking a multi-hour file is instant. Temporary plaintext lives only for the playback session and is zeroed on stop.
- **Secure vault management** — auto-organized `yyyy/MM/` file layout, search, rename, favorite, delete, and import of external `.m4a` files (imported files are encrypted and verified too).
- **Nothing leaks to the system** — `FLAG_SECURE` blocks screenshots and recents thumbnails; backups and device transfer are disabled; files live in internal storage, invisible to gallery, music apps, and file browsers.
- **Designed to move as one system** — a hand-built obsidian design system: aurora gradient canvas, glass surfaces, Inter typography, and a single source of truth for motion tokens.
- **Dark-only by deliberate choice** — a vault is a night-time object, and one theme keeps the brand exact on every device.

## Security Model

Four product invariants, checked at every phase of development:

1. **Offline by construction** — no `INTERNET` permission in the manifest. Ever.
2. **Only ciphertext at rest** — plaintext exists only as a transient capture/playback artifact and is securely deleted immediately after use.
3. **Keys never leave hardware** — AES-256-GCM data keys live in the Android Keystore (StrongBox where available). No key material in code, preferences, or database.
4. **Nothing leaks to the system** — screenshots blocked, backups disabled, files hidden from other apps.

### Key hierarchy (envelope encryption)

Every file gets a fresh random 256-bit data key (DEK). The DEK is sealed by a non-exportable AES-256-GCM key in the Android Keystore and carried in the file header. The fast software cipher does the bulk work; the hardware key only wraps/unwraps DEKs and never leaves the Keystore.

### File format (`.enc` v1)

```
[magic "NSV1"] [version] [chunkSize] [wrappedKeyLen] [wrappedKey] [noncePrefix 8B]
└─ followed by framed chunks: [len] [AES-256-GCM(chunk)], 512 KB each
```

- IV = `noncePrefix ‖ counter`, AAD = `[version, isFinalChunk]`
- Reordering breaks the counter, truncation breaks the missing-final flag, any bit flip breaks the GCM tag
- Chunking keeps memory constant for multi-hour files and enables chunk-level random access for seeking
- Every save is round-trip verified (SHA-256 during encrypt vs. full decrypt) *before* plaintext temps are zero-overwritten and deleted

### Capture pipeline

```
Record (AAC-ADTS temp) → remux to .m4a → encrypt-stream to vault/yyyy/MM/
→ verify round-trip → securely delete temp → insert Room metadata row
```

ADTS is a streaming format, so a capture truncated by a crash stays decodable — that's what makes "Recover Recording" honest. Interrupted sessions found in `capture/` at next launch are offered for recovery, never silently deleted.

### PIN

Never stored. A random 32-byte salt + PBKDF2-HMAC-SHA256 hash go into EncryptedSharedPreferences (Keystore-wrapped). Verification is constant-time. Biometric unlock wraps the same session gate via `setAllowedAuthenticators(BIOMETRIC_STRONG)`.

## Tech Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.2.21, Jetpack Compose (BOM 2025.12.01), Material 3 |
| Architecture | MVVM + Clean Architecture (single `:app` module) |
| DI | Hilt 2.57.2 |
| Persistence | Room 2.8.4 (metadata), DataStore (settings) |
| Navigation | Navigation Compose 2.9.8, type-safe `@Serializable` routes |
| Audio capture | `MediaRecorder` → AAC in `.m4a`, foreground service |
| Playback | Media3 ExoPlayer 1.8.0 with custom decrypting `DataSource` |
| Crypto | Custom chunked AES-256-GCM (`VaultCipher`) — no third-party crypto lib |
| Auth | `BiometricPrompt` + PIN |
| Build | AGP 8.13.2, Gradle 8.14.3, JDK 17 bytecode, KSP |
| SDK | min 29 (Android 10), target/compile 36 |
| Typography | Inter (bundled — no downloadable fonts, we're offline) |

## Architecture

Clean Architecture layers as package boundaries (`feature → domain ← data`), with `designsystem` and `core` as leaf utilities — nothing depends on `feature`.

```
com.nsvault.app
├── core/                  Result types, dispatcher qualifiers, utilities
├── data/                  MediaRecorder engine, VaultCipher, Room, encrypted file store, settings
├── domain/                Models and one use-case class per user intention
├── designsystem/          Theme tokens (Color, Gradient, Type, Motion…) + components (GlassCard, AuroraBackground…)
├── navigation/            Sealed @Serializable routes, NavHost
└── feature/               auth · home · recorder · player · library · settings
```

Dependency rule: **feature → domain ← data**. `designsystem` and `core` are leaf utilities.

| Layer | What lives there |
|---|---|
| `feature/*` | Screens + ViewModels (Compose, Hilt-injected) |
| `domain/` | Business rules: models + use cases, no Android dependencies |
| `data/` | `RecorderRepositoryImpl`, `VaultRepositoryImpl`, `VaultCipher`, Room DAOs, `VaultFileStore`, settings |
| `designsystem/` | The full visual language: aurora canvas, glass surfaces, motion tokens |

> Why a single Gradle module? Module boundaries pay for themselves at team scale and CI caching — neither applies here. The layer discipline is preserved as package boundaries, and if the app grows, packages lift into modules mechanically.

## Getting Started

### Prerequisites

- **Android Studio** (latest stable) — uses its bundled JDK; no setup needed
- **Command line** — JDK 17–21. Set `JAVA_HOME` to a Temurin JDK 17/21; the system JDK 26 is too new for Gradle 8.14
- `local.properties` with `sdk.dir` pointing at your Android SDK (untracked by git)

### Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (unsigned unless keystore.properties is present)
./gradlew :app:assembleRelease

# AAB for Play
./gradlew :app:bundleRelease
```

Install and launch on a device or emulator (min SDK 29):

```bash
./gradlew :app:installDebug
adb shell am start -n com.nsvault.app.debug/.MainActivity
```

### Release signing

Signing config is read from `keystore.properties` in the project root (git-ignored). When absent, release builds still assemble **unsigned** so they can be R8-verified on any machine. See [RELEASE.md](RELEASE.md) for the full release guide: keystore generation, Play App Signing enrollment, and the manual smoke-test checklist.

## Design System

Dark-only by deliberate choice. Built to move as one system:

- **Canvas** — obsidian `#0B0D12` with two faint radial "aurora" glows (violet top-left, cyan bottom-right)
- **Accent ramp** — violet `#7C5CFF` → indigo `#5B8CFF` → cyan `#4ADEDE`; gold `#E8C468` for favorites; red `#FF5C6C` exclusively means "recording"
- **Surfaces** — subtle glassmorphism: 4% white fill, gradient hairline border catching light at the top edge
- **Shape** — 8/12/16/20/28 dp radius scale
- **Motion** — single source of truth (`VaultMotion`): emphasized/decelerate/accelerate easings, 200/350/500 ms. All transitions and component animations must draw from it

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 · Foundation | Gradle + version catalog, package skeleton, design system, type-safe navigation, launcher icon, splash, manifest hardening | ✅ Done |
| 2 · Authentication | Keystore bootstrap, PIN setup (4/6), lock screen, biometrics, change-PIN, session lock | ✅ Done |
| 3 · Recording engine | Foreground service + MediaRecorder, pause/resume, live waveform, interruption handling, crash recovery | ✅ Done |
| 4 · Encryption engine | Streaming AES-GCM, file format, round-trip verification, secure delete, interrupted-encrypt recovery | ✅ Done |
| 5 · Vault management | Room schema, repository, year/month organization, library UI, search, rename/delete/favorite, import, storage stats | ✅ Done |
| 6 · Playback | Decrypting DataSource, ExoPlayer session, waveform seek, speed control, share-after-auth | ✅ Done |
| 7 · Polish | Micro-interactions, haptics, transition choreography, empty/error states | ✅ Done |
| 8 · Hardening | Unit + instrumentation tests, leak checks, baseline profile, R8 release config, error-handling audit | 🚧 In progress (tests remaining) |

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full design document.

## Known Constraints (by design)

- **Screenshots are blocked** in-app (`FLAG_SECURE`) — including the recents thumbnail. Store screenshots must be captured from a debug build.
- **No cloud backup / device transfer** — recordings are excluded from Android backup. Factory reset or uninstall erases the vault permanently.
- **Forgotten PIN = no access** — there is intentionally no reset or recovery path. The data is only as recoverable as the PIN.

## License

All rights reserved. This is a private project — no license grants are implied by sharing the source.