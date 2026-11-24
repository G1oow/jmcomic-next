package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.retrofit.annotation.SilentHttpError
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.store.ToastManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

class ToastInterceptor(
    private val toastManager: ToastManager,
    private val initManager: InitManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // 启动期的失败由 AppInitTask 自行上报，不弹全局提示；
        // 初始化完成后用户主动操作导致的失败才提示。
        if (!response.isSuccessful && initManager.deferred.isCompleted && !isSilent(chain.request())) {
            toastManager.showAsync("网络错误: ${response.code}")
        }
        return response
    }

    // 显式标记为静默的接口，例如可降级的远端配置
    private fun isSilent(request: Request): Boolean {
        val method = request.tag(Invocation::class.java)?.method() ?: return false
        return method.getAnnotation(SilentHttpError::class.java) != null
    }
}
