package com.suyash.lumen.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF005AC1),
    secondary = Color(0xFF545F70),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    secondary = Color(0xFFBCC7DC),
)

/**
 * App-wide theme wrapper. Kept intentionally small for now — expand
 * with typography scale and spacing tokens when a second screen ships.
 */
@Composable
fun LumenTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
