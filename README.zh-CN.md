# AeMusic (吟光音乐) — Preview

<p align="center">
  <strong>一款基于 Jetpack Compose 构建的现代化 Android 音乐播放器。</strong>
  <br />
  <em>A modern Android music player built with Jetpack Compose, Material 3, Media3, and customizable Liquid Glass visual effects.</em>
</p>

<p align="center">
  <a href="README.md">English</a> · 简体中文
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
> **当前版本说明**
>
> 本项目当前处于 **初代公开预览测试阶段 (Initial Public Preview / Beta)**。功能主要用于技术架构验证、UI 动效探索与开源社区交流。不同设备、系统版本及定制 ROM 上可能存在表现差异，第三方平台接口也可能随时发生变化。

## 🌟 核心特性

### 🎨 Material 3 与 Liquid Glass
- **Material 3 / Android 原生设计语言**：以 Jetpack Compose 与 Material 3 为基础构建设计系统。
- **Liquid Glass 视觉效果**：可在标准 Material 材质与实验性玻璃质感之间切换，包含动态高光、折射/模糊及柔和阴影效果。
- **动态背景**：根据当前封面生成背景色板，并针对亮色、暗色及 OLED 模式调整表现。
- **手势与过渡动画**：播放器、歌单与底部 Dock 使用 Compose 动画和弹簧过渡实现交互效果。

### 🎵 本地播放与实验性多源支持
- **本地音乐播放**：支持 Android / Media3 可解码的常见音频格式，并通过系统媒体库扫描本地音乐。
- **实验性第三方来源**：
  - **哔哩哔哩 (Bilibili)**：提供搜索、收藏夹导入及可用媒体流解析等实验性能力；实际音质与可用性取决于平台返回结果、账号状态及地区。
  - **网易云音乐**：支持账号凭据接入、部分歌单/推荐能力及可用播放流解析；实际可用音质取决于平台、账号权限及接口状态。
  - **酷我音乐**：提供实验性的搜索与可用播放地址解析。
- **跨源队列**：本地与在线条目可以在同一播放队列中管理。

> [!CAUTION]
> 第三方 Provider 属于兼容性与技术研究功能，并非对应平台的官方客户端或官方 SDK 集成。使用者应遵守所在地法律以及相应第三方服务的服务条款、版权规则和账号使用规则。

### 📜 多源歌词
- 支持从网易云音乐、LRCLIB 等来源获取可用歌词数据。
- 提供歌词匹配、逐句高亮、滚动和点击 Seek 等能力。

### ☁️ 私有云与自建媒体服务
- **WebDAV**：可连接兼容 WebDAV 的个人存储服务，用于应用支持的数据同步场景。
- **Navidrome / Subsonic**：支持连接用户自行部署或有权访问的兼容服务。

### 🔐 隐私与凭据保护
- **本地加密存储**：Provider Cookie / Token 以及云服务密码等敏感值使用由 Android Keystore 管理的不可导出 AES-GCM 密钥进行本地加密。实际硬件安全级别取决于设备实现与系统能力。
- **无 AeMusic 中转服务器**：项目本身不运营媒体代理、媒体中转或账号凭据收集服务器。用户主动发起的登录、同步、搜索与播放请求直接与相应第三方服务或用户配置的服务器通信。
- **本地播放缓存**：Media3 播放链路可能在设备的应用缓存目录保存有容量限制的临时媒体缓存，用于改善连续播放体验；项目不提供由维护者运营的服务器端媒体存储或中继服务。

---

## 🏗️ 架构概览

```text
app/src/main/java/com/aemusic/
├── app/                   # Application / AppContainer 等应用级装配
├── core/
│   ├── backup/            # 设置备份相关逻辑
│   ├── cloud/             # WebDAV / Navidrome 等云连接
│   ├── data/              # Repository、Store 与业务数据访问
│   ├── database/          # Room 数据库实体、DAO 与迁移
│   ├── diagnostics/       # 诊断事件与日志辅助
│   ├── model/             # 领域模型
│   └── network/           # OkHttp 网络访问与媒体探测
├── design/                # Compose 组件、视觉效果、图标与主题
├── feature/               # home / library / player / playlist / search / settings / shell
├── playback/              # Media3 / ExoPlayer 播放服务、连接与缓存偏好
└── provider/
    ├── account/           # ProviderCredentialStore（Android Keystore + AES-GCM）
    ├── bilibili/          # Bilibili Provider
    ├── kuwo/              # Kuwo Provider
    ├── lyrics/            # 多来源歌词聚合
    └── netease/           # NetEase Provider
```

