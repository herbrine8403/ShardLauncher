package com.lanrhyme.shardlauncher.ui.components

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

data class CardLayoutConfig(
        val isCardBlurEnabled: Boolean = false,
        val cardAlpha: Float = 1f,
        val hazeState: HazeState = HazeState()
)

val LocalCardLayoutConfig = compositionLocalOf { CardLayoutConfig() }
