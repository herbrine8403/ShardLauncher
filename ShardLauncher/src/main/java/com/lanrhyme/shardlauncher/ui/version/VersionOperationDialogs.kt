package com.lanrhyme.shardlauncher.ui.version

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.VersionsManager
import com.lanrhyme.shardlauncher.ui.components.ShardAlertDialog
import com.lanrhyme.shardlauncher.ui.components.ShardEditDialog
import com.lanrhyme.shardlauncher.ui.components.ShardInputField

@Composable
fun RenameVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(version.getVersionName()) }
    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || run {
        val validationError = validateVersionName(name, version.getVersionInfo())
        if (validationError != null) {
            errorMessage = validationError
            true
        } else {
            false
        }
    }

    ShardEditDialog(
        title = "重命名版本",
        value = name,
        onValueChange = { name = it },
        label = "版本名称",
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(text = "版本名称不能为空")
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
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var copyAll by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || run {
        val validationError = validateVersionName(name, version.getVersionInfo())
        if (validationError != null) {
            errorMessage = validationError
            true
        } else {
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("复制版本") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("创建版本的副本")
                
                ShardInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = "新版本名称",
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (name.isEmpty()) {
                    Text(
                        text = "版本名称不能为空",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isError) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = copyAll,
                        onCheckedChange = { copyAll = it }
                    )
                    Text(
                        text = "复制所有文件（包括存档、模组等）",
                        modifier = Modifier.weight(1f)
                    )
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
                Text("复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DeleteVersionDialog(
    version: Version,
    message: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    if (message != null) {
        ShardAlertDialog(
            title = "删除版本",
            text = {Text(message)},
            onDismiss = onDismissRequest,
            onConfirm = onConfirm
        )
    } else {
        ShardAlertDialog(
            title = "删除版本",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "确定要删除版本 ${version.getVersionName()} 吗？")
                    Text(text = "此操作将删除版本文件夹及其所有内容")
                    Text(text = "包括存档、模组、资源包等所有数据")
                    Text(
                        text = "此操作不可撤销！",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onDismiss = onDismissRequest,
            onConfirm = onConfirm,
            onCancel = onDismissRequest
        )
    }
}

private fun validateVersionName(name: String, versionInfo: com.lanrhyme.shardlauncher.game.version.installed.VersionInfo?): String? {
    return when {
        name.isEmpty() -> "版本名称不能为空"
        name.contains(Regex("[<>:\"/\\\\|?*]")) -> "版本名称包含非法字符"
        VersionsManager.isVersionExists(name, true) -> "版本已存在"
        versionInfo?.loaderInfo?.let {
            // 如果版本有ModLoader信息，不允许使用原版名称
            name == versionInfo.minecraftVersion
        } ?: false -> "不能使用Minecraft原版名称"
        else -> null
    }
}
