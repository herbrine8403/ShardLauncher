package com.lanrhyme.shardlauncher.ui.components.color

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 将 Compose Color 转换为 HSV 值
 * @return Triple(hue, saturation, value)
 * hue: 0..360
 * saturation: 0..1
 * value: 0..1
 */
fun Color.toHsv(): Triple<Float, Float, Float> {
    val hsv = floatArrayOf(0f, 0f, 0f)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/**
 * 从 HSV 值创建 Compose Color
 */
fun Color.Companion.hsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val hsv = floatArrayOf(hue, saturation, value)
    val colorInt = AndroidColor.HSVToColor((alpha * 255).toInt(), hsv)
    return Color(colorInt)
}

/**
 * 将十六进制字符串转换为 Color，如果格式无效则返回 null
 * 支持格式: #RRGGBB, #AARRGGBB
 */
fun String.toColorOrNull(): Color? {
    if (this.isBlank()) return null
    return try {
        val colorString = if (this.startsWith("#")) this else "#$this"
        Color(AndroidColor.parseColor(colorString))
    } catch (e: IllegalArgumentException) {
        null
    }
}
