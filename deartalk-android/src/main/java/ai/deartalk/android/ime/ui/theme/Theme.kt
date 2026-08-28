package ai.deartalk.android.ime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DearTalkBackground = Color(0xFF0F172A)
val DearTalkSurface = Color(0xFF1E293B)
val DearTalkKey = Color(0xFF334155)
val DearTalkKeyActive = Color(0xFF475569)
val DearTalkPrimary = Color(0xFF6366F1)
val DearTalkSecondary = Color(0xFF38BDF8)
val DearTalkText = Color(0xFFF8FAFC)
val DearTalkTextDim = Color(0xFF94A3B8)
val DearTalkAccentGlow = Color(0xFF818CF8)

private val DarkColorScheme = darkColorScheme(
    primary = DearTalkPrimary,
    secondary = DearTalkSecondary,
    background = DearTalkBackground,
    surface = DearTalkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = DearTalkText,
    onSurface = DearTalkText
)

@Composable
fun DearTalkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
