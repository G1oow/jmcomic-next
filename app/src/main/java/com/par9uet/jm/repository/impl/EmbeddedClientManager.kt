package com.par9uet.jm.repository.impl

import android.os.Build
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.storage.FastDomainStorage
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.applyTlsCompat
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import io.github.jukomu.jmcomic.core.net.provider.JmDomainManager
import okhttp3.Cookie
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * 共享内置 API 客户端管理器。
 * 确保 UserRepositoryImpl 和 ComicRepositoryImpl 使用同一个 JmApiClient 实例，
 * 这样 login() 设置的 loggedInUserName 和内部 Cookie 状态可以被所有 Repository 共享。
 * 解决内置 API 模式下 POST 请求（如创建收藏夹）返回 401 "請先登入會員" 的问题。
 *
 * Android 6 uses verified bootstrap domains immediately, then updates domains in
 * a daemon thread. Startup never waits for the library's background probe to finish.
 */
class EmbeddedClientManager(
    private val cookieStorage: CookieStorage,
    private val dohManager: DohManager,
    private val fastDomainStorage: FastDomainStorage,
) {
    /** Android 6 fallback list; Android 7+ can use the library's dynamic list. */
    private val androidCompatibleApiDomains = listOf(
        "www.cdngwc.net",
        "www.cdngwc.club",
        "www.cdnhjk.net",
        "www.cdngwc.cc",
    )

    // The Java API executes getFavorites synchronously. Bind its missing sort
    // parameter to that request thread, not shared state changed by preloading.
    private val favoriteOrder = ThreadLocal<String>()

    @Volatile
    private var client: JmApiClient? = null

    fun getClient(): JmApiClient {
        return client ?: synchronized(this) {
            client ?: createClient().also { client = it }
        }
    }

    fun <T> withFavoriteOrder(order: String, block: () -> T): T {
        val previous = favoriteOrder.get()
        favoriteOrder.set(order)
        return try {
            block()
        } finally {
            if (previous == null) favoriteOrder.remove() else favoriteOrder.set(previous)
        }
    }

    private fun createClient(): JmApiClient {
        val legacyAndroid = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
        val cachedDomain = if (legacyAndroid) null else fastDomainStorage.get()
        val configBuilder = JmConfiguration.Builder()
            .clientType(ClientType.API)
            .timeout(Duration.ofSeconds(12))
            .imageTimeout(Duration.ofSeconds(60))
            .downloadThreadPoolSize(2)
            .domainProbeTimeoutMs(1500)
        if (legacyAndroid) {
            // Java APIs are desugared; explicit seeds also cover background bootstrap failure.
            configBuilder.apiDomains(androidCompatibleApiDomains)
        }
        val config = configBuilder.build()
        val context = OkHttpBuilder.build(config)
        val domainManager = context.domainManager

        if (cachedDomain != null) {
            // 上次探测出的最快域名排首位，库的默认线路继续作为兜底
            domainManager.updateDomains(preferDomain(domainManager.getDomains(), cachedDomain))
            // 直接放行首批请求，不等启动期的串行探测
            domainManager.setInitialized(true)
        }
        val clientWithCookieInjection = context.client.newBuilder()
            .dns(dohManager)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                // 收藏详情标签按批并发请求；将同主机上限与批量规模对齐，
                // 避免默认 5 个连接把并发请求额外排队。
                maxRequests = 16
                maxRequestsPerHost = 8
            })
            .applyTlsCompat()
            .addInterceptor { chain ->
                val cookies = cookieStorage.get()
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                if (original.method == "GET" && original.url.encodedPath.endsWith("/favorite")) {
                    requestBuilder.url(
                        original.url.newBuilder()
                            .setQueryParameter("o", favoriteOrder.get() ?: "mr")
                            .build()
                    )
                }
                if (cookies.isNotEmpty()) {
                    val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    requestBuilder
                        .header("Cookie", cookieHeader)
                }
                val request = requestBuilder.build()
                val response = chain.proceed(request)
                // 从响应头提取 Set-Cookie，同步到 cookieStorage，保证登录态持久化
                val setCookieHeaders = response.headers("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    val newCookies = setCookieHeaders.mapNotNull { Cookie.parse(request.url, it) }
                    if (newCookies.isNotEmpty()) {
                        val existing = cookieStorage.get().toMutableList()
                        val newKeys = newCookies.map { "${it.domain}:${it.path}:${it.name}" }.toSet()
                        existing.removeAll { "${it.domain}:${it.path}:${it.name}" in newKeys }
                        existing.addAll(newCookies)
                        cookieStorage.set(existing)
                    }
                }
                response
            }
            .build()
        val jmClient = JmApiClient(config, clientWithCookieInjection, context.cookieManager, domainManager)
        // v1.1.12 起初始化不再由构造函数触发，必须显式启动，否则 getBestDomain() 会一直阻塞。
        // 有缓存域名时先让首批请求跑起来，把串行探测挪到后台，避免启动期被整体阻塞。
        if (cachedDomain == null) {
            rememberBestDomain(jmClient, domainManager)
        } else {
            startDelayedDomainProbe(jmClient, domainManager)
        }

        if (legacyAndroid) {
            domainManager.setInitialized(true)
            Thread({
                try {
                    jmClient.updateDomains()
                } catch (error: Exception) {
                    log("EmbeddedClientManager: 域名更新失败，保留启动线路：${error.message}")
                } catch (error: LinkageError) {
                    log("EmbeddedClientManager: 旧版系统域名更新兼容回退：${error.message}")
                } finally {
                    domainManager.setInitialized(true)
                }
            }, "embedded-legacy-domain-update").apply {
                isDaemon = true
                start()
            }
        }

        return jmClient
    }

    private fun preferDomain(domains: List<String>, preferred: String): List<String> =
        listOf(preferred) + domains.filter { it != preferred }

    // 跑库的完整初始化（拉域名列表 + 串行探测），完成后把最快域名记下来
    private fun rememberBestDomain(jmClient: JmApiClient, domainManager: JmDomainManager) {
        try {
            jmClient.initializeAsync().whenComplete { _, error ->
                if (error != null) return@whenComplete
                try {
                    val best = domainManager.getBestDomain()
                    if (!best.isNullOrBlank()) {
                        // 探测会整体替换域名列表，这里把最优线路重新置顶，
                        // 否则后续请求会落到探测结果原本的顺序上
                        domainManager.updateDomains(preferDomain(domainManager.getDomains(), best))
                        fastDomainStorage.set(best)
                    }
                } catch (_: Exception) {
                    // 探测后仍取不到域名时保留原缓存
                }
            }
        } catch (error: Exception) {
            log("EmbeddedClientManager: 后台域名探测失败：${error.message}")
        }
    }

    // 缓存域名先服务首批请求，稍后再探测：冷启动不必等探测，同时把下次启动要用的最快域名刷出来。
    private fun startDelayedDomainProbe(jmClient: JmApiClient, domainManager: JmDomainManager) {
        Thread({
            try {
                Thread.sleep(DOMAIN_PROBE_DELAY_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return@Thread
            }
            rememberBestDomain(jmClient, domainManager)
        }, "embedded-domain-probe").apply {
            isDaemon = true
            start()
        }
    }

    private companion object {
        // 等首批请求和首页加载走完再探测，避免探测整体替换域名列表打乱正在用的线路
        const val DOMAIN_PROBE_DELAY_MS = 15_000L
    }
}