Provider 公共契约位于 `provider/ProviderContracts.kt`，当前主要包括 `MusicProvider`、`SearchProvider`、`StreamProvider`、`LoginProvider`、`ProviderRegistry` 和 `PlaybackResolver`。

更多说明见 [CONTRIBUTING.md](CONTRIBUTING.md)。

---

## 🚀 构建

### 环境要求
- **JDK**：17 或更高版本
- **Android SDK**：compileSdk / targetSdk 37，minSdk 26
- **Gradle**：建议直接使用仓库内置 Gradle Wrapper

### 本地构建

```bash
git clone https://github.com/w0nderful666/AeMusic.git
cd AeMusic

# 单元测试
./gradlew testDebugUnitTest

# Debug 开发包
./gradlew assembleDebug

# Release-like Preview：
# R8 压缩/优化 + 资源压缩 + Debug 签名
./gradlew assembleInternal
```

常见产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- R8 Internal APK：`app/build/outputs/apk/internal/app-internal.apk`

### GitHub Actions

每次推送到 `main`、针对 `main` 的 Pull Request，以及手动运行工作流时，会分别构建：

| 变体 | R8 / 混淆优化 | 资源压缩 | 签名 | 用途 |
| --- | --- | --- | --- | --- |
| Debug | 否 | 否 | Android debug key | 开发、诊断 |
| Internal | **是** | **是** | Android debug key | 性能测试、Preview 分发 |

Internal 继承 Release 配置并使用 `proguard-android-optimize.txt`，但采用 Debug 签名，因此公开仓库无需保存正式生产签名密钥。

> [!NOTE]
> 仓库不包含任何私有生产签名文件。如需发布自己的正式构建，请自行生成并妥善保管 Release signing key，且不要提交到仓库。

---

## 🤝 二次开发与贡献

欢迎提交 Issue 和 Pull Request。新增或修改第三方 Provider 前，请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

如发现可能影响用户凭据、账号安全或远程内容加载的安全问题，请优先阅读 [SECURITY.md](SECURITY.md)，不要在公开 Issue 中直接粘贴真实 Cookie、Token、密码或可利用细节。

---

## ⚠️ 免责声明

在编译、使用、修改或分发本项目之前，请阅读完整的 [DISCLAIMER.md](DISCLAIMER.md)。

简要说明：

1. **独立开源项目**：AeMusic 与网易、哔哩哔哩、酷我等第三方服务提供方不存在隶属、授权、赞助或背书关系。
2. **版权归权利人所有**：通过第三方服务访问的音频、视频、歌词、封面和元数据，其相关权利归对应权利人及服务提供方所有。
3. **无维护者媒体中转服务**：AeMusic 维护者不运营媒体代理或中转服务器；客户端可能在用户设备本地使用临时播放缓存。
4. **第三方服务规则**：实验性 Provider 可能使用第三方 Web/客户端接口或兼容性实现，接口可能变化、限制或失效。使用者及二次开发者应自行确认其使用方式符合适用法律及服务条款。
5. **权利通知**：权利人或平台运营方如认为仓库中的代码、文档或其他材料侵犯其合法权益，可通过仓库提供的联系渠道提交说明，维护者将对具体问题进行核验并采取适当措施。

---

## 📄 开源许可证

AeMusic 源代码按照 [Apache License 2.0](LICENSE) 提供。Apache-2.0 允许在遵守许可证条款和相关法律的前提下使用、修改和分发代码，包括商业场景；第三方平台内容、接口、商标及服务本身**不因本仓库采用 Apache-2.0 而获得授权**。

第三方库及其他材料仍分别受其各自许可证、服务条款或权利声明约束。
