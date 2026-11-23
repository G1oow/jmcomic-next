# AGENTS.md

本文件面向在本仓库工作的编码 agent 与贡献者，约定目录结构、构建方式、代码风格、提交与发布流程。**动手前先读完本文件**；与 README 冲突时以本文件为准（README 面向使用者，本文件面向维护者）。

## 1. 项目速览

Android 漫画客户端，Kotlin + Jetpack Compose + Material 3，基于 `Dedicatus546/jm-mobile` 二次开发。

| 项 | 值 |
| --- | --- |
| Gradle 模块 | `:app`（手机端）、`:jm-mobile-clock`（Wear OS 手表端） |
| rootProject | `jm-mobile-android6` |
| namespace | `com.par9uet.jm` |
| applicationId | `jmcomicoi.net` |
| SDK | `compileSdk 36` / `targetSdk 35` / `minSdk 23` |
| 语言与 UI | Kotlin 2.3.20、Compose BOM 2026.01.00、Material 3 |
| 构建 | AGP 9.1.0、Gradle Wrapper 9.5.1、KSP |
| 依赖注入 | Koin |
| 网络 / 存储 / 分页 | Retrofit + OkHttp + Gson / Room + SharedPreferences / Paging 3 |

核心包（`app/src/main/java/com/par9uet/jm`）：

```text
cache/     缓存目录、图片解码缓存、封面预加载
data/      业务数据模型（Gson 会被反射读取）
database/  Room 数据库、DAO、下载任务实体
di/        Koin 模块：AppModule / CoilModule / ComicModule / DatabaseModule / RetrofitModule / UserModule
repository/ 接口 + impl/ 实现，网络与本地数据的唯一入口
retrofit/  Service 接口、拦截器、响应转换、响应模型
storage/   SharedPreferences 封装（经 SecureStorage）
store/     全局状态 Manager（User / LocalSetting / RemoteSetting / Toast / Download / Init …）
task/      AppInitTask 启动任务接口
ui/        screens/ 页面、viewModel/、components/ 组件、theme/、models/ UI 状态、pagingSource/
utils/     通用工具（PDF 导出、图片处理、日志等）
worker/    WorkManager 后台任务
```

数据流固定为 `Service / DAO / Storage → Repository → ViewModel → StateFlow → Composable`，不要跨层直连。

## 2. 环境要求

- JDK 17 及以上（CI 使用 Temurin 21）。
- Android SDK：`platforms;android-36`、`build-tools;36.0.0`、`platform-tools`。
- 仓库自带 Gradle Wrapper，**始终使用 `./gradlew`**（Windows 用 `gradlew.bat`），不要依赖本机 Gradle 安装。
- 依赖源使用阿里云镜像（见 `settings.gradle.kts`），CI 通过 init script 切回官方仓库。改动仓库源时两边都要考虑。

当前开发机可直接用的环境变量（仅本机约定，其他机器按实际情况设置）：

```powershell
$env:JAVA_HOME='F:\Android\jdk'
$env:ANDROID_HOME='F:\Android\sdk'
$env:GRADLE_USER_HOME='F:\Android\gradle-home'
```

## 3. 构建与验证

| 目的 | 命令 |
| --- | --- |
| 编译 Kotlin（改完代码的最低要求） | `gradlew.bat :app:compileDebugKotlin --console=plain` |
| 单元测试 | `gradlew.bat :app:testDebugUnitTest --console=plain` |
| Lint | `gradlew.bat :app:lintDebug --console=plain` |
| Debug APK | `gradlew.bat :app:assembleDebug --console=plain` |
| Release APK（R8 + 资源压缩） | `gradlew.bat :app:assembleRelease --console=plain` |
| 手表端 | 把 `:app` 换成 `:jm-mobile-clock` |

规则：

1. 任何代码改动，交付前至少跑一次 `:app:compileDebugKotlin`，并说明是否通过。
2. 改动了纯逻辑（解析、排序、分页、缓存、工具函数）时补单元测试并跑 `:app:testDebugUnitTest`。
3. 改动 `build.gradle.kts`、`proguard-rules.pro`、`libs.versions.toml`、workflow 时，额外跑一次 `:app:assembleRelease`（R8 相关错误只在 release 变体暴露）。
4. 构建失败就把错误原文贴出来，不要用「应该能过」代替验证。

## 4. 代码结构约定

### 新增页面

