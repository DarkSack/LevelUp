package app.levelup.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.levelup.shared.R

// Archivos variables: cada peso fija su eje wght.
private fun variable(res: Int) = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

actual val PopFont: FontFamily = variable(R.font.fredoka)
actual val AdventureFont: FontFamily = variable(R.font.pixelify)
actual val NeonFont: FontFamily = variable(R.font.spacegrotesk)
