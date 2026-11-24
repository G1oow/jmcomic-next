package com.par9uet.jm.storage

/**
 * 记录内置 API 上次探测出的最快域名。
 *
 * 冷启动时优先用它放行首批请求，避免每次都等完整的串行域名探测；
 * 探测结果之后再回写，保证缓存不会长期停留在失效线路上。
 */
class FastDomainStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "embedded_fast_domain"
    }

    fun get(): String? = secureStorage.get<String>(STORAGE_KEY, String::class.java)?.takeIf { it.isNotBlank() }

    fun set(domain: String) {
        if (domain.isBlank()) return
        secureStorage.set(STORAGE_KEY, domain)
    }

    fun remove() {
        secureStorage.remove(STORAGE_KEY)
    }
}
