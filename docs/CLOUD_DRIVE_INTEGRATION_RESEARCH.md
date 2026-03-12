# 远端媒体源接入研究

研究时间：2026-03-12  
研究分支：`codex/research-webdav-cloud-drives`

## 目标

评估 Asuka Player 接入以下来源的可行性与推荐顺序：

- WebDAV
- 百度网盘
- 阿里云盘
- 123 网盘

重点不是“能否播放一个远端 URL”，而是“能否在当前架构里做成可维护的账号接入、目录浏览、文件选择、续播与最近播放”。

## 结论

- 第一优先级：`WebDAV`
  - 标准协议，适合先做一套通用远端浏览和播放基础设施。
  - 一次实现可覆盖通用 WebDAV 服务器，也可能覆盖部分已提供 WebDAV 挂载能力的网盘产品。
- 第二优先级：`阿里云盘`
  - 官方开放能力最完整，公开可见的开发者文档也最清晰。
  - 适合在 WebDAV 基础设施做完后，作为第一个专有云盘来源接入。
- 保守优先级：`123 网盘`
  - 官方客户端能力上已明确支持 WebDAV 挂载和 URL 直链。
  - 但公开可见的专有 OpenAPI 文档在本次调研中不够稳定、可索引性较差。
  - 因此更适合先通过 WebDAV 或用户直链方式支持，而不是第一期直接绑定专有 API。
- 暂缓：`百度网盘`
  - 官方确实存在 OAuth / PCS 类开放能力，但公开索引到的资料不足以证明个人开发者可以稳定获得“目录浏览 + 可播放视频直链/转码流”这整条能力。
  - 在没有真实开发者应用验证前，不建议直接排实现期。

## 当前仓库的真实适配点

### 已经具备的能力

- 播放链路已经支持任意 `uri` 进入播放队列。
  - `app/src/main/java/com/asuka/player/app/Models.kt`
  - `player-platform/src/main/java/com/asuka/player/platform/PlaybackQueueMediaItems.kt`
- “打开网络串流”已支持手动输入 `http/https/rtsp` 类 URL 直接播放。
  - `app/src/main/java/com/asuka/player/app/OpenNetworkStreamDialog.kt`
  - `app/src/main/java/com/asuka/player/app/MainLibraryCatalogStore.kt`
- `PlaybackQueueEntry` 已经把 `mediaId` 和 `uri` 分离，这为“稳定业务 ID + 临时下载 URL”留下了设计空间。
  - `player-contract/src/main/java/com/asuka/player/core/PlaybackQueueEntry.kt`

### 目前会卡住远端来源的点

- 媒体库架构目前只有“本地 MediaStore 索引”这一路。
  - `AndroidMediaStoreVideoCatalogDataSource` 和 `AndroidMediaLibraryRepository` 都是本地目录模型。
  - 远端来源不能硬塞进现有 `MediaStore` 分页链路，应该并行增加一套 remote catalog 能力。
- 最近播放页当前只把 `content/file/http/https/rtsp` 识别为可直接回放来源。
  - 如果远端来源的稳定 `mediaId` 采用自定义 scheme，例如 `aliyundrive:...` / `webdav:...`，当前 recent 页面会把它们视为不可播放。
  - 相关代码：
    - `app/src/main/java/com/asuka/player/app/LibraryRecentPage.kt`
- 当前 `PlaybackQueueEntry` 只有 `mediaId + uri`，没有请求头、cookie、过期时间、刷新策略等表达能力。
  - 这意味着：
    - 如果云盘播放依赖 `Authorization` header，当前模型不够用。
    - 如果云盘返回的是短期签名 URL，续播和 recent 回放前必须先做 URL 重新解析。
- 项目里目前没有现成的网络层和 XML/JSON 解析层依赖。
  - `gradle/libs.versions.toml` 和现有模块代码里看不到 `okhttp` / `retrofit` / `ktor` / XML 解析库。
  - 做 WebDAV / 云盘 API 前，需要先补一层统一网络栈。

## 外部能力调研

### 1. WebDAV

#### 能力结论

