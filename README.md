<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=28&duration=2500&pause=500&color=7C5CFF&center=true&vCenter=true&width=435&lines=NS+VAULT;OFFLINE+BY+CONSTRUCTION;ENCRYPTED+BY+DEFAULT;PRIVATE+BY+DESIGN" alt="NS Vault" />
</p>

<p align="center">
  <b><span style="color:#4ADEDE">A private, fully offline encrypted audio vault for Android.</span></b><br/>
  Recordings are encrypted the moment they stop — unlocked only by <b>PIN</b> or <b>fingerprint</b>, and they <b>never leave the device</b>.
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.21-7C5CFF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0B0D12"/>
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-2025.12.01-5B8CFF?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0B0D12"/>
  <img alt="Material 3" src="https://img.shields.io/badge/Material%203-4ADEDE?style=for-the-badge&logo=materialdesign&logoColor=0B0D12&labelColor=0B0D12"/>
  <img alt="Min SDK" src="https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0B0D12"/>
  <img alt="Encryption" src="https://img.shields.io/badge/AES--256--GCM-E8C468?style=for-the-badge&logo=monero&logoColor=0B0D12&labelColor=0B0D12"/>
</p>

<p align="center">
  <a href="#-features"><img src="https://img.shields.io/badge/FEATURES-7C5CFF?style=flat-square&labelColor=0B0D12"/></a>
  <a href="#-security-model"><img src="https://img.shields.io/badge/SECURITY-5B8CFF?style=flat-square&labelColor=0B0D12"/></a>
  <a href="#-tech-stack"><img src="https://img.shields.io/badge/TECH%20STACK-4ADEDE?style=flat-square&labelColor=0B0D12"/></a>
  <a href="#-architecture"><img src="https://img.shields.io/badge/ARCHITECTURE-E8C468?style=flat-square&labelColor=0B0D12"/></a>
  <a href="#-getting-started"><img src="https://img.shields.io/badge/GETTING%20STARTED-3DDC84?style=flat-square&labelColor=0B0D12"/></a>
  <a href="#-roadmap"><img src="https://img.shields.io/badge/ROADMAP-FF5C6C?style=flat-square&labelColor=0B0D12"/></a>
</p>

---

<br/>

## 💜 THE VAULT

<div align="center">

| | |
|:---:|:---|
| <img src="https://img.shields.io/badge/NO%20INTERNET%20PERMISSION-0B0D12?style=for-the-badge&logo=wifi&logoColor=FF5C6C&labelColor=0B0D12&borderColor=FF5C6C"/> | **Offline by construction.** The manifest declares no `INTERNET` permission. The OS enforces what a privacy policy can only promise. |
| <img src="https://img.shields.io/badge/NO%20ACCOUNT-0B0D12?style=for-the-badge&logo=user&logoColor=4ADEDE&labelColor=0B0D12"/> | **No cloud. No sync. No telemetry.** Your recordings never leave the device — ever. |
| <img src="https://img.shields.io/badge/NO%20RECOVERY-0B0D12?style=for-the-badge&logo=key&logoColor=E8C468&labelColor=0B0D12"/> | **Forgot your PIN? Data gone.** There is no reset path. That is the feature. |
| <img src="https://img.shields.io/badge/NO%20SCREENSHOTS-0B0D12?style=for-the-badge&logo=camera&logoColor=7C5CFF&labelColor=0B0D12"/> | **`FLAG_SECURE` everywhere.** Screenshots and recents thumbnails are blocked. |

</div>

<br/>

## ✨ FEATURES

<div align="center">

| 🛡️ | 🔐 | 🎙️ |
|:---:|:---:|:---:|
| **End-to-end local encryption** | **PIN + biometric unlock** | **Foreground-service recording** |
| Fresh AES-256-GCM key per file, sealed by Android Keystore (StrongBox). Custom chunked `.enc` format, round-trip verified after every save. | Salted PBKDF2-HMAC-SHA256 PIN (constant-time) + `BIOMETRIC_STRONG` fingerprint. Change PIN anytime in Settings. | `MediaRecorder` → AAC in `.m4a`. Pause/resume, live amplitude waveform, interruption handling. Survives screen-off, calls & process death. |

