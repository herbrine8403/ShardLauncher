/*
 * Shard Launcher
 * Adapted from Zalith Launcher 2
 */

package com.lanrhyme.shardlauncher.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.game.multirt.Runtime
import com.lanrhyme.shardlauncher.game.multirt.RuntimesManager
import com.lanrhyme.shardlauncher.game.multirt.RuntimeInstaller
import com.lanrhyme.shardlauncher.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeManageScreen(
    animationSpeed: Float = 1.0f,
    isGlowEffectEnabled: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var runtimes by remember { mutableStateOf(RuntimesManager.getRuntimes()) }
    var bundledRuntimes by remember { mutableStateOf(RuntimeInstaller.getBundledRuntimes(context)) }
    var showDeleteDialog by remember { mutableStateOf<Runtime?>(null) }
    var showBundledInstallDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0) }
    var importRuntimeName by remember { mutableStateOf("") }

    // File picker for importing tar.xz files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (importRuntimeName.isNotBlank()) {
                showProgressDialog = true
                progressMessage = "准备导入..."
                progressValue = 0
                
                scope.launch {
                    RuntimeInstaller.importRuntimeFromFile(
                        context = context,
                        uri = selectedUri,
                        runtimeName = importRuntimeName,
                        onProgress = { progress, message ->
                            progressValue = progress
                            progressMessage = message
                        }
                    ).onSuccess {
                        showProgressDialog = false
                        showImportDialog = false
                        importRuntimeName = ""
                        runtimes = RuntimesManager.getRuntimes()
                    }.onFailure { error ->
                        showProgressDialog = false
                        // TODO: Show error dialog
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "运行库管理",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "管理 Java 运行时环境",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            runtimes = RuntimesManager.getRuntimes()
                            bundledRuntimes = RuntimeInstaller.getBundledRuntimes(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("刷新")
                    }
                    
                    Button(
                        onClick = { showBundledInstallDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.GetApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("内置")
                    }
                    
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导入")
                    }
                }
            }
        }

        // Runtime list
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (runtimes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "未找到 Java 运行时",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "请安装内置运行时或导入 tar.xz 文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(runtimes) { runtime ->
                        RuntimeItem(
                            runtime = runtime,
                            onDeleteClick = { showDeleteDialog = runtime }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { runtime ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除运行时") },
            text = { Text("确定要删除运行时 \"${runtime.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        RuntimesManager.removeRuntime(runtime.name)
                        showDeleteDialog = null
                        runtimes = RuntimesManager.getRuntimes()
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Bundled runtime install dialog
    if (showBundledInstallDialog) {
        BundledRuntimeInstallDialog(
            bundledRuntimes = bundledRuntimes,
            installedRuntimes = runtimes,
            onDismiss = { showBundledInstallDialog = false },
            onInstall = { bundledRuntime ->
                showBundledInstallDialog = false
                showProgressDialog = true
                progressMessage = "准备安装..."
                progressValue = 0
                
                scope.launch {
                    RuntimeInstaller.installBundledRuntime(
                        context = context,
                        runtime = bundledRuntime,
                        onProgress = { progress, message ->
                            progressValue = progress
                            progressMessage = message
                        }
                    ).onSuccess {
                        showProgressDialog = false
                        runtimes = RuntimesManager.getRuntimes()
                    }.onFailure { error ->
                        showProgressDialog = false
                        // TODO: Show error dialog
                    }
                }
            }
        )
    }

    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入运行时") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("请输入运行时名称，然后选择 tar.xz 文件进行导入。")
                    
                    OutlinedTextField(
                        value = importRuntimeName,
                        onValueChange = { importRuntimeName = it },
                        label = { Text("运行时名称") },
                        placeholder = { Text("例如: openjdk-8-custom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importRuntimeName.isNotBlank()) {
                            filePickerLauncher.launch("application/*")
                        }
                    },
                    enabled = importRuntimeName.isNotBlank()
                ) {
                    Text("选择文件")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportDialog = false
                    importRuntimeName = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    // Progress dialog
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("安装运行时") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(progressMessage)
                    LinearProgressIndicator(
                        progress = progressValue / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${progressValue}%")
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
private fun RuntimeItem(
    runtime: Runtime,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = runtime.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Java ${runtime.javaVersion} (${runtime.arch ?: "unknown"})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                if (runtime.versionString != null) {
                    Text(
                        text = runtime.versionString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BundledRuntimeInstallDialog(
    bundledRuntimes: List<RuntimeInstaller.BundledRuntime>,
    installedRuntimes: List<Runtime>,
    onDismiss: () -> Unit,
    onInstall: (RuntimeInstaller.BundledRuntime) -> Unit
) {
    val installedNames = installedRuntimes.map { it.name }.toSet()
    val availableRuntimes = bundledRuntimes.filter { it.name !in installedNames }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("安装内置运行时") },
        text = {
            if (availableRuntimes.isEmpty()) {
                Text("所有兼容的内置运行时都已安装。")
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableRuntimes) { runtime ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onInstall(runtime) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = runtime.displayName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = runtime.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "预计大小: ${formatSize(runtime.estimatedSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        else -> "${bytes / (1024 * 1024 * 1024)}GB"
    }
}