package com.par9uet.jm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.par9uet.jm.ui.theme.LocalAppMotion
import com.par9uet.jm.ui.theme.motionEasing
import com.par9uet.jm.ui.theme.motionExitEasing
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopButton(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val visible by remember(gridState) {
        derivedStateOf {
            val visibleItemCount = gridState.layoutInfo.visibleItemsInfo.size
            visibleItemCount > 0 && gridState.firstVisibleItemIndex >= visibleItemCount
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val motion = LocalAppMotion.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (motion.enabled) {
            fadeIn(tween(motion.exitMs, easing = motionEasing)) +
                scaleIn(initialScale = 0.88f, animationSpec = tween(motion.exitMs, easing = motionEasing))
        } else {
            EnterTransition.None
        },
        exit = if (motion.enabled) {
            fadeOut(tween(motion.exitMs, easing = motionExitEasing)) +
                scaleOut(targetScale = 0.88f, animationSpec = tween(motion.exitMs, easing = motionExitEasing))
        } else {
            ExitTransition.None
        },
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    if (motion.enabled) {
                        gridState.animateScrollToItem(0)
                    } else {
                        gridState.scrollToItem(0)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "回到顶部",
            )
        }
    }
}
