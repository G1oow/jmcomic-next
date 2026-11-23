package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.AppRelease
import com.par9uet.jm.store.AppUpdateDownloadManager
import com.par9uet.jm.store.AppUpdateDownloadRequest
import com.par9uet.jm.store.AppUpdateDownloadState
import com.par9uet.jm.store.AppUpdateDownloadStatus
import com.par9uet.jm.store.formatBytes
import com.par9uet.jm.utils.MarkdownText
import com.par9uet.jm.utils.installApk
import org.koin.compose.getKoin
import java.io.File
import kotlin.math.roundToInt

/**
 * 启动时检测到新版本的提示框。
 *
 * 下载与安装复用设置页那套 [AppUpdateDownloadManager] + [installApk]，
 * 所以在这里开始下载后，即便用户关掉弹窗也能从通知或检查更新页继续。
 */
@Composable
fun AppUpdateDialog(
    release: AppRelease,
    installedVersion: String,
    onDismiss: () -> Unit,
    onSkipVersion: () -> Unit,
    downloadManager: AppUpdateDownloadManager = getKoin().get(),
) {
    val context = LocalContext.current
    val downloadState by downloadManager.state.collectAsState()
    // 只认这一版自己的下载状态，避免上一次下载的残留状态串到新版本上。
    val current = if (downloadState.version == release.version) {
        downloadState
    } else {
        AppUpdateDownloadState()
    }
    val downloading = current.status == AppUpdateDownloadStatus.Downloading ||
        current.status == AppUpdateDownloadStatus.Paused
    val downloadedFile = current.savedPath.takeIf { path ->
        current.status == AppUpdateDownloadStatus.Completed && path.isNotBlank() && File(path).exists()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = if (downloadedFile != null) "更新已下载" else "发现新版本 v${release.version}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "当前版本 v$installedVersion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    downloadedFile != null -> Text(
                        text = "安装包已下载完成，点击「立即安装」完成更新。",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    downloading -> {
                        LinearProgressIndicator(
                            progress = { current.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = if (current.status == AppUpdateDownloadStatus.Paused) {
                                "已暂停 · ${(current.progress * 100).roundToInt()}%"
                            } else {
                                "${(current.progress * 100).roundToInt()}% · " +
                                    "${formatBytes(current.speedBytesPerSecond)}/s"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    current.status == AppUpdateDownloadStatus.Error -> Text(
                        text = "下载失败：${current.errorMessage.ifBlank { "请稍后重试" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    else -> UpdateNotes(release)
                }
            }
        },
        confirmButton = {
            when {
                downloadedFile != null -> Button(onClick = { installApk(context, downloadedFile) }) {
                    Text("立即安装")
                }

                downloading -> TextButton(
                    onClick = {
                        downloadManager.sendToBackground()
                        onDismiss()
                    },
                ) { Text("后台下载") }

                else -> Button(
                    onClick = {
                        downloadManager.start(
                            AppUpdateDownloadRequest(
                                version = release.version,
                                fileName = release.fileName.ifBlank { "jm-mobile_v${release.version}.apk" },
                                downloadUrl = release.downloadUrl,
                            )
                        )
                    },
                    enabled = release.hasDownload,
                ) {
                    Text(if (current.status == AppUpdateDownloadStatus.Error) "重试" else "立即更新")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!downloading && downloadedFile == null && release.hasDownload) {
                    TextButton(onClick = onSkipVersion) { Text("跳过此版本") }
                }
                TextButton(
                    onClick = {
                        if (downloading) downloadManager.cancel()
                        onDismiss()
                    },
                ) {
                    Text(if (downloading) "取消下载" else "稍后")
                }
            }
        },
    )
}

/** 发布说明：只给固定高度，正文过长时内部滚动。 */
@Composable
private fun UpdateNotes(release: AppRelease) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        MarkdownText(
            markdown = release.body.ifBlank { release.name.ifBlank { "GitHub 上发布了新版本。" } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
