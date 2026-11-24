# Debug 记录

记录本轮「启动报错 / 冷启动慢」相关的排查过程。所有结论都附实际观测数据；无法验证的部分单独标注。

环境：Android 模拟器（Android 17 / SDK 37，x86_64），debug 与 release 包均有测试。

---

## 问题 1：启动弹出「网络错误: 404」

### 现象

冷启动后，在引导页点「跳过」进入主界面时弹出一次「网络错误: 404」。

### 排查过程

1. 模拟器冷启动抓 logcat，锁定失败请求：

   ```
   D [JM-MOBILE] RemoteSettingManager: 远程应用设置开始初始化
   I okhttp.OkHttpClient: --> GET https://www.cdnhth.club/setting
   I okhttp.OkHttpClient: <-- 404 https://www.cdnhth.club/setting (891ms, unknown-length body)
   E [JM-MOBILE] RemoteSettingRepositoryImpl: 请求异常: retrofit2.HttpException: HTTP 404
   ```

2. 逐个探测 `LocalSetting.apiList` 里的 5 个镜像域名，全部失效：

   | 域名 | `/setting` 实测 |
   | --- | --- |
   | `www.cdnhth.club`（默认） | nginx 404；`/album`、`/search`、`/week`、`/promote` 同样 404 |
   | `www.jmapiproxyxxx.vip` | 200，返回域名停放页 HTML |
   | `www.cdnxxx-proxy.xyz` | 200，返回域名停放页 HTML |
   | `www.cdnmhwscc.vip` | 200，停放页，跳 `/lander` |
   | `www.jmeadpoolcdn.life` | SSL 握手失败 |

3. 解释「为什么点跳过之后才看到」：引导页期间 `SnackbarHost` 未进入 composition（`App.kt` 里 `showOnboarding` 分支提前 return），`snackbarHostState.showSnackbar()` 把消息挂起持有，进入主界面后才真正渲染。

### 根因

API 镜像域名整体失效（服务端问题）。`RemoteSettingManager` 本身已把失败降级处理（只记 `isError` 并继续），但 `ToastInterceptor` 对所有非 2xx 响应无条件弹提示，把这次可降级的失败弹了出来。

### 修复

新增 `retrofit/annotation/SilentHttpError.kt`，标注在 `RemoteSettingService.getRemoteSetting()` 上；`ToastInterceptor` 通过 `retrofit2.Invocation` 读取方法注解并跳过提示。

没有复用已有的 `@GInit`，因为它同时也标在 `UserService.login()` 上，登录失败需要提示。

### 验证

同一台设备、同一路径做改动前后对照，每 400ms 采样一次主界面 accessibility 文本：

| 版本 | 404 是否发生 | 采样结果 |
| --- | --- | --- |
| 修复前 | 是 | t+1.2s 命中 `text="网络错误: 404"` |
| 修复后 | 是 | t+0.4s ~ t+5.6s 全部无命中 |

修复前那次命中同时证明了采样方法本身可靠。release 包（R8 混淆后）也在设备上验证过无提示，说明注解未被打包裁掉。

### 结果

启动不再弹提示。`/setting` 请求本身仍然 404（服务端问题未解决）。

---

## 问题 2：冷启动慢，加载页停留时间长

### 现象

启动加载页（带「跳过」按钮的那个页面）停留 8~12 秒。

### 排查过程

1. 分解时间线（设备实测）：

   | 阶段 | 实测 |
   | --- | --- |
   | 首帧 `am start -W` TotalTime | 2.0 ~ 2.2 秒 |
   | 初始化任务链全部完成 | 1.5 秒 |
   | 首页数据到达、加载页关闭 | 8 ~ 12 秒 |

   说明慢的不是初始化任务链。

2. 加载页唯一关闭条件是 `App.kt` 里的 `homeComicState.list.isNotEmpty()`，即等首页首批数据。

3. 首页走内置 API，首次请求前必须先定型「最佳域名」。`EmbeddedClientManager` 配置为 `domainProbeTimeoutMs(1500)`，且注释写明「v1.1.12 起必须显式启动 `initializeAsync()`，否则 `getBestDomain()` 会一直阻塞」。

4. 从库 jar 中提取出 11 个候选域名，逐个实测响应时间：

   | 域名 | 耗时 | 结果 |
   | --- | --- | --- |
   | jmapiproxy1.cc | 351ms | SSL 失败 |
   | jmapiproxy2.cc | 583ms | 403 |
   | jmapinodeudzn.net | 1753ms | 403 |
   | bytepluses.com | 313ms | SSL 失败 |
   | jmcomicgo.org | 1491ms | 200 |
   | cdnaspa.club | 1417ms | 200 |
   | cdnaspa.vip | 904ms | 403 |
   | cdnplaystation6.cc | 1764ms | 200 |
   | cdnplaystation6.org | 929ms | 403 |
   | cdnplaystation6.vip | 343ms | SSL 失败 |
   | 18comic.vip | 363ms | 403 |

   串行累加约 **10.2 秒**，与设备实测的 8~12 秒吻合。

