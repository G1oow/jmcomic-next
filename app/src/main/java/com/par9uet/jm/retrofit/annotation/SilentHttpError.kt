package com.par9uet.jm.retrofit.annotation

// 被该注解标记的 service 方法在非 2xx 响应时只把结果交给调用方，不弹全局网络错误提示
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SilentHttpError
