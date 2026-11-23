package com.par9uet.jm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.par9uet.jm.store.ToastManager
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * 调起系统安装器安装已经下载好的 APK。
 *
 * Android 8.0 起安装未知来源应用需要单独授权，没有授权时系统安装器只会弹「无法安装」，
 * 所以这里先申请授权再拉起安装器。
 */
fun installApk(context: Context, savedPath: String) {
    val file = File(savedPath)
    if (!file.exists()) {
        showInstallMessage(context, "安装包文件不存在，请重新下载")
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        showInstallMessage(context, "请允许安装未知来源应用后重试")
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { error ->
            logError("AppUpdate", "打开未知来源授权页失败: ${error.stackTraceToString()}")
        }
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        logError("AppUpdate", "打开系统安装器失败: ${error.stackTraceToString()}")
        showInstallMessage(context, "打开安装器失败：${error.message ?: "未知错误"}")
    }
}

/** 优先走全局 Snackbar；Koin 尚未就绪时退回系统 Toast。 */
private fun showInstallMessage(context: Context, message: String) {
    runCatching {
        GlobalContext.get().get<ToastManager>().showAsync(message)
    }.onFailure {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
