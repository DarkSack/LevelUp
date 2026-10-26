package app.levelup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.levelup.content.Categories
import app.levelup.domain.MotionPref
import app.levelup.domain.Preferences
import app.levelup.domain.DEFAULT_ACCENT
import app.levelup.domain.GamificationStyle
import app.levelup.domain.ThemePref

/**
 * Identidad de LevelUp: verde azulado de crecimiento, ámbar para la XP y violeta para los logros.
 * Todos los pares texto/fondo cumplen contraste AA (4.5:1); el modo de alto contraste sube a AAA.
 */
object Brand {
    val Teal = Color(0xFF0F766E)
    val TealLight = Color(0xFF5EEAD4)
    val Amber = Color(0xFFB45309)
    val AmberLight = Color(0xFFFBBF24)
    val Violet = Color(0xFF6D28D9)
    val VioletLight = Color(0xFFC4B5FD)
}

private val Light = lightColorScheme(
    primary = Brand.Teal, onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Brand.Amber, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7), onSecondaryContainer = Color(0xFF451A03),
    tertiary = Brand.Violet, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE), onTertiaryContainer = Color(0xFF2E1065),
    background = Color(0xFFF7F7F4), onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFEDEDE8), onSurfaceVariant = Color(0xFF4E4B45),
    surfaceContainer = Color(0xFFF1F1EC), surfaceContainerHigh = Color(0xFFEAEAE4),
    outline = Color(0xFF79756D), outlineVariant = Color(0xFFD0CEC6),
    error = Color(0xFFB3261E), onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Brand.TealLight, onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF115E59), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Brand.AmberLight, onSecondary = Color(0xFF3F2300),
    secondaryContainer = Color(0xFF78350F), onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Brand.VioletLight, onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95), onTertiaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF121413), onBackground = Color(0xFFE8E9E6),
    surface = Color(0xFF1A1D1C), onSurface = Color(0xFFE8E9E6),
    surfaceVariant = Color(0xFF2A2E2C), onSurfaceVariant = Color(0xFFC3C7C3),
    surfaceContainer = Color(0xFF202322), surfaceContainerHigh = Color(0xFF2A2D2C),
    outline = Color(0xFF8D918D), outlineVariant = Color(0xFF424744),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

private fun ColorScheme.highContrast(dark: Boolean): ColorScheme = if (dark) {
    copy(
        primary = Color(0xFF99F6E4), onSurfaceVariant = Color(0xFFE6E8E6), outline = Color(0xFFD0D4D0),
        background = Color.Black, surface = Color(0xFF0B0C0C), onBackground = Color.White, onSurface = Color.White,
    )
} else {
    copy(
        primary = Color(0xFF0B4F4A), onSurfaceVariant = Color(0xFF26241F), outline = Color(0xFF3D3A35),
        background = Color.White, onBackground = Color.Black, onSurface = Color.Black,
    )
}

/** Baja estimulación: un solo color de acento y fondos neutros, para que nada compita por la atención. */
private fun ColorScheme.muted(): ColorScheme = copy(
    secondary = primary, onSecondary = onPrimary, tertiary = primary, onTertiary = onPrimary,
    primaryContainer = surfaceVariant, onPrimaryContainer = onSurface,
    secondaryContainer = surfaceVariant, onSecondaryContainer = onSurface,
    tertiaryContainer = surfaceVariant, onTertiaryContainer = onSurface,
)

/** Hay XP, niveles y logros a la vista (todo menos el estilo «sin juego»). */
val Preferences.gameOn: Boolean get() = gamification != GamificationStyle.OFF

/** Color principal elegible (LevelUp Plus). Claro y oscuro con contraste AA frente a su texto. */
data class Accent(
    val key: String,
    val name: String,
    val light: Long, val lightContainer: Long, val onLightContainer: Long,
    val dark: Long, val onDark: Long, val darkContainer: Long,
)

