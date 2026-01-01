package com.lanrhyme.shardlauncher.ui.version

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.game.path.GamePath
import com.lanrhyme.shardlauncher.game.path.GamePathManager
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.VersionsManager
import com.lanrhyme.shardlauncher.ui.components.MarqueeText
import com.lanrhyme.shardlauncher.ui.components.SimpleAlertDialog
import com.lanrhyme.shardlauncher.ui.components.SimpleEditDialog
import com.lanrhyme.shardlauncher.ui.components.SimpleTaskDialog
import com.lanrhyme.shardlauncher.utils.logging.Logger.lError
import com.lanrhyme.shardlauncher.utils.string.getMessageOrToString
import com.lanrhyme.shardlauncher.utils.string.isNotEmptyOrBlank
import kotlinx.coroutines.Dispatchers

sealed interface GamePathOperation {
    data object None: GamePathOperation
    data object PathExists: GamePathOperation
    data class AddNewPath(val path: String): GamePathOperation
    data class RenamePath(val item: GamePath): GamePathOperation
    data class DeletePath(val item: GamePath): GamePathOperation
}

sealed interface VersionsOperation {
    data object None: VersionsOperation
    data class Rename(val version: Version): VersionsOperation
    data class Copy(val version: Version): VersionsOperation
    data class Delete(val version: Version, val text: String? = null): VersionsOperation
    data class InvalidDelete(val version: Version): VersionsOperation
    data class RunTask(val title: String, val task: suspend () -> Unit): VersionsOperation
}

@Composable
fun GamePathItemLayout(
    item: GamePath,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val notDefault = item.id != GamePathManager.DEFAULT_ID

    NavigationDrawerItem(
        modifier = modifier,
        label = {
            Column(
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = if (notDefault) item.title else "默认路径", // TODO: i18n
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = item.path,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        },
        badge = {
            var menuExpanded by remember { mutableStateOf(false) }

            Row {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { menuExpanded = !menuExpanded }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "更多", // TODO: i18n
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        enabled = notDefault,
                        text = { Text(text = "重命名") }, // TODO: i18n
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "重命名" // TODO: i18n
                            )
                        },
                        onClick = {
                            onRename()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        enabled = notDefault,
                        text = { Text(text = "删除") }, // TODO: i18n
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "删除" // TODO: i18n
                            )
                        },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        },
        selected = selected,
        onClick = onClick
    )
}

@Composable
fun VersionCategoryItem(
    modifier: Modifier = Modifier,
    value: VersionCategory,
    versionsCount: Int,
    selected: Boolean,
    shape: Shape = MaterialTheme.shapes.large,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Row {
                Text(
                    text = when(value) {
                        VersionCategory.ALL -> "全部" // TODO: i18n
                        VersionCategory.VANILLA -> "原版" // TODO: i18n
                        VersionCategory.MODLOADER -> "模组" // TODO: i18n
                    },
                    style = style
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($versionsCount)",
                    style = style
                )
            }
        }
    )
}

@Composable
fun VersionsOperation(
    versionsOperation: VersionsOperation,
    updateVersionsOperation: (VersionsOperation) -> Unit,
    submitError: (String) -> Unit
) {
    when(versionsOperation) {
        is VersionsOperation.None -> {}
        is VersionsOperation.Rename -> {
            RenameVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = {
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = "重命名版本", // TODO: i18n
                            task = {
                                VersionsManager.renameVersion(versionsOperation.version, it)
                            }
                        )
                    )
                }
            )
        }
        is VersionsOperation.Copy -> {
            CopyVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { name, copyAll ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = "复制版本", // TODO: i18n
                            task = { VersionsManager.copyVersion(versionsOperation.version, name, copyAll) }
                        )
                    )
                }
            )
        }
        is VersionsOperation.InvalidDelete -> {
            updateVersionsOperation(
                VersionsOperation.Delete(
                    versionsOperation.version,
                    "此版本无效，将被删除" // TODO: i18n
                )
            )
        }
        is VersionsOperation.Delete -> {
            val version = versionsOperation.version
            DeleteVersionDialog(
                version = version,
                message = versionsOperation.text,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { title, task ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = title,
                            task = task
                        )
                    )
                }
            )
        }
        is VersionsOperation.RunTask -> {
            val errorMessage = "任务执行失败" // TODO: i18n
            SimpleTaskDialog(
                title = versionsOperation.title,
                task = versionsOperation.task,
                context = kotlinx.coroutines.CoroutineScope(Dispatchers.IO),
                onDismiss = { updateVersionsOperation(VersionsOperation.None) },
                onError = { e ->
                    lError("Failed to run task.", e)
                    submitError("${errorMessage}: ${e.getMessageOrToString()}")
                }
            )
        }
    }
}

@Composable
fun RenameVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var name by remember { mutableStateOf(version.getVersionName()) }
    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || run {
        var hasError = false
        VersionsManager.validateVersionName(name, version.getVersionInfo()) { message ->
            errorMessage = message
            hasError = true
        }
        hasError
    }

    SimpleEditDialog(
        title = "重命名版本", // TODO: i18n
        value = name,
        onValueChange = { name = it },
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(text = "名称不能为空") // TODO: i18n
                isError -> Text(text = errorMessage)
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(name)
            }
        }
    )
}

