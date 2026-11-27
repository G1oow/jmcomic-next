package com.par9uet.jm.store

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.storage.AppUpdateStorage
import com.par9uet.jm.utils.APP_UPDATE_LATEST_RELEASE_API
import com.par9uet.jm.utils.APP_UPDATE_RELEASES_URL
import com.par9uet.jm.utils.ReleaseApkAsset
import com.par9uet.jm.utils.UNKNOWN_APP_VERSION
import com.par9uet.jm.utils.applyTlsCompat
import com.par9uet.jm.utils.isUpdateAvailable
import com.par9uet.jm.utils.logError
import com.par9uet.jm.utils.normalizeVersion
import com.par9uet.jm.utils.selectApkAsset
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** 一次 GitHub Release 的解读结果。 */
data class AppRelease(
    val version: String,
    val name: String,
    val url: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String,
) {
    val hasDownload: Boolean
        get() = downloadUrl.isNotBlank()
}

/**
 * 自动检查更新的状态。
 *
 * [release] 不为空才表示需要向用户提示；检查失败只记日志和 [errorMessage]，
 * 不打断启动流程。
 */
data class AppUpdateState(
    val checking: Boolean = false,
    val release: AppRelease? = null,
    val errorMessage: String = "",
)

/**
 * 应用更新检查与提示的唯一入口。
 *
 * 数据来自本 fork 的 GitHub Release（见 [APP_UPDATE_LATEST_RELEASE_API]），
 * 下载交给 [AppUpdateDownloadManager]，安装交给 utils 的 installApk。
 */
class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val storage: AppUpdateStorage,
    private val dohManager: DohManager,
) {
    private val client = OkHttpClient.Builder()
        .dns(dohManager)
        .applyTlsCompat()
        .build()

    private val _state = MutableStateFlow(AppUpdateState())
    val state = _state.asStateFlow()

    /** 进程内只发一次启动检查：解锁、设置加载都会让上层的 LaunchedEffect 重跑。 */
    private val launchCheckStarted = AtomicBoolean(false)

    /** 已安装版本；读不到时用 [UNKNOWN_APP_VERSION]，此时不会判为有更新。 */
    val installedVersion: String by lazy {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { UNKNOWN_APP_VERSION }
    }

    /**
     * 启动时静默检查：每次冷启动请求一次，发现新版本就提示；被「跳过此版本」的版本不再提示。
     */
    fun checkOnLaunch() {
        if (!launchCheckStarted.compareAndSet(false, true)) return
        scope.launch { check(showPrompt = true) }
    }

    /** 设置页的手动检查：不受「跳过此版本」限制。 */
    suspend fun refresh(): Result<AppRelease> {
        val result = fetchLatestRelease()
        return result
    }

    /** 关闭提示；[skipVersion] 为 true 时记住不再提示这一版。 */
    fun dismissPrompt(skipVersion: Boolean = false) {
        val release = _state.value.release
        if (skipVersion && release != null) storage.setSkippedVersion(release.version)
        _state.update { it.copy(release = null) }
    }

    private suspend fun check(showPrompt: Boolean) {
        _state.update { it.copy(checking = true) }
        fetchLatestRelease().fold(
            onSuccess = { release ->
                val newer = isUpdateAvailable(release.version, installedVersion)
                val skipped = storage.getSkippedVersion()
                val shouldPrompt = showPrompt && newer && release.hasDownload && release.version != skipped
                _state.update {
                    it.copy(
                        checking = false,
                        release = if (shouldPrompt) release else null,
                        errorMessage = "",
                    )
                }
            },
            onFailure = { error ->
                logError("AppUpdate", "检查更新失败: ${error.message}")
                _state.update {
                    it.copy(checking = false, errorMessage = error.message.orEmpty())
                }
            },
        )
    }

    private suspend fun fetchLatestRelease(): Result<AppRelease> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(APP_UPDATE_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "jmcomic-next-android")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("GitHub 返回 ${response.code}")
                }
                val body = response.body?.string() ?: error("GitHub 返回空响应")
                val json = JsonParser.parseString(body).asJsonObject
                val tagName = json.stringOrEmpty("tag_name")
                val name = json.stringOrEmpty("name")
                val version = normalizeVersion(tagName.ifBlank { name })
                if (version.isBlank()) error("未读取到 Release 版本号")
                val asset = selectApkAsset(json.releaseAssets(), version)
                AppRelease(
                    version = version,
                    name = name,
                    url = json.stringOrEmpty("html_url").ifBlank { "$APP_UPDATE_RELEASES_URL/tag/$tagName" },
                    body = json.stringOrEmpty("body"),
                    downloadUrl = asset?.downloadUrl.orEmpty(),
                    fileName = asset?.name.orEmpty(),
                )
            }
        }
    }
}

private fun JsonObject.stringOrEmpty(key: String): String =
    get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()

private fun JsonObject.releaseAssets(): List<ReleaseApkAsset> {
    val array: JsonArray = getAsJsonArray("assets") ?: return emptyList()
    return array.mapNotNull { element ->
        val asset = element.asJsonObject
        val name = asset.stringOrEmpty("name")
        val url = asset.stringOrEmpty("browser_download_url")
        if (name.isBlank() || url.isBlank()) null else ReleaseApkAsset(name, url)
    }
}
