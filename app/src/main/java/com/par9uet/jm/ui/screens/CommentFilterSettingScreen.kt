package com.par9uet.jm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SpeakerNotesOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.utils.CommentFilterPreset
import com.par9uet.jm.utils.DEFAULT_COMMENT_FILTER_PRESETS
import com.par9uet.jm.utils.parseCommentKeywordInput
import com.par9uet.jm.utils.resolveActiveCommentKeywords
import org.koin.compose.getKoin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentFilterSettingScreen(
    localSettingManager: LocalSettingManager = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val enabled = localSetting.commentKeywordFilterEnabled
    val customKeywords = localSetting.commentBlockedKeywords
    val enabledPresetIds = localSetting.commentEnabledPresetIds
    val activeKeywords = remember(enabled, customKeywords, enabledPresetIds) {
        resolveActiveCommentKeywords(
            enabled = enabled,
            customKeywords = customKeywords,
            enabledPresetIds = enabledPresetIds,
        )
    }

    var keywordInput by remember { mutableStateOf("") }
    var expandedPresetIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearDialog by remember { mutableStateOf(false) }

    val allPresetIds = remember { DEFAULT_COMMENT_FILTER_PRESETS.map { it.id } }
    val allPresetsEnabled = enabledPresetIds.size == allPresetIds.size

    fun submitKeywords() {
        val parsed = parseCommentKeywordInput(keywordInput)
        if (parsed.isEmpty()) return
        val existingSet = customKeywords.map { it.lowercase() }.toSet()
        val addedCount = parsed.count { it.lowercase() !in existingSet }
        localSettingManager.addCommentBlockedKeywords(parsed)
        if (!enabled) {
            localSettingManager.updateCommentKeywordFilterEnabled(true)
        }
        keywordInput = ""
        if (addedCount > 0) {
            toastManager.showAsync("已添加 $addedCount 个屏蔽关键词")
        } else {
            toastManager.showAsync("输入的关键词已在列表中")
        }
    }

    CommonScaffold(title = "评论区关键词屏蔽") {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "启用评论关键词屏蔽",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (enabled) {
                                    "当前生效 ${activeKeywords.size} 个关键词（含回复与广告昵称匹配，忽略大小写与全角）"
                                } else {
                                    "开启后自动隐藏命中预设或自定义关键词的评论区广告与回复"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = localSettingManager::updateCommentKeywordFilterEnabled,
                        )
                    }
                }
            }

            item(key = "custom_editor") {
                CustomKeywordEditorCard(
                    keywordInput = keywordInput,
                    onKeywordInputChange = { keywordInput = it },
                    onAddKeywords = { submitKeywords() },
                    customKeywords = customKeywords,
                    onRemoveKeyword = localSettingManager::removeCommentBlockedKeyword,
                    onRequestClear = { showClearDialog = true },
                )
            }

            item(key = "preset_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "广告拦截预设",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "已启用 ${enabledPresetIds.size} / ${DEFAULT_COMMENT_FILTER_PRESETS.size} 组预设",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            if (allPresetsEnabled) {
                                localSettingManager.updateCommentEnabledPresetIds(emptyList())
                            } else {
                                localSettingManager.updateCommentEnabledPresetIds(allPresetIds)
                                if (!enabled) {
                                    localSettingManager.updateCommentKeywordFilterEnabled(true)
                                }
                            }
                        }
                    ) {
                        Text(if (allPresetsEnabled) "全部关闭" else "一键全开")
                    }
                }
            }

            items(
                items = DEFAULT_COMMENT_FILTER_PRESETS,
                key = { it.id },
            ) { preset ->
                val presetEnabled = preset.id in enabledPresetIds
                val expanded = preset.id in expandedPresetIds
                PresetCard(
                    preset = preset,
                    enabled = presetEnabled,
                    expanded = expanded,
                    onToggleEnabled = { nextEnabled ->
                        localSettingManager.setCommentFilterPresetEnabled(preset.id, nextEnabled)
                        if (nextEnabled && !enabled) {
                            localSettingManager.updateCommentKeywordFilterEnabled(true)
                        }
                    },
                    onToggleExpanded = {
                        expandedPresetIds = if (expanded) {
                            expandedPresetIds - preset.id
                        } else {
                            expandedPresetIds + preset.id
                        }
                    },
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空自定义关键词") },
            text = { Text("确定要移除全部 ${customKeywords.size} 个自定义屏蔽关键词吗？已启用的广告预设不受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        localSettingManager.clearCommentBlockedKeywords()
                        showClearDialog = false
                        toastManager.showAsync("已清空自定义关键词")
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomKeywordEditorCard(
    keywordInput: String,
    onKeywordInputChange: (String) -> Unit,
    onAddKeywords: () -> Unit,
    customKeywords: List<String>,
    onRemoveKeyword: (String) -> Unit,
    onRequestClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自定义屏蔽词",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "支持用逗号、分号或换行一次添加多个关键词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (customKeywords.isNotEmpty()) {
                    IconButton(onClick = onRequestClear) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "清空自定义关键词",
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = keywordInput,
                    onValueChange = onKeywordInputChange,
                    placeholder = { Text("输入广告词，如：加v，看主页") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddKeywords() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    trailingIcon = {
                        if (keywordInput.isNotEmpty()) {
                            IconButton(onClick = { onKeywordInputChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空输入")
                            }
                        }
                    },
                )
                FilledTonalButton(
                    enabled = keywordInput.isNotBlank(),
                    onClick = onAddKeywords,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "添加关键词",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (customKeywords.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SpeakerNotesOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "还没有自定义屏蔽词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "可在上方输入自定义词，或直接开启下方的广告拦截预设。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            } else {
                Text(
                    text = "已添加 ${customKeywords.size} 个自定义关键词",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    customKeywords.forEach { keyword ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Text(
                                    text = keyword,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "移除 $keyword",
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .size(16.dp)
                                        .clickable { onRemoveKeyword(keyword) },
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                trailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetCard(
    preset: CommentFilterPreset,
    enabled: Boolean,
    expanded: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleExpanded: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) {
                        "收起预设词表（${preset.keywords.size} 词）"
                    } else {
                        "查看预设词表（${preset.keywords.size} 词）"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "收起预设词表" else "展开预设词表",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(2.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    preset.keywords.forEach { keyword ->
                        AssistChip(
                            border = null,
                            onClick = {},
                            label = {
                                Text(
                                    text = keyword,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (enabled) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                labelColor = if (enabled) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}
