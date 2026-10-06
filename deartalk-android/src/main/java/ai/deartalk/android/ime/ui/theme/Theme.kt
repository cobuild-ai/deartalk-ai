package ai.deartalk.android.ime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class DearTalkColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val key: Color,
    val keyActive: Color,
    val keyShadow: Color,
    val keyBorder: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textDim: Color,
    val accentGlow: Color,
    val border: Color,
    val isDark: Boolean
)

// 🌙 Midnight Obsidian (다크 / 나이트 모드 프리미엄 테마)
val DarkDearTalkColors = DearTalkColors(
    background = Color(0xFF0B0F19),
    surface = Color(0xFF131B2E),
    surfaceElevated = Color(0xFF1E293B),
    key = Color(0xFF1E293B),
    keyActive = Color(0xFF334155),
    keyShadow = Color(0x99000000),
    keyBorder = Color(0xFF26334A),
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF38BDF8),
    textPrimary = Color(0xFFF8FAFC),
    textDim = Color(0xFF94A3B8),
    accentGlow = Color(0xFF818CF8),
    border = Color(0xFF26334A),
    isDark = true
)

// ☀️ Slate Frost (데이 / 라이트 모드 클린 테마)
val LightDearTalkColors = DearTalkColors(
    background = Color(0xFFECEFF3),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF8FAFC),
    key = Color(0xFFFFFFFF),
    keyActive = Color(0xFFE2E8F0),
    keyShadow = Color(0x2B000000),
    keyBorder = Color(0xFFD6DBE4),
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0284C7),
    textPrimary = Color(0xFF0F172A),
    textDim = Color(0xFF64748B),
    accentGlow = Color(0xFF6366F1),
    border = Color(0xFFE2E8F0),
    isDark = false
)

val LocalDearTalkColors = staticCompositionLocalOf { DarkDearTalkColors }

// 하위 호환을 위한 동적 프로퍼티 (@Composable get())
val DearTalkBackground: Color @Composable get() = LocalDearTalkColors.current.background
val DearTalkSurface: Color @Composable get() = LocalDearTalkColors.current.surface
val DearTalkKey: Color @Composable get() = LocalDearTalkColors.current.key
val DearTalkKeyActive: Color @Composable get() = LocalDearTalkColors.current.keyActive
val DearTalkPrimary: Color @Composable get() = LocalDearTalkColors.current.primary
val DearTalkSecondary: Color @Composable get() = LocalDearTalkColors.current.secondary
val DearTalkText: Color @Composable get() = LocalDearTalkColors.current.textPrimary
val DearTalkTextDim: Color @Composable get() = LocalDearTalkColors.current.textDim
val DearTalkAccentGlow: Color @Composable get() = LocalDearTalkColors.current.accentGlow
val DearTalkBorder: Color @Composable get() = LocalDearTalkColors.current.border

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF131B2E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0284C7),
    background = Color(0xFFECEFF3),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun DearTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkDearTalkColors else LightDearTalkColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDearTalkColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
