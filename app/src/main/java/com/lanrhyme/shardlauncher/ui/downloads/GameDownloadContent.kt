package com.lanrhyme.shardlauncher.ui.downloads

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lanrhyme.shardlauncher.R
import com.lanrhyme.shardlauncher.model.BmclapiManifest
import com.lanrhyme.shardlauncher.ui.components.CombinedCard
import com.lanrhyme.shardlauncher.ui.components.LocalCardLayoutConfig
import com.lanrhyme.shardlauncher.ui.components.SearchTextField
import com.lanrhyme.shardlauncher.ui.components.StyledFilterChip
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GameDownloadContent(navController: NavController) {
    val cardLayoutConfig = LocalCardLayoutConfig.current
    val isCardBlurEnabled = cardLayoutConfig.isCardBlurEnabled
    val cardAlpha = cardLayoutConfig.cardAlpha
    val hazeState = cardLayoutConfig.hazeState
    val viewModel: GameDownloadViewModel = viewModel()

    val versions by viewModel.filteredVersions.collectAsState()
    val selectedVersionTypes by viewModel.selectedVersionTypes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadVersions() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading && versions.isEmpty()) {
            CircularProgressIndicator()
        } else if (errorMessage != null && versions.isEmpty()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "加载失败",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                androidx.compose.material3.Button(
                    onClick = { viewModel.loadVersions(forceRefresh = true) }
                ) {
                    Text("重试")
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { ->
                    CombinedCard(title = "版本筛选", summary = null) {
                        Row(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VersionType.entries.forEach { versionType ->
                                StyledFilterChip(
                                        selected = versionType in selectedVersionTypes,
                                        onClick = { viewModel.toggleVersionType(versionType) },
                                        label = { Text(versionType.title) },
                                        modifier = Modifier.fillMaxHeight()
                                )
                            }
                            SearchTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    hint = "搜索",
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            IconButton(
                                    onClick = {
                                        viewModel.loadVersions(forceRefresh = true)
                                    },
                                    modifier = Modifier.fillMaxHeight()
                            ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                        }
                    }
                }

                items(versions) { version ->
                    VersionItem(version = version) {
                        navController.navigate("version_detail/${version.id}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.VersionItem(version: BmclapiManifest.Version, onClick: () -> Unit) {
    val cardLayoutConfig = LocalCardLayoutConfig.current
    val isCardBlurEnabled = cardLayoutConfig.isCardBlurEnabled
    val cardAlpha = cardLayoutConfig.cardAlpha
    val hazeState = cardLayoutConfig.hazeState
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val scale by
            animateFloatAsState(
                    targetValue = if (appeared) 1f else 0.9f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "scale"
            )

    // Determine version type and icon
    val (versionType, versionTypeTitle, iconRes) = when (version.type) {
        "release" -> Triple(VersionType.Release, VersionType.Release.title, R.drawable.img_minecraft)
        "snapshot" -> Triple(VersionType.Snapshot, VersionType.Snapshot.title, R.drawable.img_command_block)
        "old_alpha" -> Triple(VersionType.Ancient, "远古Alpha", R.drawable.img_old_grass_block)
        "old_beta" -> Triple(VersionType.Ancient, "远古Beta", R.drawable.img_old_cobblestone)
        else -> {
            // Check if it's April Fools version
            val isAprilFools = version.id.startsWith("2.0") ||
                               version.id.startsWith("15w14a") ||
                               version.id.startsWith("1.RV-Pre1")
            if (isAprilFools) {
                Triple(VersionType.AprilFools, VersionType.AprilFools.title, R.drawable.img_diamond_block)
            } else {
                Triple(VersionType.Release, version.type, R.drawable.img_minecraft)
            }
        }
    }

    val shape = RoundedCornerShape(22.dp)
    val cardModifier =
            if (isCardBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.fillMaxWidth()
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                        .clip(shape)
                        .hazeEffect(state = hazeState)
                        .clickable { onClick() }
            } else {
                Modifier.fillMaxWidth()
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                        .clickable { onClick() }
            }

    Card(
            modifier = cardModifier,
            shape = shape,
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)
                    ),
    ) {
        Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Minecraft",
                    modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Minecraft ${version.id}", style = MaterialTheme.typography.bodyLarge)
                Text(
                        text = "$versionTypeTitle - ${version.releaseTime.substringBefore('T')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
