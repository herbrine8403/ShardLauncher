package com.lanrhyme.shardlauncher.ui.version

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.installed.VersionConfig
import com.lanrhyme.shardlauncher.ui.components.*

@Composable
fun VersionConfigScreen(
    version: Version,
    config: VersionConfig,
    onConfigChange: (VersionConfig) -> Unit,
    onSave: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 版本设置标题
        Text(
            text = "版本设置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // 版本隔离
        SwitchLayout(
            checked = config.isolationType == com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE,
            onCheckedChange = {
                config.isolationType = if (config.isolationType == com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE) {
                    com.lanrhyme.shardlauncher.game.version.installed.SettingState.DISABLE
                } else {
                    com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE
                }
                onConfigChange(config)
            },
            title = "版本隔离",
            summary = "为此版本创建独立的游戏目录"
        )

        // 跳过游戏完整性检查
        SwitchLayout(
            checked = config.skipGameIntegrityCheck == com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE,
            onCheckedChange = {
                config.skipGameIntegrityCheck = if (config.skipGameIntegrityCheck == com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE) {
                    com.lanrhyme.shardlauncher.game.version.installed.SettingState.DISABLE
                } else {
                    com.lanrhyme.shardlauncher.game.version.installed.SettingState.ENABLE
                }
                onConfigChange(config)
            },
            title = "跳过完整性检查",
            summary = "跳过游戏文件完整性检查"
        )

        // 游戏设置标题
        Text(
            text = "游戏设置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        )

        // 内存分配
        SliderLayout(
            value = config.ramAllocation.toFloat(),
            onValueChange = { value ->
                config.ramAllocation = value.toInt()
                onConfigChange(config)
            },
            title = "内存分配",
            summary = "为游戏分配的内存大小",
            valueRange = 512f..8192f,
            displayValue = config.ramAllocation.toFloat(),
            isGlowEffectEnabled = true
        )

        // JVM参数
        TextInputLayout(
            value = config.jvmArgs,
            onValueChange = { value ->
                config.jvmArgs = value
                onConfigChange(config)
            },
            title = "JVM参数",
            summary = "自定义Java虚拟机启动参数",
            placeholder = "例如: -XX:+UseG1GC"
        )

        // 高级设置标题
        Text(
            text = "高级设置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        )

        // 服务器IP
        TextInputLayout(
            value = config.serverIp,
            onValueChange = { value ->
                config.serverIp = value
                onConfigChange(config)
            },
            title = "默认服务器",
            summary = "启动游戏时自动连接的服务器",
            placeholder = "例如: mc.hypixel.net"
        )

        // 保存按钮
        ButtonLayout(
            onClick = { showSaveDialog = true },
            title = "保存配置",
            summary = "保存当前版本的所有配置更改",
            buttonText = "保存"
        )
    }

    if (showSaveDialog) {
        SimpleAlertDialog(
            title = "保存配置",
            text = "确定要保存版本配置吗？",
            onDismiss = { showSaveDialog = false },
            onConfirm = {
                try {
                    onSave()
                    showSaveDialog = false
                } catch (e: Exception) {
                    onError("保存配置失败: ${e.message}")
                    showSaveDialog = false
                }
            }
        )
    }
}

