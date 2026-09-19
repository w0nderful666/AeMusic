# Legal Disclaimer / 免责声明

> **Please read this document before using, modifying, or distributing AeMusic.**
>
> **在使用、修改、二次开发或分发 AeMusic 之前，请阅读本文件。**

---

## 中文版

### 1. 项目性质与独立性

1. **开源 Android 项目**：AeMusic 是一个独立的 Android 开源项目，主要用于播放器架构、Jetpack Compose / Material 3 UI、Media3 播放能力、私有云连接及多来源 Provider 架构的开发与技术交流。
2. **与第三方平台无关联**：AeMusic 与网易、网易云音乐、哔哩哔哩、Bilibili、酷我音乐以及其他第三方服务提供方不存在隶属、合作、授权、赞助或背书关系，除非某项集成另有明确书面说明。
3. **非官方 Provider**：部分实验性 Provider 可能通过第三方 Web/客户端接口、公开可访问端点或兼容性实现完成搜索、元数据展示或媒体地址解析，并不代表第三方平台对这些实现进行了授权、审核或保证。

### 2. 媒体、版权与商标

1. 通过第三方服务访问的音乐、录音、视频、歌词、封面、头像、商标及其他内容，其著作权、邻接权、商标权和其他知识产权归相应权利人或服务提供方所有。
2. AeMusic 的作者和贡献者不因提供客户端代码而对第三方媒体内容主张所有权，也不会通过 Apache-2.0 向用户授予其无权授予的第三方内容、平台接口、账号权益或商标许可。
3. “网易”“网易云音乐”“哔哩哔哩”“Bilibili”“酷我音乐”等名称和标识归其各自权利人所有，仅在必要的兼容性说明和来源标识中使用。

### 3. 客户端架构与本地缓存

1. AeMusic 项目维护者不运营用于第三方媒体内容的中心化代理、转码、中转或托管服务器。客户端在用户主动操作时会直接与相应第三方服务、媒体 CDN 或用户自行配置的 WebDAV / Navidrome 等服务器通信。
2. 为实现播放、断点续播和性能优化，客户端可能使用 Media3 等组件在**用户设备本地**的应用缓存目录保存容量受限的临时媒体缓存。该缓存不构成由 AeMusic 维护者运营的服务器端媒体托管服务。
3. 网络请求会按照实现需要向对应服务发送必要的请求头、会话信息及常规网络元数据。第三方服务对这些数据的处理受其自身隐私政策和服务条款约束。

### 4. 账号凭据与隐私

1. AeMusic 持久化保存的 Provider Cookie / Token 以及受支持的云服务密码等敏感值，使用由 **Android Keystore 管理的不可导出 AES-GCM 密钥**进行本地加密。实际密钥是否由 TEE、StrongBox 或其他硬件安全组件保护，取决于具体设备和 Android 实现。
2. 内嵌网页登录流程可能在登录期间使用 Android WebView / CookieManager 的会话存储机制。为完成认证和用户主动发起的请求，相关凭据或会话信息可能被发送到对应的第三方服务。
3. AeMusic 项目维护者不运营用于收集用户第三方账号凭据的 AeMusic 后端服务器。请勿在 Issue、Pull Request、日志或截图中提交真实 Cookie、Token、密码、私钥或其他敏感信息。

### 5. 第三方服务、账号与可用性风险

1. 第三方接口、网页流程和媒体地址可能随时变化、失效、限频或受到地区、账号、版权、订阅状态以及风控策略影响。
2. 使用者应自行确认其使用方式符合所在地适用法律以及相关第三方服务的用户协议、开发者规则、版权要求和账号使用规则。
3. AeMusic 不提供绕过 DRM、伪造订阅/会员资格或破解付费访问控制的授权。贡献者不得向本仓库提交以规避 DRM、窃取许可证密钥、伪造付费权限或绕过明确访问控制为目的的实现。
4. 对于因第三方服务变更、账号限制、接口不可用、网络费用、数据损失或不当配置产生的损失，责任范围以适用法律及 Apache-2.0 中允许的范围为准。

### 6. 开源许可证与商业使用

1. AeMusic 自有源代码按仓库中的 **Apache License 2.0** 提供。该许可证允许在遵守许可证条件和适用法律的前提下使用、复制、修改和分发代码，包括商业场景。
2. Apache-2.0 **仅适用于 AeMusic 有权许可的代码和材料**。它不授予任何第三方音乐、视频、歌词、封面、商标、账号权益、平台接口或服务的使用权，也不会替代第三方平台的服务条款。
3. AeMusic 官方项目当前免费提供且不包含官方付费解锁或广告业务；这一项目运营选择不构成对 Apache-2.0 下游使用权的额外限制。

