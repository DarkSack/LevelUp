package app.levelup.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.levelup.shared.R

// Un archivo por grosor, sacado de la fuente variable con fontTools: HyperOS (Xiaomi) ignora el eje wght
// y lo pinta todo fino.
actual val PopFont: FontFamily = FontFamily(
    Font(R.font.fredoka_400, FontWeight.Normal), Font(R.font.fredoka_500, FontWeight.Medium),
    Font(R.font.fredoka_600, FontWeight.SemiBold), Font(R.font.fredoka_700, FontWeight.Bold),
)

// Solo títulos y botones: el texto corrido de Aventura va con la letra del sistema.
actual val AdventureFont: FontFamily = FontFamily(
    Font(R.font.pixelify_600, FontWeight.SemiBold), Font(R.font.pixelify_700, FontWeight.Bold),
)

actual val NeonFont: FontFamily = FontFamily(
    Font(R.font.spacegrotesk_400, FontWeight.Normal), Font(R.font.spacegrotesk_500, FontWeight.Medium),
    Font(R.font.spacegrotesk_600, FontWeight.SemiBold), Font(R.font.spacegrotesk_700, FontWeight.Bold),
)