- WebDAV 是标准协议，核心来自 RFC 4918。
- 目录浏览依赖 `PROPFIND`。
- 建目录、移动、删除、锁等能力由 `MKCOL` / `MOVE` / `DELETE` / `LOCK` 等方法定义。
- 对播放器来说，最重要的是：
  - 可以列目录
  - 可以拿到资源 URL
  - 资源本身通常仍然是基于 HTTP 的可拉流内容

#### 对本项目的意义

- 最适合做第一期通用远端源。
- 最小 MVP 可以只做：
  - 保存服务器地址 + 用户名/密码
  - 浏览目录
  - 过滤视频文件
  - 点击后直接播放
- 如果服务端支持 Range，请求层通常就能自然支持 seek。

#### 风险

- 不同服务端在 `PROPFIND` 返回字段、认证方式、路径规范上会有差异。
- 若服务端只接受 Basic/Digest/Session Cookie，而不是裸公开 URL，则播放器层必须支持附带请求头。

### 2. 阿里云盘

#### 能力结论

- 官方公开了开发者门户和网盘与相册服务文档。
- 公开资料可见的能力包括：
  - OAuth / 用户 token
  - 文件列表
  - 文件下载 URL
  - 视频预览 / 播放信息
- 这意味着阿里云盘是四类来源里最适合做“官方专有 API 接入”的对象。

#### 对本项目的意义

- 适合在 WebDAV 基础设施后落第二阶段：
  - 账号登录
  - drive/file 浏览
  - 视频文件筛选
  - 直链或预览流播放
  - 刷新 token 和短效播放链接刷新

#### 风险

- URL 很可能是短效的，不能把临时下载链接直接当稳定 `mediaId`。
- 如果官方推荐的是预览流而不是永久下载地址，播放器在重复进入 recent 时需要先重新解析文件播放地址。

### 3. 百度网盘

#### 能力结论

- 官方开放平台和 OAuth 入口仍然存在，PCS 服务也仍能在公开服务页看到。
- 但本次调研中，公开可索引资料对“个人开发者是否可稳定使用网盘文件浏览、分享、视频播放相关 API”给出的信号不够乐观。
- 官方域名下可索引的开发者问答中，仍能看到“部分文件分享类 API 不对个人开发者开放”的说法。

#### 对本项目的意义

- 百度网盘不能按“像阿里云盘一样直接接 API”来做排期。
- 更合理的前置条件是先验证：
  - 当前是否还能成功申请到可用的网盘开放平台应用
  - 能否拿到目录浏览能力
  - 能否获得可直接用于 Media3 播放的 URL 或官方视频预览流
  - 这些能力是否对个人开发者开放，还是要求企业资质

#### 风险

- 产品和开放平台能力边界不透明。
- 公开资料索引不稳定，集成风险高于编码复杂度本身。
- 如果最终只能拿到网页版分享页而不是可控 API，就不适合进入当前架构。

### 4. 123 网盘

#### 能力结论

- 官方客户端公开描述里已经出现：
  - WebDAV 挂载
  - URL 直链
- 这说明 123 网盘对播放器最关键的两类能力已经具备产品级支撑。
- 但本次调研中，公开可见的专有 OpenAPI 文档并不稳定，难以作为第一期工程方案的可靠依据。

#### 对本项目的意义

- 第一阶段最稳的接入方式不是“123 网盘专有 API”，而是：
  - 用户把 123 网盘当成 WebDAV 服务接入
  - 或者用户直接粘贴 123 网盘生成的可播直链

#### 风险

- 如果后续要做 123 网盘账号直登，就需要重新核实：
  - 是否存在稳定开放平台
  - 接口是否公开
  - 是否允许第三方播放器场景

## 推荐接入顺序

1. `WebDAV`
2. `阿里云盘`
3. `123 网盘（优先 WebDAV/直链，暂不绑定专有 API）`
4. `百度网盘（完成资质与 API 可用性验证后再决定）`

## 建议的架构演进

### A. 不要改动的边界

- 不要把远端来源塞进 `AndroidMediaStoreVideoCatalogDataSource`。
- 不要让 `player-contract` 直接感知具体云盘类型。
- 不要把账号登录、token 刷新逻辑散落到 Compose 页面里。

