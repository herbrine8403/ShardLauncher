package com.lanrhyme.shardlauncher.ui.version

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.game.version.installed.Version
import com.lanrhyme.shardlauncher.game.version.mod.AllModReader
import com.lanrhyme.shardlauncher.game.version.mod.LocalMod
import com.lanrhyme.shardlauncher.game.version.mod.isEnabled
import com.lanrhyme.shardlauncher.ui.components.SearchTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import android.graphics.BitmapFactory

private class ModsViewModel(
    private val modsDir: File
) : ViewModel() {
    private val modReader = AllModReader(modsDir)

    var allMods by mutableStateOf<List<LocalMod>>(emptyList())
        private set
    var filteredMods by mutableStateOf<List<LocalMod>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var stateFilter by mutableStateOf(ModStateFilter.ALL)
        private set

    val selectedMods = mutableStateListOf<LocalMod>()

    private var job: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            isLoading = true
            selectedMods.clear()
            try {
                allMods = modReader.readAllMods()
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun updateStateFilter(filter: ModStateFilter) {
        stateFilter = filter
        applyFilters()
    }

    private fun applyFilters() {
        filteredMods = allMods
            .filter { mod ->
                // 搜索过滤
                val matchesSearch = searchQuery.isBlank() ||
                        mod.name.contains(searchQuery, ignoreCase = true) ||
                        mod.description?.contains(searchQuery, ignoreCase = true) == true

                // 状态过滤
                val matchesState = when (stateFilter) {
                    ModStateFilter.ALL -> true
                    ModStateFilter.ENABLED -> mod.file.isEnabled()
                    ModStateFilter.DISABLED -> !mod.file.isEnabled()
                }

                matchesSearch && matchesState
            }
    }

    fun toggleMod(mod: LocalMod) {
        viewModelScope.launch(Dispatchers.IO) {
            if (mod.file.isEnabled()) {
                mod.disable()
            } else {
                mod.enable()
            }
        }
    }

    fun deleteMod(mod: LocalMod, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.deleteQuietly(mod.file)
            withContext(Dispatchers.Main) {
                onComplete()
                refresh()
            }
        }
    }

    fun deleteSelectedMods(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedMods.forEach { mod ->
                FileUtils.deleteQuietly(mod.file)
            }
            withContext(Dispatchers.Main) {
                selectedMods.clear()
                onComplete()
                refresh()
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

enum class ModStateFilter {
    ALL, ENABLED, DISABLED
}

@Composable
fun ModsManagementScreen(
    version: Version,
    onBack: () -> Unit
) {
    val modsDir = File(version.getGameDir(), "mods")
    val viewModel: ModsViewModel = viewModel(
        key = version.getVersionName() + "_mods"
    ) {
        ModsViewModel(modsDir)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var modToDelete by remember { mutableStateOf<LocalMod?>(null) }

    if (showDeleteDialog && modToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") }, // TODO: i18n
            text = { Text("确定要删除模组 \"${modToDelete?.name}\" 吗？") }, // TODO: i18n
            confirmButton = {
                TextButton(onClick = {
                    modToDelete?.let { mod ->
                        viewModel.deleteMod(mod) {
                            showDeleteDialog = false
                            modToDelete = null
                        }
                    }
                }) {
                    Text("删除") // TODO: i18n
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消") // TODO: i18n
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with search and actions
        ModsHeader(
            searchQuery = viewModel.searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            stateFilter = viewModel.stateFilter,
            onStateFilterChange = { viewModel.updateStateFilter(it) },
            onRefresh = { viewModel.refresh() },
            selectedCount = viewModel.selectedMods.size,
            onDeleteSelected = {
                if (viewModel.selectedMods.isNotEmpty()) {
                    // TODO: Show delete confirmation dialog
                    viewModel.deleteSelectedMods {}
                }
            },
            onClearSelection = { viewModel.selectedMods.clear() }
        )

        // Mods list
        when {
            viewModel.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.filteredMods.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无模组") // TODO: i18n
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.filteredMods, key = { it.file.absolutePath }) { mod ->
                        ModCard(
                            mod = mod,
                            isSelected = viewModel.selectedMods.contains(mod),
                            onToggleSelection = {
                                if (viewModel.selectedMods.contains(mod)) {
                                    viewModel.selectedMods.remove(mod)
                                } else {
                                    viewModel.selectedMods.add(mod)
                                }
                            },
                            onToggleEnabled = { viewModel.toggleMod(mod) },
                            onDelete = {
                                modToDelete = mod
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModsHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    stateFilter: ModStateFilter,
    onStateFilterChange: (ModStateFilter) -> Unit,
    onRefresh: () -> Unit,
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                hint = "搜索模组", // TODO: i18n
                modifier = Modifier.weight(1f)
            )

            // Filter dropdown
            var filterExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { filterExpanded = true }) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter") // TODO: i18n
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false }
                ) {
                    ModStateFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (filter) {
                                        ModStateFilter.ALL -> "全部" // TODO: i18n
                                        ModStateFilter.ENABLED -> "已启用" // TODO: i18n
                                        ModStateFilter.DISABLED -> "已禁用" // TODO: i18n
                                    }
                                )
                            },
                            onClick = {
                                onStateFilterChange(filter)
                                filterExpanded = false
                            },
                            trailingIcon = if (filter == stateFilter) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh") // TODO: i18n
            }

            if (selectedCount > 0) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected") // TODO: i18n
                }
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear selection") // TODO: i18n
                }
            }
        }

        if (selectedCount > 0) {
            Text(
                text = "已选择 $selectedCount 个模组", // TODO: i18n
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ModCard(
    mod: LocalMod,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Spacer(Modifier.width(8.dp))

            // Mod icon
            mod.icon?.let { iconBytes ->
                val bitmap = remember(iconBytes) {
                    BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.size)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } ?: Icon(
                painter = painterResource(R.drawable.img_minecraft),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Mod info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mod.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                mod.version?.let {
                    Text(
                        text = "版本: $it", // TODO: i18n
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mod.authors.isNotEmpty()) {
                    Text(
                        text = "作者: ${mod.authors.joinToString(", ")}", // TODO: i18n
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Enable/Disable switch
            Switch(
                checked = mod.file.isEnabled(),
                onCheckedChange = { onToggleEnabled() }
            )

            Spacer(Modifier.width(8.dp))

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete") // TODO: i18n
            }
        }
    }
}
