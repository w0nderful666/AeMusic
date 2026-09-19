# AeMusic (吟光音乐) — Preview

<p align="center">
  <strong>A modern Android music player built with Jetpack Compose, Material 3, Media3, and customizable Liquid Glass visual effects.</strong>
</p>

<p align="center">
  <a href="README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v0.1.0--preview-blue.svg" alt="Release" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.x-purple.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Core-Media3%20ExoPlayer-orange.svg" alt="Media3" />
  <img src="https://img.shields.io/badge/Security-Android%20Keystore%20Encrypted-red.svg" alt="Security" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg" alt="License" />
</p>

---

> [!IMPORTANT]
> **Project status**
>
> AeMusic is currently in its **initial public preview / beta stage**. This release is intended for architecture validation, UI/interaction experimentation, and open-source community feedback. Behavior may vary across Android versions, devices, and vendor ROMs. Experimental third-party integrations may change or stop working without notice.

## 🌟 Features

### 🎨 Material 3 and Liquid Glass
- **Modern Android UI** built with Jetpack Compose and Material 3.
- **Experimental Liquid Glass styling** with dynamic highlights, blur/refraction-like effects, and soft shadows.
- **Dynamic Canvas** that derives background palettes from the current artwork and adapts to light, dark, and OLED-oriented themes.
- **Motion-focused interactions** using Compose animations and spring transitions throughout the player, playlists, and bottom dock.

### 🎵 Local playback and experimental multi-source support
- **Local playback** for common audio formats supported by Android / Media3, with media-library scanning.
- **Experimental third-party providers**:
  - **Bilibili**: experimental search, favorites import, and playable media-stream resolution. Availability and quality depend on the platform response, account state, and region.
  - **NetEase Cloud Music**: account credential integration, selected playlist/recommendation features, and playable stream resolution. Availability and quality depend on account permissions and platform behavior.
  - **Kuwo Music**: experimental search and playable URL resolution.
- **Cross-source queueing** for mixing local and online entries in one playback queue.

> [!CAUTION]
> Third-party providers are compatibility and technical-research features. They are not official clients or official SDK integrations for the corresponding services. Users are responsible for complying with applicable law and the relevant service terms, copyright rules, and account policies.

### 📜 Synchronized lyrics
- Retrieves available lyrics from sources such as NetEase Cloud Music and LRCLIB.
- Supports matching, line highlighting, scrolling, and tap-to-seek behavior.

### ☁️ Private cloud and self-hosted media
- **WebDAV** connectivity for supported personal storage and synchronization scenarios.
- **Navidrome / Subsonic-compatible services** for servers you operate or are authorized to access.

### 🔐 Security and privacy
- **Encrypted local credential storage**: persisted provider cookies/tokens and supported cloud-service passwords are encrypted using AES-GCM with a non-exportable key managed by Android Keystore. Actual hardware-backed protection depends on the device and Android implementation.
- **No AeMusic relay backend**: the project does not operate a media proxy, media relay, or credential-collection server. User-initiated login, sync, search, and playback requests are made directly to the corresponding service or user-configured server.
- **Local playback cache**: Media3 may keep size-limited temporary media cache files inside the app's local cache directory to improve playback continuity. This is not server-side media hosting operated by the AeMusic maintainers.

---

## 🏗️ Architecture

AeMusic currently uses a single `:app` module organized by feature and infrastructure responsibility:

```text
app/src/main/java/com/aemusic/
├── app/                   # Application / AppContainer and app-level wiring
├── core/
│   ├── backup/            # Settings backup
│   ├── cloud/             # WebDAV / Navidrome integrations
│   ├── data/              # Repositories and stores
│   ├── database/          # Room entities, DAOs, migrations
│   ├── diagnostics/       # Diagnostic events and logging helpers
│   ├── model/             # Domain models
│   └── network/           # OkHttp access and media probing
├── design/                # Compose components, visual effects, icons, theme
├── feature/               # home / library / player / playlist / search / settings / shell
├── playback/              # Media3 service, PlaybackConnection, cache preferences
└── provider/
    ├── account/           # ProviderCredentialStore (Android Keystore + AES-GCM)
    ├── bilibili/          # Bilibili provider
    ├── kuwo/              # Kuwo provider
    ├── lyrics/            # Multi-source lyric aggregation
    └── netease/           # NetEase provider
```

Provider contracts live in `provider/ProviderContracts.kt`, including:

- `MusicProvider`
- `SearchProvider`
- `StreamProvider`
- `LoginProvider`
- `ProviderRegistry`
- `PlaybackResolver`

See [CONTRIBUTING.md](CONTRIBUTING.md) for implementation and contribution details.

---

## 🚀 Building

### Requirements
- **JDK:** 17 or newer
- **Android SDK:** compileSdk / targetSdk 37, minSdk 26
- **Gradle:** use the included Gradle Wrapper

### Local builds

```bash
git clone https://github.com/w0nderful666/AeMusic.git
cd AeMusic

# Unit tests
./gradlew testDebugUnitTest

# Development build
./gradlew assembleDebug

# Release-like preview build:
# R8 minification + optimization + resource shrinking + debug signing
./gradlew assembleInternal
```

Typical output paths:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- R8 Internal APK: `app/build/outputs/apk/internal/app-internal.apk`

### GitHub Actions builds

Every push to `main`, every pull request targeting `main`, and manual workflow dispatch builds two APK variants:

| Variant | R8 / minification | Resource shrinking | Signing | Intended use |
| --- | --- | --- | --- | --- |
| Debug | No | No | Android debug key | Development and diagnostics |
| Internal | **Yes** | **Yes** | Android debug key | Performance testing and preview distribution |

The Internal variant inherits the Release build configuration, including `proguard-android-optimize.txt`, but uses a debug signing key so the public repository does not need a production signing secret.

> [!NOTE]
> This repository does not contain production signing keys. If you publish your own production release, generate and protect your own release signing key and never commit it to the repository.

---

## 🤝 Contributing

Issues and pull requests are welcome. Before adding or modifying a third-party provider, read [CONTRIBUTING.md](CONTRIBUTING.md) for provider contracts, credential-handling requirements, and integration boundaries.

For vulnerabilities that may affect credentials, account security, or remote content loading, read [SECURITY.md](SECURITY.md) first. Do not paste real cookies, tokens, passwords, or exploitable secrets into a public issue.

---

## ⚠️ Legal disclaimer

Please read the full [DISCLAIMER.md](DISCLAIMER.md) before building, using, modifying, or distributing AeMusic.

In short:

1. **Independent project:** AeMusic is not affiliated with, authorized by, sponsored by, or endorsed by NetEase, Bilibili, Kuwo, or other third-party service providers.
2. **Third-party rights remain with their owners:** audio, video, lyrics, artwork, metadata, trademarks, and account entitlements remain subject to their respective rights holders and service providers.
3. **No maintainer-operated media relay:** AeMusic maintainers do not operate a third-party media proxy or relay. The client may use temporary on-device playback caching.
4. **Third-party service rules still apply:** experimental providers may rely on web/client-facing endpoints or compatibility implementations and may change, become restricted, or stop working.
5. **Rights notices:** rights holders or platform operators may use the repository's available contact/security channels to identify specific repository materials they believe require review.

---

## 📄 License

AeMusic-owned source code is provided under the [Apache License 2.0](LICENSE). Subject to that license and applicable law, the code may be used, modified, and redistributed, including in commercial contexts.

Apache-2.0 does **not** grant rights to third-party music, video, lyrics, artwork, trademarks, account entitlements, APIs, or services. Third-party libraries and materials remain subject to their own licenses, terms, and rights notices.