1. 在 `ui/screens/` 下新建 `XxxScreen.kt`（大页面建子目录，如 `readScreen/`）。
2. 需要业务状态时在 `ui/viewModel/` 添加 `XxxViewModel`，用 `StateFlow` 暴露状态。
3. 在 `di/` 对应模块用 `viewModel { XxxViewModel(get()) }` 注册。
4. 在 `ui/screens/AppScreen.kt` 的 `NavHost` 添加路由；作为底部 Tab 入口的再改 `tabScreen/`。

### 新增接口

`retrofit/service/` 加方法 → `retrofit/model/` 加响应模型 → `repository/` 加抽象方法 → `repository/impl/` 用 `safeApiCall` / `safeStringCall` 包装 → ViewModel 消费。不要在 ViewModel 或 Composable 里直接持有 Retrofit Service。

### 新增持久化

- 简单配置、小体量状态：`storage/` + `SecureStorage`，通过 Manager 暴露。
- 需要查询、分页、状态更新：Room，新增 Entity / DAO 并注册到 `DatabaseModule`。

### 新增后台任务

用 WorkManager 建 `Worker`，在 `worker/` 下实现，通过 `databaseModule` 或 `AppModule` 交给 Koin 创建，禁止在 UI 层直接起长任务。

### 依赖注入

- 单例：`single { Xxx(get()) }`，需要绑定接口时用 `bind XxxRepository::class`。
- ViewModel：`viewModel { XxxViewModel(get(), get()) }`。
- Composable 里取依赖用默认参数 `getKoin().get()`，便于测试时替换。
- 实现 `AppInitTask` 的类会在启动时被 `InitManager` 按 `AppTaskInfo.sort` 执行，启动期逻辑放这里。

## 5. 代码规范

### Kotlin

- 遵循 Kotlin 官方风格（`gradle.properties` 已设 `kotlin.code.style=official`）：4 空格缩进、`val` 优先、表达式体优先、不写多余 `Unit`。
- 命名：类 / Composable 用大驼峰；函数与属性用小驼峰；常量用 `UPPER_SNAKE_CASE`；Composable 必须是名词短语（`ComicCard`），返回 `Modifier` 的扩展函数用 `Modifier.xxx()`。
- 文件内顺序：`package` → `import`（字母序，无通配符）→ 声明；单文件尽量只放一个主类。
- 可空性用 `?.` / `?:` / `requireNotNull` 表达，不要 `!!`；确实不可能为空时附注释说明原因。
- 协程统一用 `viewModelScope` / 注入的 `CoroutineScope`，不要在 Composable 里 `GlobalScope.launch`。
- 注释语言跟随所在文件现有语言（本仓库中英混排），解释「为什么」而非「做了什么」。

### Compose

- 颜色、字体、圆角、阴影一律取 `MaterialTheme.colorScheme` / `typography` / `shapes`，禁止硬编码色值（主题色在 `ui/theme/`，自定义调色板见 `ColorPalette.kt`）。
- 动效复用 `LocalAppMotion`（`AppMotionPolicy`）：系统关闭动画时自动降级为直接切换。新动效不要写死时长，从 `LocalAppMotion` 取值。
- 图标优先复用 `material-icons-extended`，项目自有图标放 `res/drawable`；不要用 emoji 代替功能图标。
- 列表用 `LazyColumn` / `LazyVerticalGrid` + `key`；状态用 `remember` / `rememberSaveable` 区分是否需要跨配置变更保留。
- 页面骨架沿用 `ui/components/CommonScaffold.kt` 与既有 Screen 的写法，不要引入第二套 Scaffold 约定。
- 提示统一走 `ToastManager`（全局 Snackbar），不要各页面自己弹 Toast。
- 交互必须覆盖：加载、空数据、错误、重试；可点击元素要有无障碍 `contentDescription`。

### 状态与数据

- UI 状态优先复用 `ui/models/` 中的 `CommonUIState` / `ListUIState` / `PageAppendUIState`，不要每页新造一套。
- ViewModel 只暴露不可变状态（`StateFlow<XxxUIState>`），可变集合在内部复制后再发射。
- 全局状态（用户、设置、下载、启动）只在 `store/` 维护，页面不直接读写 SharedPreferences。

### 依赖与资源

- 新增依赖必须在 `gradle/libs.versions.toml` 声明版本与 alias，禁止在 `build.gradle.kts` 里写死版本号。
- 新增依赖前确认没有同类库已在用（Compose 内已有 Material 3、Coil、Paging、Koin、Retrofit）。
- 字符串、颜色、尺寸能抽资源就抽 `res/values/`；不要把长文本散落在 Composable 里。

## 6. 测试规范

