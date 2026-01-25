package com.lanrhyme.shardlauncher.ui.components.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter.State
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.lanrhyme.shardlauncher.model.MusicItem
import com.lanrhyme.shardlauncher.ui.components.basic.ShardDropdownMenu
import com.lanrhyme.shardlauncher.ui.components.basic.TitleAndSummary
import com.lanrhyme.shardlauncher.ui.components.basic.selectableCard

/**
 * 音乐卡片组件
 * 显示音乐的封面、标题、艺术家信息，并支持点击播放和长按删除
 *
 * @param item 音乐数据项
 * @param isSelected 是否被选中
 * @param onCLick 点击回调
 * @param onDelete 删除回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicCard(
    item: MusicItem,
    isSelected: Boolean,
    onCLick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showDeleteMenu by remember { mutableStateOf(false) }

    Card(
        modifier =
        Modifier.fillMaxWidth()
            .height(70.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showDeleteMenu = true })
            }
            .clickable(onClick = onCLick)
            .selectableCard(isSelected, isPressed)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCLick,
                onLongClick = { showDeleteMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        border =
        if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = item.albumArtUri,
                contentDescription = "专辑封面",
                modifier = Modifier.aspectRatio(1f),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is State.Success -> {
                        SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())
                    }
                    else -> { // // 错误，加载中，空时使用默认封面
                        Box(
                            modifier =
                            Modifier.fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = "专辑封面占位符",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Title and Summary
            Box(modifier = Modifier.padding(16.dp)) {
                TitleAndSummary(title = item.title, summary = item.artist)
            }

            // 删除菜单
            ShardDropdownMenu(expanded = showDeleteMenu, onDismissRequest = { showDeleteMenu = false }) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { // TODO:i18n
                        onDelete()
                        showDeleteMenu = false
                    }
                )
            }
        }
    }
}
