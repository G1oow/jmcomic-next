package com.par9uet.jm.retrofit.service

import com.par9uet.jm.retrofit.annotation.GInit
import com.par9uet.jm.retrofit.annotation.SilentHttpError
import com.par9uet.jm.retrofit.model.RemoteSettingResponse
import com.par9uet.jm.retrofit.model.ResponseWrapper
import retrofit2.http.GET

interface RemoteSettingService {
    @GInit
    // 远端设置属于可降级的启动数据，取不到时保留本地默认值，不弹网络错误提示
    @SilentHttpError
    @GET("setting")
    suspend fun getRemoteSetting(): ResponseWrapper<RemoteSettingResponse>
}