| ▶️ | 🗂️ | 🎨 |
|:---:|:---:|:---:|
| **Encrypted playback, instant seek** | **Secure vault management** | **Obsidian design system** |
| Custom Media3 `DataSource` decrypts on the fly. Chunk-level random access — seeking a multi-hour file is instant. Temp plaintext is zeroed on stop. | Auto `yyyy/MM/` organization, search, rename, favorite, delete, and encrypted import of external `.m4a` files. | Aurora gradient canvas, glass surfaces, Inter typography, one source of truth for motion. Dark-only by deliberate choice. |

</div>

<br/>

## 🛡️ SECURITY MODEL

<p align="center">
  <i>Four product invariants — non-negotiable, checked at every phase of development.</i>
</p>

<div align="center">

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. OFFLINE BY CONSTRUCTION   no INTERNET permission in the manifest         │
│  2. CIPHERTEXT AT REST        plaintext exists only transiently, then zeroed │
│  3. KEYS IN HARDWARE          Android Keystore / StrongBox — never exported  │
│  4. NOTHING LEAKS             no screenshots, no backups, hidden from OS     │
└─────────────────────────────────────────────────────────────────────────────┘
```

</div>

### 🔑 Key hierarchy — envelope encryption

```
┌───────────────────────────┐      ┌───────────────────────────┐
│  Android Keystore         │      │  Each .enc file           │
│  (StrongBox where avail.) │      │                           │
│                           │      │  ┌─────────────────────┐  │
│  ┌─────────────────────┐  │ wraps │  │  wrapped DEK       │  │
│  │  Master Key         │◄─┼───────┼──┤  (sealed by master)│  │
│  │  AES-256-GCM        │  │      │  └─────────────────────┘  │
│  └─────────────────────┘  │      │  │  NSV1 │ ver │ chunk │  │
│                           │      │  │  noncePrefix │ ...    │  │
│  never leaves hardware    │      │  │  [len][AES-GCM chunk] │  │
└───────────────────────────┘      └───────────────────────────┘
```

Every file gets a **fresh random DEK** → sealed by a **non-exportable Keystore key** → carried in the file header. The fast software cipher does the bulk work; the hardware key only wraps/unwraps DEKs.

### 📦 File format (`.enc` v1)

```text
[magic "NSV1"] [version] [chunkSize] [wrappedKeyLen] [wrappedKey] [noncePrefix 8B]
└─ then framed chunks: [len] [AES-256-GCM(chunk)], 512 KB each
```

- IV = `noncePrefix ‖ counter`, AAD = `[version, isFinalChunk]`
- 🔀 Reordering breaks the counter · ✂️ Truncation breaks the final flag · 🔄 Any bit flip breaks the GCM tag
- 💾 Constant memory for multi-hour files · ⚡ Chunk-level random access for seeking
- ✅ Every save is round-trip verified (SHA-256 during encrypt vs. full decrypt) *before* plaintext temps are zeroed & deleted

### 🎬 Capture pipeline

```text
🎙️ Record (AAC-ADTS temp)  →  🎞️ Remux to .m4a  →  🔒 Encrypt-stream to vault/yyyy/MM/
       →  ✅ Round-trip verify  →  🧹 Securely delete temp  →  🗄️ Insert Room metadata
