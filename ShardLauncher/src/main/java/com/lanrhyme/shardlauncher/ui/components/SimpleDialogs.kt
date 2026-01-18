package com.lanrhyme.shardlauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDismissRequest: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            if (onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text("确定") // TODO: i18n
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel ?: onDismiss) {
                Text("取消") // TODO: i18n
            }
        }
    )
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDismissRequest: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = text,
        confirmButton = {
            if (onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text("确定") // TODO: i18n
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel ?: onDismiss) {
                Text("取消") // TODO: i18n
            }
        }
    )
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                isError = isError,
                supportingText = supportingText,
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
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
fun SimpleTaskDialog(
    title: String,
    task: suspend () -> Unit,
    context: CoroutineScope,
    onDismiss: () -> Unit,
    onError: (Throwable) -> Unit
) {
    var isRunning by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            task()
            onDismiss()
        } catch (e: Throwable) {
            isRunning = false
            onError(e)
        }
    }
    
    if (isRunning) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Row {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("正在执行...") // TODO: i18n
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1
    )
}