### 7. 权利通知与处理

如您是权利人或第三方平台运营方，并认为本仓库中的特定代码、文档、标识或其他材料侵犯了您的合法权益，请通过仓库提供的安全/联系渠道提交足以定位问题的说明。维护者将在收到可核验的信息后进行审查，并根据具体情况采取删除、修改、禁用或其他适当措施。

提交通知时请避免公开披露不必要的个人信息、账号凭据或安全漏洞利用细节。

---

## English Version

### 1. Project Nature and Independence

1. **Open-source Android project**: AeMusic is an independent Android project focused on player architecture, Jetpack Compose / Material 3 UI, Media3 playback, private-cloud connectivity, and pluggable media-provider experiments.
2. **No affiliation**: AeMusic is not affiliated with, authorized by, sponsored by, maintained by, or endorsed by NetEase, NetEase Cloud Music, Bilibili, Kuwo, or other third-party service providers unless an integration explicitly states otherwise in writing.
3. **Unofficial provider integrations**: Experimental providers may interact with web-facing/client-facing endpoints, publicly reachable service endpoints, or compatibility implementations. Their presence in this repository does not imply approval or authorization by the relevant platform.

### 2. Media, Copyright, and Trademarks

1. Music, recordings, videos, lyrics, artwork, portraits, trademarks, and other materials accessed from third-party services remain subject to the rights of their respective owners and service providers.
2. The AeMusic authors and contributors do not claim ownership of third-party media content and cannot grant rights they do not own merely because the AeMusic source code is licensed under Apache-2.0.
3. Third-party names and marks are used only as necessary to identify compatibility or source integrations and remain the property of their respective owners.

### 3. Client Architecture and Local Caching

1. The AeMusic maintainers do not operate a centralized media proxy, transcoding relay, or hosted streaming backend for third-party media. At the user's direction, the client communicates directly with relevant third-party services, media CDNs, or user-configured WebDAV / Navidrome servers.
2. For playback continuity and performance, Media3 or related components may store **temporary, size-limited media cache files on the user's device**. This local application cache is not a server-side media hosting service operated by the AeMusic maintainers.
3. Requests may include headers, session information, and ordinary network metadata required by the relevant integration. Third-party services process that data under their own terms and privacy policies.

### 4. Credentials and Privacy

1. Persisted provider cookies/tokens and supported cloud-service passwords are encrypted locally using AES-GCM with a non-exportable key managed by **Android Keystore**. Whether the key is backed by TEE, StrongBox, or other hardware security depends on the device and Android implementation.
2. Embedded web login flows may use Android WebView / CookieManager session storage while authentication is in progress. Credentials or session information may be transmitted to the corresponding third-party service when required for authentication or a user-initiated request.
3. The AeMusic maintainers do not operate an AeMusic backend designed to collect users' third-party account credentials. Never include real cookies, tokens, passwords, private keys, or other secrets in Issues, Pull Requests, logs, or screenshots.

### 5. Third-Party Service and Account Risks

1. Third-party APIs, web flows, and media URLs may change, fail, throttle requests, or vary based on region, account status, subscription status, copyright availability, and anti-abuse controls.
2. Users and downstream developers are responsible for ensuring that their use complies with applicable law and with the relevant third-party terms, developer rules, copyright restrictions, and account policies.
3. AeMusic does not grant permission to bypass DRM, forge subscription or membership status, steal license keys, or circumvent explicit access controls. Contributions primarily intended to perform such circumvention are not accepted by this project.
4. To the extent permitted by applicable law and the Apache-2.0 license, the project is provided without warranties regarding third-party availability, account status, network cost, or fitness for a particular purpose.

### 6. Open-source License and Commercial Use

1. AeMusic-owned source code is licensed under the repository's **Apache License 2.0**. Subject to that license and applicable law, the code may be used, modified, and redistributed, including in commercial contexts.
2. Apache-2.0 applies only to materials AeMusic is entitled to license. It does **not** grant rights to third-party music, video, lyrics, artwork, trademarks, account entitlements, APIs, or services, and it does not replace a third party's terms of service.
3. The official AeMusic project is currently distributed free of charge and does not provide an official paid-unlock or advertising business. That project choice does not add a non-commercial restriction to the Apache-2.0 license.

### 7. Rights Notices

If you are a rights holder or service operator and believe that specific code, documentation, marks, or other repository materials infringe your rights, please use the repository's available security/contact channel and provide enough information to identify the material at issue. The maintainers will review verifiable notices and take appropriate action based on the circumstances, which may include modification, disabling, or removal.

Please avoid publicly disclosing unnecessary personal information, account credentials, or security exploit details when submitting a notice.