### 根因

内置客户端每次冷启动都要串行探测全部候选域名，而首页首批数据必须等探测完成才发出。

### 修复

- 新增 `storage/FastDomainStorage.kt`，用现有 `SecureStorage` 持久化「上次探测出的最快域名」。
- `EmbeddedClientManager` 改造：
  - 有缓存：把缓存域名排到 `getDomains()` 首位 + `setInitialized(true)` 直接放行首批请求，跳过启动期探测；
  - 无缓存：保持原行为走库的完整探测，完成后回写缓存；
  - 串行探测推迟 15 秒在后台执行（原为立即），完成后把最优线路重新 `updateDomains` 置顶并回写缓存。
- 探测后重新置顶是必需的：`JmDomainManager.updateDomains()` 是整表替换且清零失败计数，不置顶会让后续请求落到探测结果原本的顺序上，实测出现过 10.5 秒的离群值。

### 验证

| 场景 | 首页数据到达 |
| --- | --- |
| 无缓存（首次） | 10700ms |
| 有缓存 | 3925 / 4277 / 7495ms |

### 结果

中位数从约 10.7 秒降到约 4.3 秒。剩余时间是域名本身的实际响应（单域名 350ms~1.8s），不是探测开销。

### 遗留

- 首次安装后的第一次冷启动仍需完整探测（没有缓存可用），第二次起才提速。
- 各场景样本量只有 1~3 次，区间 3.9~7.5 秒，不保证每次都落在均值上。

---

## 问题 3：加载页闪一下，露出首页画面

### 现象

启动加载页出现时，会闪现一帧首页画面。

### 排查过程

`App.kt` 原实现：

```kotlin
var splashVisible by rememberSaveable { mutableStateOf(false) }
...
if (!settingsLoaded) { LoadingScreen(); return }
Box {
    Box { AppScreen(...) }              // 主界面直接渲染
    if (splashVisible) LoadingScreen()  // 加载页只是盖在上面
}
```

`splashVisible` 初值为 `false`，靠 `LaunchedEffect` 异步置 `true`。`settingsLoaded` 刚变 `true` 的那次组合里，`splashVisible` 仍是 `false`，主界面 `AppScreen` 先渲染出来，下一次 recomposition 才盖上加载页。`hasUnlockedOnce` 同样是异步置位，存在同样的空档。

### 根因

启动过渡的判定是异步的，存在一帧空档。

### 修复

- 早退条件从 `if (!settingsLoaded)` 扩到 `if (!settingsLoaded || !hasUnlockedOnce)`，主界面在状态就绪前不进入组合；
- 删除 `splashVisible` / `splashStarted`，改为同步求值：

  ```kotlin
  val splashActive = !splashSkipped &&
          localSetting.splashLoadingEnabled &&
          homeComicState.list.isEmpty() &&
          settingsLoaded &&
          !showOnboarding &&
          hasUnlockedOnce
  ```

- 「跳过」按钮仍是一秒后出现，点击写 `splashSkipped = true`，行为不变。

### 验证

冷启动过程中每 500ms 采样一次界面状态（`S`=加载页，`H`=首页，`?`=空）：

```
? -> S -> H -> H -> H -> H -> H -> H -> H -> ? -> H -> H
```

未出现 `H -> S` 反转。

### 验证边界

原闪烁是**一帧级**（16ms），`uiautomator` 采样最快几百毫秒，抓不到这一帧。上面的序列只能证明没有反转，真正的保证来自代码层：主界面现在必须等 `settingsLoaded && hasUnlockedOnce` 同时成立才进入组合。

---

## 问题 4：v1.2.14 之后首页首次刷新结束仍报 404（含两次误判）

### 现象

修复问题 1 后，用户在首页首次自动刷新结束时仍会看到「网络错误: 404」。

### 第一次误判：以为是启动期自动重登

用户提供的 app 内日志：

```
[17:56:01][D][UserManager] 检测到已保存了用户登录信息，开始执行一次用户登录
[17:56:01][E][RemoteSettingRepositoryImpl] 请求异常: retrofit2.HttpException: HTTP 404
```

`UserManager.init()`（启动任务链 `sort=4`）在检测到保存的登录信息时会 `autoLogin()` → Retrofit `POST /login`。上一版刻意没给 `login` 加静默（手动登录需要提示），而自动重登与手动登录共用同一接口，注解无法区分。

据此改成按「初始化是否完成」判定（`initManager.deferred.isCompleted`），v1.2.15 发布。

**结果：问题依旧。这个修复本身是对的，但不是这次报错的原因。**

### 真正的来源：首页的偏好推荐请求

排查方法：把 `ComicRepositoryImpl` 里 15 处 `service.*` 调用全部过一遍，找哪些在内置源（`useEmbeddedApi() == true`）下仍会触发。

结果只有一处 —— 第 849 行，位于 `getHomeSwiperComicListFromEmbeddedApi()` 内部：