```

Interrupted captures stay decodable (ADTS is a streaming format) — found at next launch, offered for **recovery, never silently deleted**.

<br/>

## ⚡ TECH STACK

<div align="center">

| Concern | Choice |
|:---|:---|
| 🧠 Language / UI | Kotlin 2.2.21 · Jetpack Compose (BOM 2025.12.01) · Material 3 |
| 🏗️ Architecture | MVVM + Clean Architecture (single `:app` module) |
| 💉 DI | Hilt 2.57.2 |
| 🗄️ Persistence | Room 2.8.4 (metadata) · DataStore (settings) |
| 🧭 Navigation | Navigation Compose 2.9.8 · type-safe `@Serializable` routes |
| 🎙️ Audio capture | `MediaRecorder` → AAC in `.m4a` · foreground service |
| ▶️ Playback | Media3 ExoPlayer 1.8.0 · custom decrypting `DataSource` |
| 🔐 Crypto | Custom chunked AES-256-GCM (`VaultCipher`) — **zero third-party crypto libs** |
| 🛂 Auth | `BiometricPrompt` + PIN |
| 🔨 Build | AGP 8.13.2 · Gradle 8.14.3 · JDK 17 bytecode · KSP |
| 📱 SDK | min 29 (Android 10) · target/compile 36 |
| 🔤 Typography | Inter (bundled — no downloadable fonts, we're offline) |

</div>

<br/>

## 🏗️ ARCHITECTURE

<p align="center">
  <code>feature → domain ← data</code> — dependency rule, enforced by package boundaries
</p>

```text
com.nsvault.app
│
├── 🎯 core/            Result types · dispatcher qualifiers · utilities
├── 🗄️ data/            Recording engine · VaultCipher · Room · encrypted file store · settings
├── 🧠 domain/          Models · one use-case class per user intention
├── 🎨 designsystem/    Theme tokens (Color · Gradient · Type · Motion) + components
├── 🧭 navigation/      Sealed @Serializable routes · NavHost
└── 📱 feature/         auth · home · recorder · player · library · settings
```

> 💡 **Why a single Gradle module?** Module boundaries pay for themselves at team scale and CI caching — neither applies here. Layer discipline is preserved as package boundaries; packages lift into modules mechanically if the app grows.

<br/>

## 🚀 GETTING STARTED

### Prerequisites

- **Android Studio** (latest stable) — bundled JDK, zero config
- **CLI builds** — JDK 17–21 (`JAVA_HOME` set; system JDK 26 is too new for Gradle 8.14)
- `local.properties` → `sdk.dir` (untracked by git)

### Build

```bash
# 🔨 Debug APK
./gradlew :app:assembleDebug

# 📦 Release APK (unsigned unless keystore.properties present)
./gradlew :app:assembleRelease

# 🏪 AAB for Play
./gradlew :app:bundleRelease
```

### Install & run

```bash
./gradlew :app:installDebug
adb shell am start -n com.nsvault.app.debug/.MainActivity
```

### 🔏 Release signing

Signing config is read from `keystore.properties` (git-ignored). Absent → release builds still assemble **unsigned** (R8-verifiable anywhere). Full guide in **[RELEASE.md](RELEASE.md)**: keystore generation, Play App Signing, manual smoke-test checklist.

<br/>

## 🗺️ ROADMAP

<div align="center">

| Phase | Scope | Status |
|:---|:---|:---|
| 1 · Foundation | Gradle · design system · type-safe nav · splash · manifest hardening | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 2 · Authentication | Keystore bootstrap · PIN (4/6) · biometrics · session lock | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 3 · Recording engine | Foreground service · waveform · interruption handling · crash recovery | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 4 · Encryption engine | Streaming AES-GCM · file format · round-trip verification | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 5 · Vault management | Room schema · library UI · search · import · storage stats | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 6 · Playback | Decrypting DataSource · ExoPlayer · waveform seek · share-after-auth | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 7 · Polish | Micro-interactions · haptics · choreography · empty/error states | <img src="https://img.shields.io/badge/DONE-0B0D12?style=for-the-badge&labelColor=0B0D12&color=3DDC84"/> |
| 8 · Hardening | Unit + instrumentation tests · leak checks · baseline profile · R8 audit | <img src="https://img.shields.io/badge/IN%20PROGRESS-0B0D12?style=for-the-badge&labelColor=0B0D12&color=FF5C6C"/> |

</div>

> 📖 Full design document: **[ARCHITECTURE.md](ARCHITECTURE.md)**

<br/>

## ⚠️ KNOWN CONSTRAINTS (by design)

- 📵 **Screenshots blocked** in-app (`FLAG_SECURE`) — including recents thumbnails. Store screenshots must be captured from a debug build.
- ☁️ **No cloud backup / device transfer** — recordings are excluded from Android backup. Factory reset or uninstall erases the vault permanently.
- 🔑 **Forgotten PIN = no access** — no reset or recovery, intentionally. The data is only as recoverable as the PIN.

<br/>

---

<p align="center">
  <img src="https://img.shields.io/badge/PRIVATE%20PROJECT-0B0D12?style=for-the-badge&logo=github&logoColor=7C5CFF&labelColor=0B0D12"/>
  <img src="https://img.shields.io/badge/ALL%20RIGHTS%20RESERVED-0B0D12?style=for-the-badge&logo=shield&logoColor=4ADEDE&labelColor=0B0D12"/>
</p>

<p align="center">
  <i>Built with 💜 for people who take privacy seriously.</i>
</p>