- 单元测试放 `app/src/test/java/...`，与被测类同包，类名 `XxxTest`，方法名用英文描述行为（如 `keepsUnknownCategoriesAfterSavedOrder`）。
- 断言用 `org.junit.Assert`，纯逻辑优先抽成可测函数（`utils/`、`repository/` 中已有先例），不要为了测试引入 mock 框架。
- Compose 页面依赖 Koin 与网络，暂不做 UI 单测；需要验证界面时用真机 / 模拟器手动跑。
- `androidTest` 目前只有模板用例，不要往里堆临时验证代码。

## 7. 提交规范

### 分支

- 从 `main` 新建，命名 `<type>/<kebab-case>`：`feat/auto-page-turn`、`fix/tag-filtering`、`chore/release-signing`。
- 一个分支只做一件事；跨主题的改动拆分支、拆 PR。

### 提交信息

采用 Conventional Commits：

```text
<type>(<scope>): <subject>

<body>

<footer>
```

- `type`：`feat`、`fix`、`refactor`、`perf`、`docs`、`test`、`build`、`ci`、`chore`。
- `scope` 可选，取模块或领域：`ui`、`read`、`download`、`ai`、`settings`、`build`、`ci`。
- `subject` 用祈使句、不加句号，中文或英文均可但同一 PR 保持一致；建议 ≤ 72 字符。
- 正文说明「为什么改、影响面、需要注意的行为变化」，一行不超过 100 字符。
- 破坏性变更在 type 后加 `!` 并在 footer 写 `BREAKING CHANGE:`。
- 不要把格式化、重命名、功能改动混在同一提交里。

示例：

```text
feat(ui): auto-hide top and bottom bars while scrolling

Scrolling forward collapses the app bar and bottom navigation, scrolling
back reveals them. Nested scroll is observed on user drag only so flings
do not toggle the bars.

chore(build): sign release builds with a project-owned keystore
```

### 禁止提交

`secrets.properties`、`*.jks`、`*.keystore`、`*.apk`、`*.aab`、`local.properties`、`build/`、`.gradle/`、`.idea/`（`.gitignore` 已覆盖，不要用 `-f` 绕过）。提交前跑 `git status` 确认没有意外文件。

## 8. PR 规范

- 基于自己的 fork：`fork` remote → `G1oow/jmcomic-next`，`origin` 指向上游。PR 目标为 fork 的 `main`。
- 标题与首个提交一致，正文至少包含：**问题 / 变更点 / 验证方式 / 迁移或兼容影响**。
- 涉及签名、存储格式、数据库结构、导航路由的改动，必须在正文单独说明兼容性影响。
- 合并前确认 CI `Release APK` 通过；不要合并带着失败构建的 PR。

## 9. 版本、签名与发布

### 版本

`version.properties` 是唯一版本来源，`app` 与 `jm-mobile-clock` 都读它：

```properties
VERSION_CODE=126
VERSION_NAME=1.2.6
```

发版时两个值必须一起改，且满足：

- `VERSION_CODE` 是**正整数**，每次发版 `+1`，只增不减、不复用历史值。写入非整数会让 `versionCode` 为空（`toIntOrNull()` 没有默认值），安装和升级都会出问题。
- `VERSION_NAME` 用 `MAJOR.MINOR.PATCH` 三段数字，逐段单调递增；不带 `v` 前缀，不带构建号、日期或 `-beta` 之外的标记。应用内更新检查按它逐段比较，命名乱序会让用户收不到更新或反复被提示。
- 标签统一为 `v{VERSION_NAME}`，由 CI 依 `version.properties` 自动创建，不要手打标签；标签与 `version.properties` 不一致会导致 Release 和安装包版本对不上。
- APK 输出名自动带上版本与短 hash：`jm-mobile_v{versionName}_{gitHash}.apk`；应用内更新按 `jm-mobile_v{versionName}` 前缀挑选下载资产，改命名规则要同步改 `selectApkAsset`。
- 只有可见变化才升号：纯 CI、文档改动不升 `VERSION_CODE`。
- 面向用户的变更同步更新 `CHANGELOG.md`（若存在）：平时累积在 `# 未发布` 下，发版时改成 `# vX.Y.Z`，CI 会把该段落写进 Release 说明。
- Release 必须是非草稿、非预发布并标记为 latest，否则 `releases/latest` 取不到，应用内检查更新会一直认为已是最新。
- 已发布的 Release 和标签不要删除或改名：用户端的更新检查与下载链接都指向它们。

### 签名

`release` 变体使用项目自有密钥，配置来源优先级：`secrets.properties`（本地，已忽略）→ 同名环境变量。

