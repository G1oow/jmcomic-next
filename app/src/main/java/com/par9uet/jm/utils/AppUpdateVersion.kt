package com.par9uet.jm.utils

/**
 * 应用更新检查的来源：本 fork 的 GitHub Release。
 *
 * 更新包由 `.github/workflows/android-release.yml` 以 `v{VERSION_NAME}` 标签自动发布，
 * 这里的仓库地址必须和 workflow 的发布目标保持一致，否则检查更新会读不到自己的版本。
 */
const val APP_UPDATE_REPO_OWNER = "G1oow"
const val APP_UPDATE_REPO_NAME = "jmcomic-next"
const val APP_UPDATE_REPO_URL = "https://github.com/$APP_UPDATE_REPO_OWNER/$APP_UPDATE_REPO_NAME"
const val APP_UPDATE_RELEASES_URL = "$APP_UPDATE_REPO_URL/releases"
const val APP_UPDATE_LATEST_RELEASE_API =
    "https://api.github.com/repos/$APP_UPDATE_REPO_OWNER/$APP_UPDATE_REPO_NAME/releases/latest"

/** Release 里的一个下载资源。 */
data class ReleaseApkAsset(val name: String, val downloadUrl: String)

/** `v1.2.6`、`1.2.6-beta1`、`1.2.6 更新` 统一归一成 `1.2.6`。 */
fun normalizeVersion(value: String): String = value.trim()
    .removePrefix("v")
    .removePrefix("V")
    .substringBefore(' ')
    .substringBefore('-')

/** 逐段比较语义化版本：left 比 right 新时返回正数，相同时返回 0。 */
fun compareVersion(left: String, right: String): Int {
    val leftParts = normalizeVersion(left).split('.').map { it.toIntOrNull() ?: 0 }
    val rightParts = normalizeVersion(right).split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
        val leftPart = leftParts.getOrElse(index) { 0 }
        val rightPart = rightParts.getOrElse(index) { 0 }
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }
    return 0
}

/** 系统读不到版本号时的占位值。 */
const val UNKNOWN_APP_VERSION = "unknown"

/** Release 版本是否比当前安装的版本更新。 */
fun isUpdateAvailable(remoteVersion: String, installedVersion: String): Boolean {
    val installed = normalizeVersion(installedVersion)
    // 读不到已安装版本时宁可不提示，也别每次启动都弹一次更新框。
    if (installed.isBlank() || installed == UNKNOWN_APP_VERSION) return false
    return compareVersion(remoteVersion, installed) > 0
}

/**
 * 从 Release 资源中挑出可下载的 APK。
 *
 * 优先匹配文件名里带版本号的那个（`jm-mobile_v{version}_{hash}.apk`），
 * 否则退回第一个 APK，避免发布时改了命名规则就完全下载不了。
 */
fun selectApkAsset(assets: List<ReleaseApkAsset>, version: String): ReleaseApkAsset? {
    val apkAssets = assets.filter {
        it.name.endsWith(".apk", ignoreCase = true) && it.downloadUrl.isNotBlank()
    }
    val expected = "jm-mobile_v${normalizeVersion(version)}"
    return apkAssets.firstOrNull { it.name.contains(expected, ignoreCase = true) }
        ?: apkAssets.firstOrNull()
}
