package app.levelup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import app.levelup.domain.TrophyTier
import java.io.File
import app.levelup.i18n.tr

/** Dibuja la tarjeta de un logro en un PNG cuadrado y la deja en caché para la hoja de compartir. */
object ShareImage {
    private const val SIZE = 1080

    fun write(context: Context, card: ShareCard): Uri {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (background, accent) = when (card.tier) {
            TrophyTier.BRONZE -> 0xFF7C2D12.toInt() to 0xFFFDBA74.toInt()
            TrophyTier.SILVER -> 0xFF334155.toInt() to 0xFFE2E8F0.toInt()
            TrophyTier.GOLD -> 0xFF713F12.toInt() to 0xFFFDE047.toInt()
            TrophyTier.PLATINUM -> 0xFF1E3A5F.toInt() to 0xFFBAE6FD.toInt()
        }
        canvas.drawColor(0xFF0F766E.toInt())
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
        canvas.drawRoundRect(RectF(72f, 72f, SIZE - 72f, SIZE - 72f), 64f, 64f, panel)

        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 44f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(tr("LOGRO DESBLOQUEADO"), 140f, 230f, small)
        // Medalla: un círculo del color del nivel del trofeo.
        canvas.drawCircle(SIZE - 210f, 210f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 88f; typeface = Typeface.DEFAULT_BOLD }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F5F9.toInt(); textSize = 48f }
        var y = 340f
        y += drawWrapped(canvas, tr(card.title), title, 140f, y)
        y += 40f
        drawWrapped(canvas, tr(card.description), body, 140f, y)

        val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 40f }
        canvas.drawText(card.date, 140f, SIZE - 190f, footer)
        canvas.drawText("LevelUp", 140f, SIZE - 130f, footer.apply { typeface = Typeface.DEFAULT_BOLD })

        val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
        val file = File(dir, "logro.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.archivos", file)
    }

    /** Devuelve la altura que ocupó el texto. */
    private fun drawWrapped(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, SIZE - 2 * x.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(6).build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }
}
