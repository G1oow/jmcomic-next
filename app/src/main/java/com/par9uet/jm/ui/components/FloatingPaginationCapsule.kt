package com.par9uet.jm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.pagingSource.ComicPageState
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleBottomMargin
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleHeight
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleInnerInset
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleItemHeight
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleMaxWidth
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleSideMargin
import com.par9uet.jm.ui.screens.tabScreen.ChromeCapsuleStadium
import com.par9uet.jm.ui.screens.tabScreen.LocalTabFloatingChrome
import com.par9uet.jm.ui.screens.tabScreen.chromeStackTravelPx

/**
 * 悬浮胶囊分页条：造型与悬浮底栏一致，叠在底栏上方，跟着滚动一起收起。
 *
 * 它不占布局，靠绘制位移滑出屏幕；行程与底栏胶囊共用同一份，整叠胶囊保持相对间距一起
 * 进出，收起过程中不会互相穿插。不在 tab 壳里（没有悬浮底栏）时退回页面底部的普通分页条。
 */
@Composable
fun FloatingComicPaginationCapsule(
    state: ComicPageState,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalTabFloatingChrome.current
    if (chrome == null) {
        ComicPaginationBar(state = state, onPageChange = onPageChange, modifier = modifier)
        return
    }
    val density = LocalDensity.current
    val travelPx = remember(density, chrome.navigationBarInset) {
        chromeStackTravelPx(density, chrome.navigationBarInset, pagerPresent = true)
    }
    // 登记自己叠在底栏上方：底栏胶囊的收起行程要连这一层一起算。
    DisposableEffect(chrome.state) {
        chrome.state.setPagerPresent(true)
        onDispose { chrome.state.setPagerPresent(false) }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ChromeCapsuleSideMargin,
                end = ChromeCapsuleSideMargin,
                bottom = chrome.navigationBarInset + ChromeCapsuleBottomMargin +
                    ChromeCapsuleHeight + ChromeCapsuleBottomMargin,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = ChromeCapsuleMaxWidth)
                .graphicsLayer {
                    // 与底栏胶囊同一个行程，在绘制阶段算，进度变化不触发重组和重测量。
                    translationY = chrome.state.progress * travelPx
                },
            shape = ChromeCapsuleStadium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            PagerCapsuleRow(state = state, onPageChange = onPageChange)
        }
    }
}

@Composable
private fun PagerCapsuleRow(
    state: ComicPageState,
    onPageChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.padding(ChromeCapsuleInnerInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CapsulePagerAction(
            label = "上一页",
            enabled = !state.isLoading && state.page > 1,
            emphasized = false,
            onClick = { onPageChange(state.page - 1) },
        )
        Text(
            text = if (state.isLoading) "…" else state.pageCount?.let { "${state.page} / $it" } ?: "第 ${state.page} 页",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 6.dp),
        )
        CapsulePagerAction(
            label = "下一页",
            enabled = !state.isLoading && state.hasNext,
            emphasized = true,
            onClick = { onPageChange(state.page + 1) },
        )
    }
}

/** 胶囊里的一个分页动作：与底栏入口同高，主按钮用淡强调色铺满。 */
@Composable
private fun CapsulePagerAction(
    label: String,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    // 与底栏选中 pill 同一档的淡强调色，只算一次，避免每次重组重建 Color。
    val containerColor = remember(colorScheme.primary) { colorScheme.primary.copy(alpha = 0.11f) }
    Box(
        modifier = Modifier
            .height(ChromeCapsuleItemHeight)
            .clip(ChromeCapsuleStadium)
            .background(if (emphasized) containerColor else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                emphasized -> colorScheme.primary
                else -> colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}
