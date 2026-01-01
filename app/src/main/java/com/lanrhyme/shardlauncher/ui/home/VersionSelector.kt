package com.lanrhyme.shardlauncher.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.VersionsManager
import com.lanrhyme.shardlauncher.ui.components.LocalCardLayoutConfig
import dev.chrisbanes.haze.hazeEffect
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VersionSelector(
    selectedVersion: Version?,
    versions: List<Version>,
    onVersionSelected: (Version) -> Unit,
    modifier: Modifier = Modifier
) {
    val (isCardBlurEnabled, cardAlpha, hazeState) = LocalCardLayoutConfig.current
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val cardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .then(
                if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.clip(cardShape).hazeEffect(state = hazeState)
                } else Modifier
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
        )
    ) {
        Column {
            // 当前选中的版本显示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 版本图标
                AsyncImage(
                    model = selectedVersion?.let { VersionsManager.getVersionIconFile(it) },
                    contentDescription = "版本图标",
                    placeholder = painterResource(R.drawable.img_minecraft),
                    error = painterResource(R.drawable.img_minecraft),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = selectedVersion?.getVersionName() ?: "未选择版本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (selectedVersion != null) {
                        // 显示版本信息
                        selectedVersion.getVersionInfo()?.let { info ->
                            Text(
                                text = "Minecraft ${info.minecraftVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            info.loaderInfo?.let { loader ->
                                Text(
                                    text = "${loader.loader.displayName} ${loader.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // 显示上次启动时间
                        val lastLaunchTime = selectedVersion.getVersionConfig().lastLaunchTime
                        if (lastLaunchTime > 0) {
                            Text(
                                text = "上次启动: ${dateFormat.format(Date(lastLaunchTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "从未启动",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "点击选择游戏版本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 版本列表
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val listCardShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.clip(listCardShape).hazeEffect(state = hazeState)
                            } else Modifier
                        ),
                    shape = listCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha * 0.5f)
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(versions.filter { it.isValid() }) { version ->
                            VersionListItem(
                                version = version,
                                isSelected = version == selectedVersion,
                                onClick = {
                                    onVersionSelected(version)
                                    expanded = false
                                },
                                isCardBlurEnabled = isCardBlurEnabled,
                                cardAlpha = cardAlpha,
                                hazeState = hazeState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionListItem(
    version: Version,
    isSelected: Boolean,
    onClick: () -> Unit,
    isCardBlurEnabled: Boolean,
    cardAlpha: Float,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val itemCardShape = RoundedCornerShape(8.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() }
            .then(
                if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.clip(itemCardShape).hazeEffect(state = hazeState)
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = cardAlpha * 0.7f)
            else MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha * 0.8f)
        ),
        shape = itemCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 版本图标
            AsyncImage(
                model = VersionsManager.getVersionIconFile(version),
                contentDescription = "版本图标",
                placeholder = painterResource(R.drawable.img_minecraft),
                error = painterResource(R.drawable.img_minecraft),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 置顶图标
                    if (version.pinnedState) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(45f)
                        )
                    }
                }

                // 版本详细信息
                version.getVersionInfo()?.let { info ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = info.minecraftVersion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        info.loaderInfo?.let { loader ->
                            Text(
                                text = loader.loader.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 上次启动时间
            val lastLaunchTime = version.getVersionConfig().lastLaunchTime
            if (lastLaunchTime > 0) {
                Text(
                    text = dateFormat.format(Date(lastLaunchTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "从未启动",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}