@Composable
fun CopyVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String, copyAll: Boolean) -> Unit = { _, _ -> }
) {
    var copyAll by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || run {
        var hasError = false
        VersionsManager.validateVersionName(name, version.getVersionInfo()) { message ->
            errorMessage = message
            hasError = true
        }
        hasError
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("复制版本") }, // TODO: i18n
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("复制版本到新的版本") // TODO: i18n
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("版本名称") }, // TODO: i18n
                    isError = isError,
                    supportingText = {
                        when {
                            name.isEmpty() -> Text(text = "名称不能为空") // TODO: i18n
                            isError -> Text(text = errorMessage)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = copyAll,
                        onCheckedChange = { copyAll = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("复制所有文件") // TODO: i18n
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isError) {
                        onConfirm(name, copyAll)
                    }
                },
                enabled = !isError
            ) {
                Text("确定") // TODO: i18n
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消") // TODO: i18n
            }
        }
    )
}

@Composable
fun DeleteVersionDialog(
    version: Version,
    message: String? = null,
    onDismissRequest: () -> Unit = {},
    onConfirm: (title: String, task: suspend () -> Unit) -> Unit = { _, _ -> },
    onVersionDeleted: () -> Unit = {}
) {
    val deleteVersion = {
        onConfirm("删除版本") { // TODO: i18n
            VersionsManager.deleteVersion(version)
            onVersionDeleted()
        }
    }

    if (message != null) {
        SimpleAlertDialog(
            title = "删除版本", // TODO: i18n
            text = message,
            onDismiss = onDismissRequest,
            onConfirm = deleteVersion
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("删除版本", fontWeight = FontWeight.Bold) }, // TODO: i18n
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "确定要删除版本 ${version.getVersionName()} 吗？") // TODO: i18n
                    Text(text = "此操作将删除版本文件夹及其所有内容") // TODO: i18n
                    Text(text = "包括模组、存档、资源包等文件") // TODO: i18n
                    Text(
                        text = "此操作不可撤销！", // TODO: i18n
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = deleteVersion) {
                    Text("确定") // TODO: i18n
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("取消") // TODO: i18n
                }
            }
        )
    }
}

@Composable
fun VersionItemLayout(
    version: Version,
    selected: Boolean,
    submitError: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPinned: () -> Unit = {}
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null,
        onClick = {
            if (selected) return@Card
            onSelected()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = {
                    if (selected) return@RadioButton
                    onSelected()
                }
            )
            
            CommonVersionInfoLayout(
                modifier = Modifier.weight(1f),
                version = version
            )

            // Pin button
            IconButton(
                onClick = {
                    val currentValue = version.pinnedState
                    runCatching {
                        version.setPinnedAndSave(!currentValue)
                    }.onFailure { e ->
                        lError("Failed to save version config!", e)
                        submitError("保存版本配置失败: ${e.getMessageOrToString()}") // TODO: i18n
                    }.onSuccess {
                        onPinned()
                    }
                },
                enabled = version.isValid()
            ) {
                Crossfade(
                    targetState = version.pinnedState
                ) { pinned ->
                    Icon(
                        modifier = Modifier.rotate(45.0f),
                        imageVector = if (pinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        contentDescription = "置顶", // TODO: i18n
                        tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                enabled = version.isValid()
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置" // TODO: i18n
                )
            }

            // More menu
            Row {
                var menuExpanded by remember { mutableStateOf(false) }

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "更多" // TODO: i18n
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "重命名") }, // TODO: i18n
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "重命名" // TODO: i18n
                            )
                        },
                        onClick = {
                            onRenameClick()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = "复制") }, // TODO: i18n
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.FileCopy,
                                contentDescription = "复制" // TODO: i18n
                            )
                        },
                        onClick = {
                            onCopyClick()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = "删除") }, // TODO: i18n
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "删除" // TODO: i18n
                            )
                        },
                        onClick = {
                            onDeleteClick()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommonVersionInfoLayout(
    modifier: Modifier = Modifier,
    version: Version
) {
    Row(modifier = modifier) {
        VersionIconImage(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterVertically),
            version = version
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Version name
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                maxLines = 1,
                text = version.getVersionName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Version summary if available
            if (version.isValid() && version.isSummaryValid()) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    maxLines = 1,
                    text = version.getVersionSummary(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Version details
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!version.isValid()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "无效", // TODO: i18n
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    version.getVersionInfo()?.let { versionInfo ->
                        Text(
                            text = versionInfo.minecraftVersion,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        versionInfo.loaderInfo?.let { loaderInfo ->
                            Text(
                                text = loaderInfo.loader.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = loaderInfo.version,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VersionIconImage(
    version: Version?,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null
) {
    val model = remember(version, refreshKey) {
        version?.let {
            val iconFile = VersionsManager.getVersionIconFile(it)
            when {
                iconFile.exists() -> iconFile
                else -> getLoaderIconRes(it)
            }
        } ?: R.drawable.img_minecraft
    }

    when (model) {
        is Int -> {
            androidx.compose.foundation.Image(
                painter = painterResource(id = model),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = modifier
            )
        }
        else -> {
            coil.compose.AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}

private fun getLoaderIconRes(version: Version): Int {
    return when (version.getVersionInfo()?.loaderInfo?.loader?.displayName) {
        "Fabric" -> R.drawable.img_loader_fabric
        "Forge" -> R.drawable.img_anvil
        "Quilt" -> R.drawable.img_loader_quilt
        "NeoForge" -> R.drawable.img_loader_neoforge
        "OptiFine" -> R.drawable.img_loader_optifine
        "LiteLoader" -> R.drawable.img_minecraft // 使用默认图标替代
        else -> R.drawable.img_minecraft
    }
}