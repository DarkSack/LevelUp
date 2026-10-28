package app.levelup.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.levelup.domain.AppStyle

/** Lo que cambia entre estilos además de los colores: letra, esquinas, grosor del borde y sombra sólida. */
class StyleTokens(val typography: Typography, val shapes: Shapes, val border: Dp, val shadow: Boolean)

/** Títulos y botones con [display]; el texto corrido con [body], para que se lea bien aunque el título sea pixelado. */
private fun typography(display: FontFamily, body: FontFamily): Typography = Typography().run {
    fun TextStyle.f(family: FontFamily, weight: FontWeight) = copy(fontFamily = family, fontWeight = weight)
    Typography(
        displayLarge = displayLarge.f(display, FontWeight.Bold), displayMedium = displayMedium.f(display, FontWeight.Bold),
        displaySmall = displaySmall.f(display, FontWeight.Bold), headlineLarge = headlineLarge.f(display, FontWeight.Bold),
        headlineMedium = headlineMedium.f(display, FontWeight.Bold), headlineSmall = headlineSmall.f(display, FontWeight.Bold),
        titleLarge = titleLarge.f(display, FontWeight.SemiBold), titleMedium = titleMedium.f(display, FontWeight.SemiBold),
        titleSmall = titleSmall.f(display, FontWeight.SemiBold), labelLarge = labelLarge.f(display, FontWeight.SemiBold),
        bodyLarge = bodyLarge.f(body, FontWeight.Normal), bodyMedium = bodyMedium.f(body, FontWeight.Normal),
        bodySmall = bodySmall.f(body, FontWeight.Normal), labelMedium = labelMedium.f(body, FontWeight.Medium),
        labelSmall = labelSmall.f(body, FontWeight.Medium),
    )
}

private fun shapes(small: Int, medium: Int, large: Int) =
    Shapes(small = RoundedCornerShape(small.dp), medium = RoundedCornerShape(medium.dp), large = RoundedCornerShape(large.dp))

private val tokens = mapOf(
    AppStyle.POP to StyleTokens(typography(PopFont, PopFont), shapes(10, 18, 24), 2.dp, shadow = true),
    AppStyle.AVENTURA to StyleTokens(typography(AdventureFont, FontFamily.Default), shapes(4, 6, 8), 2.dp, shadow = false),
    AppStyle.NEON to StyleTokens(typography(NeonFont, NeonFont), shapes(12, 16, 24), 1.dp, shadow = false),
)

fun tokensFor(style: AppStyle): StyleTokens = tokens.getValue(style)

val LocalStyleTokens = staticCompositionLocalOf { tokensFor(AppStyle.POP) }

/** Grosor de los bordes del estilo actual. */
val StyleBorder: Dp @Composable get() = LocalStyleTokens.current.border

private val ShadowOffset = 4.dp

/** Sombra sólida desplazada (solo en Pop, y nunca en baja estimulación). */
@Composable
fun Modifier.styleShadow(shape: Shape = MaterialTheme.shapes.medium): Modifier {
    if (!LocalStyleTokens.current.shadow || LocalPrefs.current.lowStimulation) return this
    val color = MaterialTheme.colorScheme.outline
    return padding(end = ShadowOffset, bottom = ShadowOffset).drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(ShadowOffset.toPx(), ShadowOffset.toPx()) { drawOutline(outline, color) }
    }
}

/** Aventura: noche índigo, oro en bordes y botones, pergamino para el reto principal. */
internal val Aventura = darkColorScheme(
    primary = Color(0xFFF5B83D), onPrimary = Color(0xFF1B1838),
    primaryContainer = Color(0xFFF3E8C8), onPrimaryContainer = Color(0xFF3A2A12),
    secondary = Color(0xFFFFA552), onSecondary = Color(0xFF1B1838),
    secondaryContainer = Color(0xFF3B3470), onSecondaryContainer = Color(0xFFEDE7D3),
    tertiary = Color(0xFFA99BFF), onTertiary = Color(0xFF1B1838),
    tertiaryContainer = Color(0xFF4A3F8C), onTertiaryContainer = Color(0xFFEDE7D3),
    background = Color(0xFF1B1838), onBackground = Color(0xFFEDE7D3),
    surface = Color(0xFF26214D), onSurface = Color(0xFFEDE7D3),
    surfaceVariant = Color(0xFF322B63), onSurfaceVariant = Color(0xFFC4BDE3),
    surfaceContainer = Color(0xFF26214D), surfaceContainerHigh = Color(0xFF322B63),
    outline = Color(0xFFF5B83D), outlineVariant = Color(0xFF3B3470),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

/** Neón: casi negro con lima y cian eléctricos; bordes finos y sin sombras. */
internal val Neon = darkColorScheme(
    primary = Color(0xFFC6FF3D), onPrimary = Color(0xFF0D0F12),
    primaryContainer = Color(0xFF26331A), onPrimaryContainer = Color(0xFFE4FFB0),
    secondary = Color(0xFF3DD9FF), onSecondary = Color(0xFF0D0F12),
    secondaryContainer = Color(0xFF10313A), onSecondaryContainer = Color(0xFFC5F5FF),
    tertiary = Color(0xFFFF5AD9), onTertiary = Color(0xFF0D0F12),
    tertiaryContainer = Color(0xFF3A1034), onTertiaryContainer = Color(0xFFFFD3F5),
    background = Color(0xFF0D0F12), onBackground = Color(0xFFE9ECEF),
    surface = Color(0xFF171B21), onSurface = Color(0xFFE9ECEF),
    surfaceVariant = Color(0xFF1F242B), onSurfaceVariant = Color(0xFF9AA3AF),
    surfaceContainer = Color(0xFF171B21), surfaceContainerHigh = Color(0xFF1F242B),
    outline = Color(0xFF3A4350), outlineVariant = Color(0xFF1F242B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)