val Accents = listOf(
    Accent("teal", "Verde azulado", 0xFF0F766E, 0xFFCCFBF1, 0xFF042F2E, 0xFF5EEAD4, 0xFF003733, 0xFF115E59),
    Accent("azul", "Azul", 0xFF1D4ED8, 0xFFDBEAFE, 0xFF172554, 0xFF93C5FD, 0xFF0C1E4A, 0xFF1E3A8A),
    Accent("violeta", "Violeta", 0xFF6D28D9, 0xFFEDE9FE, 0xFF2E1065, 0xFFC4B5FD, 0xFF2E1065, 0xFF4C1D95),
    Accent("rosa", "Rosa", 0xFFBE185D, 0xFFFCE7F3, 0xFF500724, 0xFFF9A8D4, 0xFF4A0420, 0xFF831843),
    Accent("ambar", "Ámbar", 0xFFB45309, 0xFFFEF3C7, 0xFF451A03, 0xFFFCD34D, 0xFF3F2300, 0xFF78350F),
    Accent("verde", "Verde", 0xFF15803D, 0xFFDCFCE7, 0xFF052E16, 0xFF86EFAC, 0xFF052E16, 0xFF14532D),
)

private fun ColorScheme.withAccent(a: Accent, dark: Boolean): ColorScheme = if (dark) {
    copy(primary = Color(a.dark), onPrimary = Color(a.onDark), primaryContainer = Color(a.darkContainer), onPrimaryContainer = Color(a.lightContainer))
} else {
    copy(primary = Color(a.light), onPrimary = Color.White, primaryContainer = Color(a.lightContainer), onPrimaryContainer = Color(a.onLightContainer))
}

/** Color de cada categoría. Siempre va acompañado del nombre: el color nunca es la única pista. */
fun categoryColor(key: String, dark: Boolean): Color {
    val light = when (key) {
        Categories.FISICO -> 0xFFC2410C
        Categories.SOCIAL -> 0xFFBE185D
        Categories.MENTAL -> 0xFF4338CA
        Categories.PRODUCTIVIDAD -> 0xFF0369A1
        Categories.BIENESTAR -> 0xFF0F766E
        Categories.CREATIVIDAD -> 0xFF7E22CE
        Categories.FINANZAS -> 0xFF15803D
        Categories.DIGITAL -> 0xFF475569
        Categories.HOGAR -> 0xFFA16207
        else -> 0xFF57534E
    }
    val color = Color(light)
    return if (dark) lerpToWhite(color) else color
}

private fun lerpToWhite(c: Color): Color = Color(
    red = c.red + (1 - c.red) * 0.45f, green = c.green + (1 - c.green) * 0.45f, blue = c.blue + (1 - c.blue) * 0.45f,
)

/** Espaciado en múltiplos de 4 dp. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val LevelUpShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** true = sin animaciones decorativas (preferencia de la app o del sistema). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Preferencias actuales, para que cualquier pantalla respete modo sencillo, estilo de juego, etc. */
val LocalPrefs = staticCompositionLocalOf { Preferences() }

/** false = esta compilación no tiene la nube: no se ofrece cuenta, amigos ni nada que hable de ellos. */
val LocalHasAccount = staticCompositionLocalOf { false }

@Composable
fun isDarkTheme(prefs: Preferences): Boolean = when (prefs.theme) {
    ThemePref.SYSTEM -> isSystemInDarkTheme()
    ThemePref.LIGHT -> false
    ThemePref.DARK -> true
}

@Composable
fun LevelUpTheme(prefs: Preferences, systemReducedMotion: Boolean, plus: Boolean = false, content: @Composable () -> Unit) {
    val dark = isDarkTheme(prefs)
    // Sin Plus (p. ej. tras un reembolso) se vuelve al color de siempre, sin tocar la preferencia guardada.
    val accent = Accents.firstOrNull { it.key == prefs.accent && (plus || it.key == DEFAULT_ACCENT) } ?: Accents.first()
    val base = (if (dark) Dark else Light).withAccent(accent, dark)
    val contrasted = if (prefs.highContrast) base.highContrast(dark) else base
    val colors = if (prefs.lowStimulation) contrasted.muted() else contrasted
    val reduce = prefs.lowStimulation || !prefs.gameOn || when (prefs.motion) {
        MotionPref.SYSTEM -> systemReducedMotion
        MotionPref.REDUCED -> true
        MotionPref.FULL -> false
    }
    val density = LocalDensity.current
    // El tamaño de texto de la app se suma al del sistema (los dos multiplican el sp).
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * prefs.textScale),
        LocalReduceMotion provides reduce,
        LocalPrefs provides prefs,
    ) {
        MaterialTheme(colorScheme = colors, shapes = LevelUpShapes, content = content)
    }
}
