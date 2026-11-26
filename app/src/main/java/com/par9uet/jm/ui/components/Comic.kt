package com.par9uet.jm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.ui.theme.LocalAppMotion
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Comic(
    comic: Comic,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onToggleSelected: (() -> Unit)? = null,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalAppMotion.current
    val colorScheme = MaterialTheme.colorScheme
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.975f else 1f,
        animationSpec = tween(if (motion.enabled) 100 else 0),
        label = "comic-press",
    )
    val borderColor = remember(
        colorScheme.outlineVariant,
        colorScheme.primary,
        editing,
        selected,
    ) {
        if (editing && selected) {
            colorScheme.primary.copy(alpha = 0.7f)
        } else {
            colorScheme.outlineVariant.copy(alpha = 0.28f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = {
                    if (editing && onToggleSelected != null) {
                        onToggleSelected()
                    } else {
                        comicDetailViewModel.reset(comic.id)
                        mainNavController.navigate("comicDetail/${comic.id}")
                    }
                },
                onLongClick = {
                    onLongClick?.invoke()
                }
            ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (editing && selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box {
                ComicCoverImage(comic, modifier = Modifier.fillMaxWidth())
                if (editing && selected) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                    )
                }
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp),
                text = comic.name,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                text = if (localSetting.showComicIdAsSubtitle) {
                    "JM${comic.id}"
                } else {
                    comic.authorList.joinToString(", ").ifBlank { "暂无作者" }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
