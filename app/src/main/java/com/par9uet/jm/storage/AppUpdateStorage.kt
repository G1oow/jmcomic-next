package com.par9uet.jm.storage

/**
 * 自动检查更新的轻量状态：用户选择跳过的版本。
 *
 * 只影响提示节奏，丢了最多多提示一次，所以跟其他小体量配置一样走 [SecureStorage]。
 */
class AppUpdateStorage(
    private val secureStorage: SecureStorage,
) {
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
        const val KEY_SKIPPED_VERSION = "app_update_skipped_version"
    }
}
