package app.levelup.ui.theme

import androidx.compose.ui.text.font.FontFamily

// Las tres con licencia SIL OFL 1.1 (docs/licencias).
/** Fredoka, redondeada: estilo Pop. */
expect val PopFont: FontFamily

/** Pixelify Sans, pixelada: títulos del estilo Aventura. */
expect val AdventureFont: FontFamily

/** Space Grotesk, geométrica: estilo Neón. */
expect val NeonFont: FontFamily

/** Iconos de la barra de estado y de navegación claros sobre fondo oscuro, y al revés. */
@androidx.compose.runtime.Composable
expect fun SystemBarsEffect(dark: Boolean)
