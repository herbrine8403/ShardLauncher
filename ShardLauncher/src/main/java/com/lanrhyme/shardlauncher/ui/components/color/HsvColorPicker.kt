package com.lanrhyme.shardlauncher.ui.components.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/**
 * HSV 颜色选择器组件
 * 支持色相、饱和度、亮度调节以及 Hex 颜色输入
 *
 * @param modifier 应用于组件的修饰符
 * @param color 当前选中的颜色
 * @param onColorSelected 颜色选择变更时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HsvColorPicker(
    modifier: Modifier = Modifier,
    color: Color,
    onColorSelected: (Color) -> Unit
) {
    val initialColor = remember { color } // Remember the initial color

    val (h, saturation, value) = color.toHsv()
    // Remember the hue state. Initialize with the hue from the initial color.
    var hue by remember { mutableStateOf(h) }

    var hexInput by remember { mutableStateOf("") }

    LaunchedEffect(color) {
        hexInput = String.format("#%06X", 0xFFFFFF and color.toArgb())
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HueSlider(
                hue = hue,
                onHueChanged = { newHue ->
                    hue = newHue
                    onColorSelected(Color.hsv(newHue, saturation, value))
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            SaturationValuePanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onSatValChanged = { newSat, newVal ->
                    onColorSelected(Color.hsv(hue, newSat, newVal))
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Color comparison view
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Text("修改前", style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(initialColor, RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("修改后", style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, RoundedCornerShape(16.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = hexInput,
            onValueChange = { newHex ->
                hexInput = newHex
                newHex.toColorOrNull()?.let { newColor ->
                    onColorSelected(newColor)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = hexInput,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = { Text("Hex Color") },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), // Reduced padding
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = OutlinedTextFieldDefaults.colors(),
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                )
            }
        )
    }
}

/**
 * 饱和度和亮度选择面板
 *
 * @param hue 当前色相
 * @param saturation 当前饱和度
 * @param value 当前亮度
 * @param onSatValChanged 饱和度和亮度变更时的回调
 * @param modifier 应用于组件的修饰符
 */
@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, _ ->
                val x = change.position.x.coerceIn(0f, size.width.toFloat())
                val y = change.position.y.coerceIn(0f, size.height.toFloat())
                onSatValChanged(x / size.width, 1f - y / size.height)
            },
            onDragStart = { offset ->
                val x = offset.x.coerceIn(0f, size.width.toFloat())
                val y = offset.y.coerceIn(0f, size.height.toFloat())
                onSatValChanged(x / size.width, 1f - y / size.height)
            }
        )
    }.pointerInput(Unit) {
        detectTapGestures { offset ->
            val x = offset.x.coerceIn(0f, size.width.toFloat())
            val y = offset.y.coerceIn(0f, size.height.toFloat())
            onSatValChanged(x / size.width, 1f - y / size.height)
        }
    }) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val satValBrush = Brush.verticalGradient(listOf(Color.White, Color.Black))
            val hueBrush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f)))

            drawRect(brush = satValBrush)
            drawRect(brush = hueBrush, blendMode = BlendMode.Multiply)

            val selectorX = size.width * saturation
            val selectorY = size.height * (1f - value)

            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 6.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * 色相选择滑块
 *
 * @param hue 当前色相
 * @param onHueChanged 色相变更时的回调
 * @param modifier 应用于组件的修饰符
 */
@Composable
private fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )

    Box(modifier = modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, _ ->
                val y = change.position.y.coerceIn(0f, size.height.toFloat())
                onHueChanged(360f * y / size.height)
            },
            onDragStart = { offset ->
                val y = offset.y.coerceIn(0f, size.height.toFloat())
                onHueChanged(360f * y / size.height)
            }
        )
    }.pointerInput(Unit) {
        detectTapGestures { offset ->
            val y = offset.y.coerceIn(0f, size.height.toFloat())
            onHueChanged(360f * y / size.height)
        }
    }) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val brush = Brush.verticalGradient(hueColors)
            drawRect(brush = brush)

            val selectorY = size.height * hue / 360f

            drawLine(
                color = Color.White,
                start = Offset(0f, selectorY),
                end = Offset(size.width, selectorY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
