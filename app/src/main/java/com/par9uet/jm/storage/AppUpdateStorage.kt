package com.par9uet.jm.storage

/**
 * 自动检查更新的轻量状态：上次成功检查的时间与用户选择跳过的版本。
 *
 * 只影响提示节奏，丢了最多多提示一次，所以跟其他小体量配置一样走 [SecureStorage]。
 *
 * 注意：SecureStorage 存的是 JSON，写 String 会带上引号。时间戳按数字写入、按数字解析；
 * 1.2.7 曾把它当字符串存成 `"1758..."`，这类遗留值一律丢弃，重新计时。
 */
class AppUpdateStorage(
    private val secureStorage: SecureStorage,
) {
    fun getLastCheckAt(): Long {
        val raw = secureStorage.getString(KEY_LAST_CHECK_AT).orEmpty()
        // 带引号的是旧版本写坏的 JSON 字符串，不能再参与节流判断。
        if (raw.startsWith("\"")) return 0L
        return raw.toLongOrNull() ?: 0L
    }

    fun setLastCheckAt(value: Long) {
        secureStorage.set(KEY_LAST_CHECK_AT, value)
    }

    fun getSkippedVersion(): String =
        secureStorage.get<String>(KEY_SKIPPED_VERSION, String::class.java).orEmpty()

    /** 传空字符串表示清除跳过记录。 */
    fun setSkippedVersion(version: String) {
        if (version.isBlank()) {
            secureStorage.remove(KEY_SKIPPED_VERSION)
        } else {
            secureStorage.set(KEY_SKIPPED_VERSION, version)
        }
    }

    private companion object {
        const val KEY_LAST_CHECK_AT = "app_update_last_check_at"
        const val KEY_SKIPPED_VERSION = "app_update_skipped_version"
    }
}
