package com.lanrhyme.shardlauncher.ui.version

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.ui.components.LocalCardLayoutConfig
import dev.chrisbanes.haze.hazeEffect
import java.io.File

@Composable
fun ResourcePacksManagementScreen(
    version: Version,
    onBack: () -> Unit
) {
    val (isCardBlurEnabled, cardAlpha, hazeState) = LocalCardLayoutConfig.current
    val resourcePacksFolder = File(version.getVersionPath(), "resourcepacks")
    var resourcePackFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // 刷新资源包列表
    LaunchedEffect(refreshTrigger) {
        resourcePackFiles = if (resourcePacksFolder.exists()) {
            resourcePacksFolder.listFiles()?.filter { 
                it.isFile && (it.extension == "zip" || it.isDirectory)
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部操作栏
        val topCardShape = RoundedCornerShape(16.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.clip(topCardShape).hazeEffect(state = hazeState)
                    } else Modifier
                ),
            shape = topCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "资源包管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedButton(
                    onClick = { 
                        // 创建resourcepacks文件夹
                        if (!resourcePacksFolder.exists()) {
                            resourcePacksFolder.mkdirs()
                        }
                        refreshTrigger++
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新")
                }
                
                Button(
                    onClick = {
                        // TODO: 实现添加资源包功能
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加资源包")
                }
            }
        }

        // 资源包列表
        if (resourcePackFiles.isEmpty()) {
            val emptyCardShape = RoundedCornerShape(16.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.clip(emptyCardShape).hazeEffect(state = hazeState)
                        } else Modifier
                    ),
                shape = emptyCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "暂无资源包",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "点击\"添加资源包\"按钮来安装资源包",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resourcePackFiles) { resourcePackFile ->
                    ResourcePackItem(
                        resourcePackFile = resourcePackFile,
                        onDelete = {
                            if (resourcePackFile.isDirectory) {
                                resourcePackFile.deleteRecursively()
                            } else {
                                resourcePackFile.delete()
                            }
                            refreshTrigger++
                        },
                        onToggle = { enabled ->
                            // TODO: 实现资源包启用/禁用功能
                            val newName = if (enabled) {
                                resourcePackFile.name.removeSuffix(".disabled")
                            } else {
                                resourcePackFile.name + ".disabled"
                            }
                            val newFile = File(resourcePackFile.parent, newName)
                            resourcePackFile.renameTo(newFile)
                            refreshTrigger++
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

@Composable
private fun ResourcePackItem(
    resourcePackFile: File,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    isCardBlurEnabled: Boolean,
    cardAlpha: Float,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val isEnabled = !resourcePackFile.name.endsWith(".disabled")
    var showDeleteDialog by remember { mutableStateOf(false) }
    val itemCardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.clip(itemCardShape).hazeEffect(state = hazeState)
                } else Modifier
            ),
        shape = itemCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha * 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 资源包图标
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = resourcePackFile.nameWithoutExtension.removeSuffix(".disabled"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isEnabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (resourcePackFile.isDirectory) "文件夹" else "${(resourcePackFile.length() / 1024).toInt()} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 启用/禁用开关
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )

            // 删除按钮
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除资源包",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除资源包") },
            text = { Text("确定要删除资源包 ${resourcePackFile.nameWithoutExtension} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}