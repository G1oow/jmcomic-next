<p align="center">
  <img src="app/src/main/res/mipmap-hdpi/logo_round.webp" width="128" alt="JMcomic Next logo">
</p>

<h1 align="center">JMcomic Next</h1>

<p align="center">用 Jetpack Compose 重写的 JM 漫画客户端 · 手机端 + Wear OS 手表端</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-6.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 6.0+">
  <img src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.4.21">
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/license-GPL--3.0-green" alt="GPL-3.0">
</p>

> [!WARNING]
> 本项目包含 NSFW 内容。请根据自身情况与当地法律法规谨慎使用。

JMcomic Next 是一个 Android 漫画客户端，界面使用 Kotlin、Jetpack Compose 与 Material 3 实现。项目在 [Dedicatus546/jm-mobile](https://github.com/Dedicatus546/jm-mobile) 的基础上继续维护，默认使用 [JUKOMU/JMComic-Api-Java](https://github.com/JUKOMU/JMComic-Api-Java) 作为内置数据源，同时保留可切换的网络 API 入口。

- 手机端 `:app`（Android 6.0+）与手表端 `:jm-mobile-clock`（Wear OS，Android 7.1+）是两个独立的可安装应用。
- 网络、缓存、下载、AI 对话一律按 `Service / DAO / Storage → Repository → ViewModel → StateFlow → Composable` 分层，不在 UI 层直接请求数据。

## 截图

<p align="center">
  <img src="readme-assets/首页.jpg" width="24%" alt="首页">
  <img src="readme-assets/详情（日间模式）.jpg" width="24%" alt="详情（日间模式）">
  <img src="readme-assets/详情（夜间模式）.jpg" width="24%" alt="详情（夜间模式）">
  <img src="readme-assets/搜索1.jpg" width="24%" alt="搜索">
</p>

<p align="center">
  <img src="readme-assets/搜索2.jpg" width="24%" alt="搜索结果">
  <img src="readme-assets/每周必看.jpg" width="24%" alt="每周必看">
  <img src="readme-assets/签到.jpg" width="24%" alt="签到">
  <img src="readme-assets/个人中心.jpg" width="24%" alt="个人中心">
</p>

## 功能

### 浏览与发现

- 首页轮播推荐、分类入口与自定义分类排序；网格列数可固定或自适应。
- 推荐来源可选内置推荐、网络推荐或基于账号偏好的个性化推荐。
- 搜索支持关键词、`-tag` 排除语法、主分类筛选、排序方式与搜索历史。
- 从任意标签或分类进入日榜 / 周榜 / 月榜。
- 详情页展示封面、作者、标签、章节、评论与相关推荐，支持收藏、点赞与评论回复。
- 每周必看、收藏列表、阅读历史、评论历史。

### 阅读

- 滚动、分页、点击三种阅读模式，点击区支持 `default` 与 `side` 两种布局。
- 自动翻页（200 毫秒到 60 秒可调，按钮外圈显示剩余进度）、音量键翻页、亮度调节。
- 记住上次阅读的章节与页码，章节选择器标记已读章节。
- 按前后范围预解码图片，支持并发解码上限与内存优化开关。
- 长按图片保存到相册，默认关闭，开启后仍需在弹窗中二次确认。
- 图片以原始尺寸解码后再做切片还原，规避缩放导致的错位与白线。

### 下载与缓存

- WorkManager 后台下载，Room 记录进度、状态与分组信息，通知栏显示进度并支持重试。
- 下载目录可留在应用私有目录，也可通过系统目录授权（SAF）放到外部存储，支持后台迁移与持续进度通知。
- 离线阅读支持图片目录与 zip 包两种来源。
- 缓存完整性检查（关闭 / 配置与封面 / 全量），缓存清理页可按类别清理封面、漫画、解码与导出临时文件。
- 已缓存漫画可导出为 PDF。

### 隐私与安全

- 应用锁：密码、图案、生物识别，解锁规则可选“任一通过”或“全部通过”。
- 胁迫凭据：输入预设的密码或图案后进入电子木鱼伪装页，不会解锁主应用。
- 桌面图标伪装：可在 JMcomic、系统工具、相册、木鱼之间切换。
- 启用 `FLAG_SECURE` 阻止截图与任务预览，切到后台时再盖一层纯色遮挡。
- 本地敏感数据经 `SecureStorage` 统一加密，优先使用 Android Keystore 的 AES/GCM 密钥。
- 备份与恢复：可导出带访问保护的备份，覆盖设置、下载路径、AI 会话、人设与缓存索引。

### AI 助手

- 流式对话与 Markdown 渲染，思考块可折叠。
- 可在两个上游模型间切换，支持多个联网搜索引擎与自定义人设（Persona）管理。
- 会话保存在本地，可纳入备份。

### 其他

- 主题跟随系统 / 浅色 / 深色，内置配色预设与自定义四色。
- DNS over HTTPS：内置与自定义服务器、延迟测速、启动时自动启用。
- 应用内检查更新，直接下载并调起系统安装器。
- 剪贴板识别与「提取编码」页：从分享文案里提取数字并直接拉取漫画详情。
- 运行日志查看器，方便反馈问题。
- Wear OS 手表端：圆形 / 方形表盘适配、阅读控制、重力翻页、离线缓存、应用锁与悬浮球。

## 技术栈

| 类型 | 使用内容 |
| --- | --- |
| 语言 | Kotlin 2.4.21，JVM 17 |
| UI | Jetpack Compose、Material 3、Material Icons Extended |
| 导航 | AndroidX Navigation Compose |
| 架构与状态 | ViewModel、StateFlow、Compose State |
| 依赖注入 | Koin 4.2 |
| 网络 | Retrofit 3、OkHttp 4、Gson、jsoup |
| 内置数据源 | JMComic-Api-Java v1.1.12 |
| 图片加载 | Coil 2.7 |
| 本地数据库 | Room 2.8、Room Paging |
| 分页 | Paging 3 |
| 后台任务 | WorkManager |
| 日历组件 | Kizitonwose Calendar Compose |
| 构建 | Gradle 9.5.1、AGP 9.1.0、KSP、Gradle Kotlin DSL |

## 项目结构

```text
.
├── app/                         手机端 Android 应用模块（:app）
│   ├── build.gradle.kts         应用构建配置、版本读取、发布签名
│   ├── proguard-rules.pro       Release 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml  权限、入口 Activity、桌面图标别名
│       ├── java/com/par9uet/jm/ Kotlin 源码
│       └── res/                 图标、主题、字符串等资源
├── jm-mobile-clock/             手表端 Wear OS 模块（:jm-mobile-clock）
├── gradle/libs.versions.toml    依赖与插件版本目录
├── readme-assets/               README 截图
├── docs/                        调试与验证记录
├── tools/                       一次性验证脚本
├── CHANGELOG                    变更记录
├── LICENSE                      GPL-3.0
├── version.properties           唯一的版本号来源
└── settings.gradle.kts          仓库源与模块声明
```

| 模块 | applicationId | minSdk | 说明 |
| --- | --- | --- | --- |
| `:app` | `jmcomicoi.net` | 23 | 手机端 |
| `:jm-mobile-clock` | `jmcomicoi.net.clock` | 25 | Wear OS 手表端 |

两个模块的 `namespace` 都是 `com.par9uet.jm`。`applicationId` 与 `namespace` 一旦改动，已安装用户将无法覆盖升级，源码与资源引用也会大面积失效，不要修改。

核心源码位于 `app/src/main/java/com/par9uet/jm`：

```text
com.par9uet.jm
├── cache/          缓存目录、封面缓存、下载缓存格式与迁移
├── coil/           Coil ImageLoader 配置
├── data/models/    业务数据模型（Gson / Room 会被反射读取）
├── database/       Room 数据库、DAO、下载任务实体
├── di/             Koin 模块：App / Coil / Comic / Database / Retrofit / User
├── network/        DoH、封面地址解析、时间排行抓取
├── repository/     接口与 impl/，网络与本地数据的唯一入口
├── retrofit/       Service、拦截器、响应转换、响应模型
├── storage/        SharedPreferences 封装与加解密
├── store/          全局状态（用户、设置、下载、更新、备份、初始化）
├── task/           AppInitTask 启动任务接口
├── ui/             Compose 页面、组件、主题、ViewModel、分页源
├── utils/          通用工具、PDF 导出、Markdown、日志、图片处理
├── worker/         WorkManager 后台任务
├── App.kt          Compose 应用根节点（开屏、锁屏、警告与更新弹窗）
├── JmApplication.kt Application，启动 Koin 与 WorkManager 注入
└── MainActivity.kt Activity 入口，启用 edge-to-edge 并挂载 Compose
```

## 环境要求

- JDK 17 或更高版本（CI 使用 Temurin 21）。
- Android SDK：`platforms;android-36` 与 `build-tools;36.0.0`。
- 能识别 AGP 9.1 的 Android Studio，或直接使用命令行构建。
- Gradle Wrapper 已包含在仓库中，请始终使用 `gradlew` / `gradlew.bat`。

> [!TIP]
> 仓库默认走阿里云 Maven 镜像与腾讯的 Gradle 分发镜像；在海外网络下可换回 `google()`、`mavenCentral()` 与官方 `services.gradle.org`。CI 通过 init script 切回官方仓库，改动仓库源时两边都要考虑。

## 快速开始

```bash
git clone https://github.com/G1oow/jmcomic-next.git
cd jmcomic-next
```

用 Android Studio 打开仓库根目录，等待 Gradle 同步完成后运行 `app` 模块。首次同步会自动生成 `local.properties`，该文件记录本机 SDK 路径，不要提交。

常用命令：

| 目的 | 命令 |
| --- | --- |
| 编译 Kotlin | `gradlew.bat :app:compileDebugKotlin` |
| 单元测试 | `gradlew.bat :app:testDebugUnitTest` |
| Lint | `gradlew.bat :app:lintDebug` |
| Debug APK | `gradlew.bat :app:assembleDebug` |
| Release APK | `gradlew.bat :app:assembleRelease` |
| 手表端编译 | `gradlew.bat :jm-mobile-clock:compileDebugKotlin` |

Linux / macOS 把 `gradlew.bat` 换成 `./gradlew` 即可。产物文件名带版本号和 Git 短 hash：

```text
jm-mobile_v{versionName}_{gitHash}.apk
jm-mobile-clock_v{versionName}_{gitHash}.apk
```

### 发布签名

`release` 变体使用项目自有密钥，配置来源优先级为 `secrets.properties`（本地，已忽略）→ 同名环境变量：

| 键 | 说明 |
| --- | --- |
| `JM_RELEASE_STORE_FILE` | keystore 路径，放在仓库之外 |
| `JM_RELEASE_STORE_PASSWORD` | keystore 口令 |
| `JM_RELEASE_KEY_ALIAS` | 密钥别名 |
| `JM_RELEASE_KEY_PASSWORD` | 密钥口令，缺省时回退到 store 口令 |
| `JM_REQUIRE_RELEASE_SIGNING` | 设为 `true` 时缺少配置直接构建失败 |

可参考仓库内的 `secrets.properties.example` 生成自己的 `secrets.properties`。

> [!IMPORTANT]
> 未配置签名时 release 会回退到 debug 签名并打印警告。debug keystore 由构建机自动生成，**每台机器、每次 CI 都不同**，这类产物只能自用，无法覆盖安装或对外分发。

手表端 `:jm-mobile-clock` 的 release 变体固定使用 debug 签名，目前只适合本地与 CI 自测。

## 架构概览

### 数据流

```text
Retrofit Service / Room DAO / SecureStorage
                ↓
           Repository
                ↓
           ViewModel
                ↓
      StateFlow / Compose State
                ↓
        Composable Screen
```

UI 状态优先复用 `ui/models` 中的 `CommonUIState`、`ListUIState`、`PageAppendUIState`，分页列表走 Paging 3（`ui/pagingSource`）。

### 依赖注入

`JmApplication` 在启动时通过 Koin 注册以下模块：

| 模块 | 内容 |
| --- | --- |
| `appModule` | 协程作用域、各 Storage、Manager、AI 仓库、全局 ViewModel |
| `coilModule` | 全局 `ImageLoader` 与封面缓存 |
| `comicModule` | 漫画仓库与漫画相关 ViewModel |
| `retrofitModule` | Retrofit、OkHttp、拦截器与 Service |
| `userModule` | 用户仓库与用户 ViewModel |
| `databaseModule` | Room 数据库、下载 DAO、下载 Manager、下载与缓存迁移 Worker |

### 启动流程

`App` 先读取本地设置决定是否锁屏，再调用 `GlobalViewModel.init()`。初始化任务来自所有实现 `AppInitTask` 的对象，按 `AppTaskInfo.sort` 串行执行：本地设置 → DoH 与 Retrofit 配置 → 远端设置 → 用户与搜索历史 → 阅读历史。全部完成后 `InitManager` 的 `deferred` 被完成，网络请求可通过 `InitInterceptor` 等待初始化状态。

### 页面导航

主导航定义在 `ui/screens/AppScreen.kt`，默认入口为 `tab/home`；底部 Tab 为首页、AI（可选入口）、收藏、个人中心，宽屏自动切换为侧边导航栏。

| 路由 | 页面 |
| --- | --- |
| `tab/{tabName}` | 底部 Tab 容器 |
| `comicDetail/{id}` | 漫画详情 |
| `comicChapter/{id}?currentChapterId={cid}` | 章节列表 |
| `comicRead/{id}` / `localComicRead/{id}` | 在线阅读 / 本地阅读 |
| `comicSearch?searchContent=…` / `comicSearchResult/{searchContent}?…` | 搜索入口与搜索结果 |
| `comicRanking/{targetType}/{targetValue}?period={period}` | 日 / 周 / 月排行 |
| `comicRecommend` | 每周必看 |
| `comicRelate/{id}` / `comment/{comicId}` | 相关推荐 / 评论 |
| `download` / `downloadComicDetail/{id}` | 下载管理 / 已缓存漫画详情 |
| `userCollectComic` / `userHistoryComic` / `userHistoryComment` | 收藏、阅读历史、评论历史 |
| `appLocalSetting` | 本地设置 |
| `appLockSetting` / `colorPalette` / `blockedTags` | 应用锁、配色、屏蔽标签 |
| `dohSetting` / `cacheCleanup` / `backupRestore` | DoH、缓存清理、备份恢复 |
| `personaManager` / `logViewer` / `about` | 人设管理、日志、关于 |
| `sign` / `login` / `extractCode` | 签到、登录、提取编码 |

## 关键实现

### 数据源与网络

- `builtin`：通过 JMComic-Api-Java 的内置客户端直接解析站点，共享同一个 `JmApiClient` 实例，登录态与 Cookie 在仓库之间复用。
- `network`：Retrofit 请求本地设置里的 API 域名，域名由 `BaseUrlInterceptor` 在运行时替换。
- `mixed`：先走内置客户端，解析失败时回退到同章节的网络接口。

`EmbeddedClientManager` 会缓存探测到的最快域名（`FastDomainStorage`），Android 6 上直接使用一组验证过的兼容域名，避免启动等待域名探测，内置客户端在某条线路异常时也会自动换线。网络读请求统一包裹在 `retryNetworkRequest` 中，失败按退避策略重试；`DoH` 作为进程级 `Dns` 实现，可让所有应用自有的 OkHttp 客户端走加密 DNS。

漫画图片列表不是标准 JSON：内置客户端与网络接口都会拿到章节页面，再由 `retrofit/DataDecode.kt` 中的解析逻辑提取图片地址、扰乱阈值与速度参数。

### 图片解码

`ComicPicImageState` 负责加载、扰乱还原与本地解码缓存：

- 以原始尺寸解码，避免缩放出白线。
- GIF、漫画 ID 不高于扰乱阈值，或服务端速度参数为 `1` 时不做切片还原。
- 需要还原时以 `comicId + page` 的 MD5 作为 seed，纵向分片后重绘为正确顺序。
- 解码结果以 WebP 写入 `cacheDir/pic_decode/{comicId}`，下次直接读缓存。

### 下载与缓存目录

| 目录 | 内容 |
| --- | --- |
| `filesDir/download/{comicId}` | 漫画正文、`config.json`、封面与章节 |
| `cacheDir/pic_decode/{comicId}` | 阅读页解码缓存 |
| `cacheDir/comic_covers` | Coil 封面缓存，可被系统回收 |
| `cacheDir/common` | 其他临时文件 |

漫画属于用户数据，因此放在 `filesDir` 而不是 `cacheDir`；1.2.5 之前的 `cacheDir/download` 会在启动时迁移到新位置。下载记录保存在 Room 的 `download_comics` 表（数据库版本 4），字段包括 `id`、`name`、`authorList`、`tagList`、`coverPath`、`zipPath`、`progress`、`status`、`createTime`、`groupId`、`groupName`、`chapterName`。

### 安全存储与备份

`SecureStorage` 统一序列化并加密写入 SharedPreferences，`CryptoManager` 优先使用 Android Keystore 的 AES/GCM 密钥，不可用时退化为带 `plain:` 前缀的 Base64 编码以兼容异常设备。

备份文件为版本化 JSON（当前格式版本 4），可选用密码或图案保护，并可选绑定本机生物识别；密码与图案始终是跨设备恢复的保底凭据。

## 版本与发布

`version.properties` 是唯一版本来源，`app` 与 `jm-mobile-clock` 都读它：

```properties
VERSION_CODE=136
VERSION_NAME=1.2.16
```

发版规则：

- `VERSION_CODE` 必须为正整数，每次发版 +1，只增不减、不复用历史值。
- `VERSION_NAME` 使用 `MAJOR.MINOR.PATCH`，逐段单调递增，不带 `v` 前缀。
- 标签统一为 `v{VERSION_NAME}`，由 CI 依据 `version.properties` 创建，不要手打。
- 面向用户的变更同步写入 `CHANGELOG` 对应版本段落，CI 会把该段落填入 Release 说明。

`.github/workflows/android-release.yml` 在推送 `v*` 标签或手动触发时构建 Release APK（`JM_REQUIRE_RELEASE_SIGNING=true`），并上传 `jm-mobile-release` artifact。构建成功后自动发布 GitHub Release：标签取 `v{VERSION_NAME}`，资产为 `jm-mobile_v*_*.apk`，同版本重复构建会覆盖旧资产。发布步骤只在 `main` 分支或 `v*` 标签上执行，分支验证构建只产出 artifact。

需要的仓库 Secrets：`JM_RELEASE_KEYSTORE_BASE64`、`JM_RELEASE_STORE_PASSWORD`、`JM_RELEASE_KEY_ALIAS`、`JM_RELEASE_KEY_PASSWORD`。

### 应用内更新

「设置 → 检查更新」可手动检查，启动后也会静默检查（24 小时内不重复请求）。更新源是本 fork 的 GitHub Releases，常量在 `utils/AppUpdateVersion.kt`：

- 读取 `https://api.github.com/repos/G1oow/jmcomic-next/releases/latest`，按 `VERSION_NAME` 逐段比较版本。
- 支持应用内下载（暂停、后台下载、通知进度），完成后调起系统安装器。
- Release 必须是非草稿、非预发布并标记为 latest，否则永远检测不到更新。

> [!IMPORTANT]
> 换仓库时要同步修改 `utils/AppUpdateVersion.kt` 中的常量；已发布的 Release 与标签不要删除或改名，用户端的检查与下载链接都指向它们。

## 开发约定

- 代码结构、命名、Compose 与状态规范见 `AGENTS.md`，改动前先读一遍。
- 新增页面：`ui/screens` 建 Compose 页面 → 需要状态时在 `ui/viewModel` 加 ViewModel → Koin 注册 → `AppScreen.kt` 加路由 → 需要 Tab 入口时改 `tabScreen`。
- 新增接口：`retrofit/service` 加方法 → `retrofit/model` 加响应模型 → `repository` 加抽象方法 → `repository/impl` 用 `safeApiCall` / `safeStringCall` 包装 → ViewModel 消费。
- 新增持久化：小体量配置走 `storage/` + `SecureStorage`；需要查询与分页的数据新增 Room Entity / DAO 并注册到 `databaseModule`。
- 新增后台任务：在 `worker/` 实现 Worker，通过 Koin 注册，禁止在 UI 层直接起长任务。
- 提交前至少跑一次 `:app:compileDebugKotlin`；纯逻辑改动补单元测试并跑 `:app:testDebugUnitTest`；改构建脚本、R8 规则或 workflow 时额外跑 `:app:assembleRelease`。

## 常见问题

### Gradle 同步失败

先确认 Android Studio、JDK、Android SDK 版本：项目使用 AGP 9.1、Kotlin 2.4 与 Compose BOM 2026.01，旧版 IDE 可能无法识别插件或 `compileSdk 36`。网络受限时检查 `settings.gradle.kts` 的镜像配置。

### 接口请求失败

先在设置页切换 API 域名与数据源（内置 / 网络 / 混合），再确认设备能访问对应服务。baseUrl 是运行时动态替换的，实际地址取决于本地设置与拦截器。

### 图片显示白线或错位

阅读页依赖原始尺寸解码与本地解码缓存。可尝试清理应用缓存后重新打开；若只在特定分流下出现，切换到其他图片分流再试。

### 下载任务一直失败

下载依赖网络、图片域名、解码与本地写入。先确认在线阅读正常，再检查下载目录是否有该漫画的目录与 `config.json`，必要时用「缓存完整性检查」定位缺失文件。

### PDF 导出失败

导出需要系统目录授权，请选择可写目录，并确认对应漫画已完成缓存、本地图片可读。

### 忘记应用锁

应用锁凭据只保存在本地且不可绕过，重装会清空全部本地数据。建议开启前先做好备份。

## 致谢

本项目离不开 [LINUX DO 论坛](https://linux.do) 与 [RawChat 团队](https://linux.do/u/RawChat) 的支持，也感谢上游项目 [Dedicatus546/jm-mobile](https://github.com/Dedicatus546/jm-mobile) 与 [JUKOMU/JMComic-Api-Java](https://github.com/JUKOMU/JMComic-Api-Java)。

<details>
<summary>提交建议与反馈问题的朋友们</summary>

LINUX DO 提供建议的佬友：dijunwanshou、zeroLin、dunxuan、lyston、catnap、JasonGui、sswh317、Jealousy、lxy521031、Timothy、sylarQAQ、cftyhn、chowhound。

GitHub 提交 issue 的朋友：yinshu2002、lyston11、Nines70、lin39c5bb、ZhangXusen、Luo923、snakeJohn、franklleee、788gdd、ban-code-art、EntID-Ke、yevonou、j1v-software、Hanestl、LanHuXiangYuan、yxxbc、Yanzietta、funnytime75、xiban123、plane714、swhje。

为项目添砖加瓦的朋友：Jea1ousy、linze0721、hifumi_mizuhara、serious198706、funnytime75。

以及一直提出需求的 QQ 群友们。

</details>

## 免责声明

本项目仅供学习、研究与技术交流使用。项目作者与任何第三方服务、原始应用或内容提供方无关。

使用者应自行遵守当地法律法规以及相关服务条款。因使用本项目产生的任何法律、版权、账号、数据或财务风险均由使用者自行承担。许可证见仓库中的 `LICENSE`（GPL-3.0），原项目版权与许可信息请参考 `Dedicatus546/jm-mobile`。
