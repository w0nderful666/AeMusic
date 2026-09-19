# Contributing to AeMusic / 贡献与二次开发指南

感谢您关注 AeMusic。欢迎通过 Issue 和 Pull Request 改进 Android 架构、播放器体验、UI、稳定性、测试和 Provider 兼容性。

提交代码即表示您理解：仓库中的 AeMusic 自有代码按 Apache License 2.0 提供；同时，第三方平台内容、接口、商标及账号权益仍受其各自规则和适用法律约束。

---

## 🧭 当前源码结构

```text
app/src/main/java/com/aemusic/
├── app/                   # Application / AppContainer 与依赖装配
├── core/
│   ├── backup/            # 设置备份
│   ├── cloud/             # WebDAV / Navidrome 连接
│   ├── data/              # Repository / Store
│   ├── database/          # Room
│   ├── diagnostics/       # 诊断事件
│   ├── model/             # 领域模型
│   └── network/           # 网络客户端与媒体探测
├── design/                # Compose 组件、效果、图标、Theme
├── feature/               # home / library / player / playlist / search / settings / shell
├── playback/              # Media3 播放服务、PlaybackConnection、缓存偏好
└── provider/
    ├── account/           # ProviderCredentialStore
    ├── bilibili/
    ├── kuwo/
    ├── lyrics/
    └── netease/
```

Provider 公共契约位于：

`app/src/main/java/com/aemusic/provider/ProviderContracts.kt`

当前核心类型：

- `MusicProvider`：所有 Provider 的基础契约，提供 `ProviderDescriptor`。
- `SearchProvider`：实现 `suspend fun search(...)`。
- `StreamProvider`：实现 `suspend fun resolve(...)` 并返回 `ResolvedStream`。
- `LoginProvider`：声明网页登录入口、Cookie 作用域及凭据验证逻辑。
- `ProviderRegistry`：根据能力组织 Provider。
- `PlaybackResolver`：将 `PlaybackReference` 解析为本地或 Provider 播放流。

---

## 🛠️ 新增 Provider

### 1. 只实现真正需要的能力

在 `com.aemusic.provider.<source>` 下创建实现，并根据功能选择接口。例如：

```kotlin
class ExampleProvider(...) : SearchProvider, StreamProvider {
    override val descriptor = ProviderDescriptor(...)

    override suspend fun search(
        query: String,
        limit: Int,
    ): ProviderResult<List<Track>> = ...

    override suspend fun resolve(
        reference: PlaybackReference.Provider,
        quality: StreamQuality,
    ): ProviderResult<ResolvedStream> = ...
}
```

如果来源需要账号登录，再实现 `LoginProvider`。不要为了满足统一模板而实现实际不存在的能力。

### 2. 注册 Provider

目前 Provider 在 `app/AppContainer.kt` 中实例化，并传入 `ProviderRegistry`：

```kotlin
val providerRegistry = ProviderRegistry(
    listOf(bilibiliProvider, neteaseProvider, kuwoProvider)
)
```

新增 Provider 后，应同时检查搜索、播放解析以及需要该 Provider 的上层 Repository / UI 是否需要显式接入。

### 3. 凭据安全

需要 Cookie / Token / Password 的功能应复用：

`provider/account/ProviderCredentialStore.kt`

当前实现使用 Android Keystore 管理的不可导出 AES 密钥，并以 AES-GCM 加密持久化值。

贡献时请遵守：

- 禁止在源码、测试、Issue、PR、日志或截图中提交真实 Cookie、Token、密码、API Key、签名私钥或其他凭据。
- 不要新增明文持久化敏感凭据的普通 `SharedPreferences`、数据库列或日志。
- 不要假设所有 Android 设备都具有 StrongBox / TEE；如果功能依赖特定安全级别，应显式检测并处理降级。
- 只向确实需要认证的请求发送账号凭据，避免将完整 Cookie/Token 扩散到不必要的域名或媒体请求。

### 4. 第三方服务与访问边界

Provider 属于实验性兼容功能。提交新的第三方来源时：

- 优先使用服务提供方正式公开并允许的 SDK / API（如适用）。
- 在使用 Web/客户端接口或其他兼容性实现时，应在代码和文档中准确描述其性质，不要将其宣传成“官方 API”或“官方授权”。
- 不提交以绕过 DRM、窃取许可证密钥、伪造付费/会员资格、规避明确访问控制为主要目的的实现。
- 不提交隐藏爬虫、批量采集、账号撞库、验证码规避或其他明显扩大滥用能力的代码。
- 对请求频率、错误码、认证失效和平台限频做保守处理，避免不必要的高频访问。

---

## ✅ Pull Request 检查项

提交 PR 前请尽量完成：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleInternal
```

同时检查：

- 没有提交 `local.properties`、keystore、私钥、`.env`、真实凭据或本机绝对路径。
- 新增日志不包含 Cookie、Token、密码、完整授权头或其他敏感数据。
- 网络与磁盘 IO 不应无必要地阻塞主线程。
- Compose 中避免长期持有 Activity / View 引用；自建协程应具有明确生命周期。
- UI 优先复用 `com.aemusic.design` 下已有组件和主题 Token。
- 新功能应补充必要的单元测试或可复现说明。
- 文档中的能力描述应与实际源码保持一致，尤其避免“官方”“绝对安全”“绝不缓存”等无法由实现保证的绝对化表述。

---

## 🔐 安全问题

安全漏洞、凭据泄露或可导致账号风险的问题请先阅读 [SECURITY.md](SECURITY.md)。不要在公开 Issue 中提交真实秘密或完整可利用细节。
