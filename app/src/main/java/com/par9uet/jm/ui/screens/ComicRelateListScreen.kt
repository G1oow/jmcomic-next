package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.utils.filterBlockedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ComicRelateListScreen(
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val relateList = remember(comicDetailState.data, localSetting.blockedTagList) {
        comicDetailState.data?.relateComicList?.filterBlockedTags(localSetting.blockedTagList) ?: emptyList()
    }
    CommonScaffold(title = "相关本子") {
        when {
            comicDetailState.isLoading && relateList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            relateList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无相关漫画",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = adaptiveComicGridCells(),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    items(
                        relateList,
                        key = { it.id },
                    ) {
                        Comic(comic = it)
                    }
                }
            }
        }
    }
}