| 键 | 说明 |
| --- | --- |
| `JM_RELEASE_STORE_FILE` | keystore 路径，放在仓库之外 |
| `JM_RELEASE_STORE_PASSWORD` | keystore 口令 |
| `JM_RELEASE_KEY_ALIAS` | 密钥别名 |
| `JM_RELEASE_KEY_PASSWORD` | 密钥口令，缺省回退到 store 口令 |
| `JM_REQUIRE_RELEASE_SIGNING` | `true` 时缺配置直接构建失败（CI 默认开启） |

- 未配置签名时 release 会回退 debug 签名并打印警告。debug keystore 由构建机自动生成，**每台机器、每次 CI 都不一样**，这类产物不得对外分发。
- 密钥丢失后无法再签出可覆盖升级的同包名 APK，务必离线备份 keystore 与口令，不要把密钥写进仓库或提交信息。

### CI

`.github/workflows/android-release.yml`：推送 `v*` 标签或手动 `workflow_dispatch` 时构建 Release APK，构建成功后自动发布到 GitHub Releases（标签 `v{VERSION_NAME}`，资产为 `jm-mobile_v*_*.apk`），同时上传 `jm-mobile-release` artifact（保留 14 天）。需要仓库 Secrets：`JM_RELEASE_KEYSTORE_BASE64`（keystore 的 base64）、`JM_RELEASE_STORE_PASSWORD`、`JM_RELEASE_KEY_ALIAS`、`JM_RELEASE_KEY_PASSWORD`。

- 发布步骤 `Publish GitHub release` 只在 `main` 分支或 `v*` 标签上执行；在分支上验证构建只产出 artifact，不会创建标签和 Release。
- 同名 Release 已存在时更新说明并覆盖 APK（`--clobber`），重复构建同一版本不会失败。

- 修 CI 时优先在分支上 `gh workflow run ... --ref <branch>` 验证，再合入 `main`。
- 不要为了「让流水线变绿」而放宽 `JM_REQUIRE_RELEASE_SIGNING` 或使用 debug 签名。

## 10. 不要做的事

- 修改 `applicationId` 或 `namespace`：会让已安装用户无法覆盖升级，且大量资源引用依赖 `com.par9uet.jm`。
- 在 ViewModel / Composable 中直接 `new` Repository 或 Service，绕过 Koin。
- 把网络响应模型放到 `data/models/` 之外的未 keep 包中（R8 会混淆字段，Gson 反序列化失败）。
- 引入第二套主题、第二套 Scaffold、第二套 Toast。
- 在 `main` 上直接提交；不要 `push --force` 已推送的分支，除非确认没有人基于它工作。
- 提交带 `!!`、吞异常（`catch (e: Exception) {}`）、无超时的网络调用等会让问题被掩盖的代码。

## 11. 常见坑

- **R8 keep 规则**：`proguard-rules.pro` 按整包 keep 了 `data.models`、`retrofit.model`、`ui.models`、`utils`、`database`、`store`、`repository`、`storage`、`di`。新增包若被 Gson / Koin / 反射访问，必须补规则，否则只在 release 崩。
- **图片解码**：阅读页依赖原始尺寸解码与 `cacheDir/pic_decode/{comicId}` 缓存，改动 `ComicPicImageState` 时注意扰乱还原与白线问题。
- **版本号**：`app/build.gradle.kts` 中 `VERSION_CODE` 走 `toIntOrNull()` 且无默认值，非法值不会报错而是静默变空。应用内更新检查读 `releases/latest`（仓库地址常量在 `utils/AppUpdateVersion.kt`），发版忘了升 `VERSION_NAME` 时用户永远看不到更新提示；换仓库也要同步改那里的常量。
- **Compose Preview**：页面强依赖 Koin 与网络，`@Preview` 只适合纯展示型组件。
- **依赖镜像**：本地走阿里云镜像，CI 走官方仓库；新增依赖时确认两边都能解析。

## 12. 交付前检查清单

- [ ] `gradlew.bat :app:compileDebugKotlin` 通过（本轮有 Kotlin 改动）
- [ ] 涉及的纯逻辑有单测且 `:app:testDebugUnitTest` 通过
- [ ] 改动构建脚本 / R8 / workflow 时 `:app:assembleRelease` 通过
- [ ] `git status` 无密钥、APK、构建产物、临时文件
- [ ] 提交信息符合第 7 节，分支命名符合规范
- [ ] 发版时 `version.properties` 的 `VERSION_CODE` / `VERSION_NAME` 已递增，Release 标签为 `v{VERSION_NAME}`
- [ ] `CHANGELOG.md`（若存在）与 `README` 在需要时已更新（面向用户的变更写 CHANGELOG）
- [ ] 无法验证的部分（真机观感、特定设备行为）在交付说明中明确写出
