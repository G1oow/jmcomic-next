package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.retrofit.annotation.SilentHttpError
import com.par9uet.jm.store.ToastManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

class ToastInterceptor(
    private val toastManager: ToastManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (!response.isSuccessful && !isSilent(chain.request())) {
            toastManager.showAsync("网络错误: ${response.code}")
        }
        return response
    }

    // 启动期的降级请求由 AppInitTask 自行上报失败，不该再弹一次全局提示
    private fun isSilent(request: Request): Boolean {
        val method = request.tag(Invocation::class.java)?.method() ?: return false
        return method.getAnnotation(SilentHttpError::class.java) != null
    }
}
