package com.par9uet.jm.storage

/**
 * 自动检查更新的轻量状态：上次成功检查的时间与用户选择跳过的版本。
 *
 * 只影响提示节奏，丢了最多多提示一次，所以跟其他小体量配置一样走 [SecureStorage]。
 */
class AppUpdateStorage(
    private val secureStorage: SecureStorage,
) {
    fun getLastCheckAt(): Long =
        secureStorage.getString(KEY_LAST_CHECK_AT)?.toLongOrNull() ?: 0L

    fun setLastCheckAt(value: Long) {
        secureStorage.set(KEY_LAST_CHECK_AT, value.toString())
    }

    fun getSkippedVersion(): String =
        secureStorage.getString(KEY_SKIPPED_VERSION).orEmpty()

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