```kotlin
// 偏好推荐开关开启时，额外请求网络 API 获取基于登录账号的个性化推荐
val preferenceEnabled = localSettingManager.localSettingState.value.preferenceRecommendEnabled
val preferenceCategories = if (preferenceEnabled) {
    val networkResponse = service.getHomeSwiperComicList()   // Retrofit GET /promote
```

其余 14 处都在 `useEmbeddedApi()` 的 `else`（网络源）分支，内置源下不会执行。

**时机也吻合**：该请求排在所有内置分类 `await()` 之后，所以内置数据先渲染出来，它随后失败并弹提示 —— 正是「首次自动刷新之后」。

### 根因

内置 API 数据源下，`preferenceRecommendEnabled`（偏好推荐）开启时，首页加载会额外发一次 Retrofit `GET /promote`。域名失效后返回 404，被 `ToastInterceptor` 弹成全局提示。

### 修复

给 `ComicService.getHomeSwiperComicList()` 标注 `@SilentHttpError`。该接口只有两条路径：

- 内置源下的偏好推荐 → 可降级数据，失败直接跳过；
- 网络源下的首页主路径 → 失败会在 `homeComicState.isError` 展示重试态，不需要全局提示。

v1.2.16 发布，用户确认不再报错。

### 教训

app 内的 `[JM-MOBILE]` 日志不含请求 URL，容易把启动阶段两个无关的 404（`/setting`、`/login`）当成根因。定位页面级 404 应该直接拿系统 logcat：

```
adb logcat -s okhttp.OkHttpClient
```

里面会有 `<-- 404 https://...` 的完整 URL，能直接定位调用点。

---

## 问题 5：模拟器性能误判（非代码问题）

### 现象

某轮测试首帧突然变成 8.6~10.5 秒（此前稳定 1.9~2.1 秒），主线程日志出现：

```
Choreographer: Skipped 387 frames! The application may be doing too much work on its main thread.
HWUI: Davey! duration=6106ms
```

一度怀疑是自己上一轮的改动引入了性能回退。

### 排查过程

1. 同刻对照系统设置冷启动：1990 / 2808ms（正常），说明不是模拟器整体变慢；
2. 检查 `/proc/loadavg` = **6.97**（该设备只分配 4 核），设备被其他项目占用；
3. 换到另一台模拟器（emulator-5560）测同一份 APK：首帧稳定 **1798 / 1801 / 1807ms**，同刻系统设置 565ms。

### 结论

不是代码回退。`Davey` 主线程阻塞与首帧延迟都来自宿主机资源竞争。

### 教训

性能问题必须先做同刻基线对照（系统应用冷启动），再判断是应用还是环境；跨设备比较数据没有意义。

---

## 附录 A：域名现状（2026-11-24）

| 用途 | 域名 | 状态 |
| --- | --- | --- |
| 应用配置（`LocalSetting.apiList`） | cdnhth.club / jmapiproxyxxx.vip / cdnxxx-proxy.xyz / cdnmhwscc.vip / jmeadpoolcdn.life | 全部失效：404 / 域名停放页 / SSL 失败 |
| 内置客户端候选（库 v1.1.12） | jmapiproxy1.cc / jmapiproxy2.cc / jmapinodeudzn.net / bytepluses.com / jmcomicgo.org / cdnaspa.club / cdnaspa.vip / cdnplaystation6.cc / cdnplaystation6.org / cdnplaystation6.vip / 18comic.vip | 3 个返回 200，3 个 SSL 失败，其余 403 |

由此产生的连带影响（未修复，属服务端问题）：

- `imgHost` 始终为空 → 用户头像、评论头像、下载任务的图片 host 前缀缺失；
- 数据源切到「网络」或「混合」时整个漫画请求链路不可用；
- 「偏好推荐」功能不可用（本记录只让它不再弹提示）。

## 附录 B：版本流水

| 版本 | VERSION_CODE | 内容 |
| --- | --- | --- |
| v1.2.14 | 134 | 静默 `/setting` 404 提示；持久化最快域名加速冷启动；修复加载页闪出主界面 |
| v1.2.15 | 135 | 启动期（初始化未完成）的请求失败不再弹提示（未解决首页 404） |
| v1.2.16 | 136 | 静默 `/promote` 偏好推荐请求，解决首页首次刷新后的 404 |

## 附录 C：关键代码位置

| 文件 | 作用 |
| --- | --- |
| `retrofit/interceptor/ToastInterceptor.kt` | 全局网络错误提示的唯一出口，静默逻辑都在这里 |
| `retrofit/annotation/SilentHttpError.kt` | 标记可降级接口，跳过全局提示 |
| `retrofit/service/RemoteSettingService.kt` | `/setting`，标注静默 |
| `retrofit/service/ComicService.kt` | `/promote`，标注静默 |
| `repository/impl/ComicRepositoryImpl.kt` | 首页加载与偏好推荐分支（约 845~860 行） |
| `repository/impl/EmbeddedClientManager.kt` | 内置客户端与域名探测策略 |
| `storage/FastDomainStorage.kt` | 最快域名持久化 |
| `store/UserManager.kt` | 启动期自动重登 |
| `App.kt` | 启动过渡（splash）判定 |