### B. 建议新增的 app 层能力

- 新增一个独立的 remote source feature slice，建议仍放在 `app` 模块起步：
  - `RemoteAccount`
  - `RemoteProviderId`
  - `RemoteCatalogNode`
  - `RemoteCatalogRepository`
  - `BrowseRemoteCatalogUseCase`
  - `ResolveRemotePlaybackUseCase`
- 本地库和远端库保持平行关系：
  - local: `MediaStore -> index db -> repository -> use case`
  - remote: `provider api/webdav -> repository -> use case`

### C. 播放模型需要补的表达能力

当前 `PlaybackQueueEntry(mediaId, uri)` 对远端来源不够完整，至少要补其中一种方案：

- 方案 1：扩展 `PlaybackQueueEntry`
  - 增加 headers / resolvedAt / expiresAt / sourceType 等字段
- 方案 2：保持 `PlaybackQueueEntry` 简单，但在进入播放前统一做一次“远端可播地址解析”
  - `mediaId` 始终保持稳定业务 ID
  - `uri` 在真正启动播放前被替换成新鲜的可播 URL

当前仓库更适合先做方案 2，因为它对 `player-contract` 的侵入更小。

### D. 最近播放要同步升级

远端来源一定要避免使用临时 URL 当 `mediaId`。

建议规则：

- WebDAV 文件：`webdav:{accountId}:{normalizedPath}`
- 阿里云盘文件：`aliyundrive:{driveId}:{fileId}`
- 百度网盘文件：`baidunetdisk:{fsid or path}`
- 123 网盘文件：`pan123:{accountId}:{fileId or path}`

然后给 recent 页面增加“按 provider 解析 descriptor”的能力，而不是只认 `http/https/file/content`。

### E. 账号和凭据存储

需要新增安全存储层，至少覆盖：

- access token
- refresh token
- WebDAV 用户名/密码
- endpoint / drive id / account metadata

建议不要直接复用当前普通 settings store。远端账号凭据应单独建 store，并使用系统安全能力保护。

## 分阶段实施建议

### Phase 0: 基础设施

- 引入统一网络层
- 增加远端账号存储
- 增加远端来源模型和 repository 契约
- 增加“稳定 mediaId -> 临时可播 URL”解析链路
- recent 页增加 provider-aware 展示和回放恢复

### Phase 1: WebDAV MVP

- 手动添加 WebDAV 服务器
- 浏览目录
- 仅展示视频文件
- 直接播放
- recent / 续播打通

### Phase 2: 阿里云盘

- 登录与 token 刷新
- 文件列表与分页
- 获取播放 URL / 预览流
- 失效链接自动重解析

### Phase 3: 123 网盘

- 优先支持：
  - WebDAV
  - 用户粘贴直链
- 如果后续拿到稳定专有 API，再补账号直连

### Phase 4: 百度网盘

- 先做真实开发者资质验证
- 再决定是否投入编码

## 验证重点

如果后续开始实现，测试重点应补到这些层：

- WebDAV / 云盘响应解析单测
- token 刷新和链接过期重试单测
- recent 对自定义 provider `mediaId` 的展示与恢复测试
- 启动播放前的 remote resolve 流程测试
- 失效 URL 二次解析后的 seek / resume 行为测试

## 本次调研引用

- WebDAV 标准：<https://www.rfc-editor.org/rfc/rfc4918>
- 百度开放服务页：<https://openapi.baidu.com/service>
- 百度 OAuth 授权入口：<https://openapi.baidu.com/oauth/2.0/authorize>
- 百度 OAuth token 入口：<https://openapi.baidu.com/oauth/2.0/token>

## 调研备注

- 阿里云盘公开资料结论来自其官方开发者门户和网盘与相册服务公开文档。
- 123 网盘“WebDAV / URL 直链”结论来自官方客户端公开能力描述；但其专有 OpenAPI 文档在本次调研中没有形成足够稳定的公开引用，因此本文件对 123 网盘 API 接入保持保守判断。
- 百度网盘开放平台相关资料的公开索引稳定性一般，因此本文件明确把“先做真实应用验证”列为编码前置条件，而不是直接承诺可接入。
