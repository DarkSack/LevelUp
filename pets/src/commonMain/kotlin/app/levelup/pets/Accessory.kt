package app.levelup.pets

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Dónde va un accesorio: se lleva uno por sitio a la vez. Lo de [SIDE] vuela o flota junto a la mascota. */
enum class Slot { HEAD, FACE, NECK, SIDE }

/** Cosas que se le ponen a cualquier mascota. Cuál se desbloquea y cuándo lo decide la app. */
enum class Accessory(val key: String, val slot: Slot) {
    MONO("mono", Slot.HEAD), FLOR("flor", Slot.HEAD), BOINA("boina", Slot.HEAD), GORRA("gorra", Slot.HEAD),
    AUDIFONOS("audifonos", Slot.HEAD), GORRO("gorro", Slot.HEAD), NAVIDAD("navidad", Slot.HEAD), MAGO("mago", Slot.HEAD),
    VIKINGO("vikingo", Slot.HEAD), COPA("copa", Slot.HEAD), SOMBRERO("sombrero", Slot.HEAD), CORONA("corona", Slot.HEAD),
    HALO("halo", Slot.HEAD), CORONA_FUEGO("corona_fuego", Slot.HEAD),
    LENTES("lentes", Slot.FACE), CORAZONES("corazones", Slot.FACE), REDONDOS("redondos", Slot.FACE),
    MONOCULO("monoculo", Slot.FACE), BIGOTE("bigote", Slot.FACE), ANTIFAZ("antifaz", Slot.FACE),
    PALIACATE("paliacate", Slot.NECK), CASCABEL("cascabel", Slot.NECK), CORBATIN("corbatin", Slot.NECK),
    BUFANDA("bufanda", Slot.NECK), COLLAR("collar", Slot.NECK), CADENA("cadena", Slot.NECK), MEDALLA("medalla", Slot.NECK),
    GLOBO("globo", Slot.SIDE), MARIPOSA("mariposa", Slot.SIDE), LUCIERNAGAS("luciernagas", Slot.SIDE);

    companion object {
        fun byKey(key: String): Accessory? = entries.firstOrNull { it.key == key }

        /** Lista guardada como texto ("mono,lentes") a conjunto; ignora lo que no conoce. */
        fun parse(keys: String): Set<Accessory> = keys.split(',').mapNotNull { byKey(it.trim()) }.toSet()

        fun format(set: Set<Accessory>): String = set.sortedBy { it.ordinal }.joinToString(",") { it.key }
    }
}

/** Ponerse [a]: reemplaza lo que hubiera en su sitio; si ya lo lleva, se lo quita. */
fun Set<Accessory>.toggle(a: Accessory): Set<Accessory> =
    if (a in this) this - a else filterNot { it.slot == a.slot }.toSet() + a

private val Gold = Color(0xFFFFC21A)
private val Red = Color(0xFFE63946)
private val TAU = 2 * PI.toFloat()

/** [beat] va de 0 a 1 en cada vuelta de la animación: lo usan lo que se mueve (halo, globo, mariposa…). */
internal fun DrawScope.accessory(a: Accessory, species: Species, beat: Float) {
    val f = species.face
    val wave = sin(TAU * beat)
    when (a) {
        Accessory.MONO -> {
            val pink = Color(0xFFFF6FA5)
            filledPath(Path().apply { moveTo(64f, 28f); lineTo(55f, 21f); lineTo(56f, 33f); close() }, pink)
            filledPath(Path().apply { moveTo(64f, 28f); lineTo(73f, 21f); lineTo(72f, 33f); close() }, pink)
            part(pink, 64f, 27.5f, 3f, 3f)
        }
        Accessory.FLOR -> {
            // Cempasúchil: dos coronas de pétalos naranjas.
            for ((r, color) in listOf(5.5f to Color(0xFFFF9F1C), 3.2f to Color(0xFFFFB627))) {
                for (i in 0 until 10) {
                    val t = TAU * i / 10
                    part(color, 64f + r * cos(t), 27f + r * sin(t), 2.6f, 2.6f, outline = r > 4f)
                }
            }
            part(Color(0xFFE76F00), 64f, 27f, 2f, 2f, outline = false)
        }
        Accessory.BOINA -> rotate(-12f, pivot = Offset(50f, 26f)) {
            part(Color(0xFFC1121F), 50f, 25f, 15f, 5.5f, shine = true)
            part(Color(0xFFC1121F), 50f, 19.2f, 1.5f, 1.8f)
        }
        Accessory.GORRA -> {
            filledPath(Path().apply { moveTo(36f, 29f); cubicTo(36f, 12f, 64f, 12f, 64f, 29f); close() }, Color(0xFF2563EB))
            filledPath(Path().apply { moveTo(58f, 28f); quadraticTo(76f, 26f, 77f, 30f); quadraticTo(70f, 32f, 58f, 30.5f); close() }, Color(0xFF1E40AF))
            part(Color.White, 50f, 15f, 1.6f, 1.6f)
            drawLine(Color.White.copy(alpha = 0.5f), Offset(42f, 20f), Offset(46f, 17f), 1.4f, StrokeCap.Round)
        }
        Accessory.AUDIFONOS -> {
            inkedPath(Path().apply { moveTo(29f, 42f); cubicTo(28f, 16f, 72f, 16f, 71f, 42f) }, Color(0xFF3F3D56), 2.5f)
            part(Color(0xFFFF6FA5), 28.5f, 45f, 4.5f, 6.5f, shine = true)
            part(Color(0xFFFF6FA5), 71.5f, 45f, 4.5f, 6.5f, shine = true)
        }
        Accessory.GORRO -> {
            filledPath(Path().apply { moveTo(41f, 29f); lineTo(59f, 29f); lineTo(52f, 5f); close() }, Color(0xFF4CC9F0))
            drawLine(Color(0xFFFFD23F), Offset(44f, 22f), Offset(56.5f, 21f), 2.2f, StrokeCap.Round)
            drawLine(Color(0xFFFF6FA5), Offset(47f, 14f), Offset(54f, 13.5f), 2.2f, StrokeCap.Round)
            part(Color(0xFFFFD23F), 52f, 5f, 3.2f, 3.2f)
        }
        Accessory.NAVIDAD -> {
            val tip = Offset(70f, 18f + 1.5f * wave)
            filledPath(Path().apply { moveTo(38f, 27f); quadraticTo(44f, 8f, 56f, 6f); quadraticTo(66f, 9f, tip.x, tip.y); quadraticTo(62f, 15f, 62f, 27f); close() }, Color(0xFFD62828))
            drawRoundRect(Color.White, Offset(36f, 24f), Size(28f, 6f), CornerRadius(3f))
            drawRoundRect(Ink, Offset(36f, 24f), Size(28f, 6f), CornerRadius(3f), style = Stroke(LINE))
            part(Color.White, tip.x, tip.y, 3.5f, 3.5f)
        }
        Accessory.MAGO -> {
            val purple = Color(0xFF6C4AB6)
            filledPath(Path().apply { moveTo(38f, 28f); quadraticTo(46f, 16f, 47f, 4f); quadraticTo(56f, 10f, 62f, 28f); close() }, purple)
            part(purple, 50f, 28f, 19f, 4f)
            for ((i, xy) in listOf(48f to 14f, 53f to 22f, 44f to 23f).withIndex()) {
                val twinkle = 1.2f + 0.8f * abs(sin(TAU * (beat + i / 3f)))
                part(Gold, xy.first, xy.second, twinkle, twinkle, outline = false)
            }
        }
        Accessory.VIKINGO -> {
            val ivory = Color(0xFFF3E9D2)
            filledPath(Path().apply { moveTo(37f, 24f); quadraticTo(26f, 22f, 24f, 8f); quadraticTo(31f, 16f, 40f, 19f); close() }, ivory)
            filledPath(Path().apply { moveTo(63f, 24f); quadraticTo(74f, 22f, 76f, 8f); quadraticTo(69f, 16f, 60f, 19f); close() }, ivory)
            filledPath(Path().apply { moveTo(35f, 29f); cubicTo(35f, 12f, 65f, 12f, 65f, 29f); close() }, Color(0xFF9AA5B1))
            drawLine(Color(0xFFB08D57), Offset(35.5f, 27f), Offset(64.5f, 27f), 3f)
            for (x in listOf(40f, 50f, 60f)) part(Color(0xFF6B7684), x, 22f, 1.2f, 1.2f, outline = false)
        }
        Accessory.COPA -> {
            val black = Color(0xFF22202A)
            filledPath(Path().apply { moveTo(41f, 27f); lineTo(42f, 8f); lineTo(58f, 8f); lineTo(59f, 27f); close() }, black)
            drawRect(Red, Offset(41.3f, 21.5f), Size(17.4f, 3.5f))
            part(black, 50f, 27f, 16f, 3.5f)
            drawLine(Color.White.copy(alpha = 0.35f), Offset(45f, 11f), Offset(45f, 19f), 1.4f, StrokeCap.Round)
        }
        Accessory.SOMBRERO -> {
            val straw = Color(0xFFF2C94C)
            part(straw, 50f, 26f, 30f, 5.5f)
            filledPath(Path().apply { moveTo(39f, 26f); cubicTo(39f, 10f, 61f, 10f, 61f, 26f); close() }, straw)
            drawLine(Red, Offset(40f, 22.5f), Offset(60f, 22.5f), 2.6f, StrokeCap.Butt)
            drawLine(Color(0xFF2A9D8F), Offset(40f, 25f), Offset(60f, 25f), 1.4f, StrokeCap.Butt)
        }
        Accessory.CORONA, Accessory.CORONA_FUEGO -> {
            if (a == Accessory.CORONA_FUEGO) {
                // Tres llamitas que bailan encima de la corona.
                for ((i, x) in listOf(41f, 50f, 59f).withIndex()) {
                    val h = (if (i == 1) 14f else 10f) * (1f + 0.12f * sin(TAU * (2f * beat + i * 0.3f)))
                    val lean = 1.5f * sin(TAU * (beat + i * 0.25f))
                    filledPath(flamePath(x, 17f, 4.5f, h, lean), Color(0xFFFF6A00), line = 1.2f)
                    drawPath(flamePath(x, 16.5f, 2.4f, h * 0.55f, lean * 0.6f), Color(0xFFFFE08A))
                }
            }
            filledPath(Path().apply {
                moveTo(39f, 29f); lineTo(38f, 14f); lineTo(44.5f, 21f); lineTo(50f, 10f); lineTo(55.5f, 21f)
                lineTo(62f, 14f); lineTo(61f, 29f); close()
            }, Gold)
            part(Red, 50f, 24f, 2.2f, 2.2f)
            part(Color(0xFF4CC9F0), 43f, 25f, 1.6f, 1.6f)
            part(Color(0xFF4CC9F0), 57f, 25f, 1.6f, 1.6f)
        }
        Accessory.HALO -> {
            val y = 14f + 1.5f * wave
            drawOval(Brush.radialGradient(listOf(Gold.copy(alpha = 0.45f), Color.Transparent), Offset(50f, y + 3.5f), 18f), Offset(32f, y - 6f), Size(36f, 19f))
            drawOval(Color(0xFFFFE08A), Offset(38f, y), Size(24f, 7f), style = Stroke(3.2f))
            drawOval(Gold, Offset(38f, y), Size(24f, 7f), style = Stroke(1.4f))
        }
        Accessory.LENTES, Accessory.CORAZONES -> {
            drawLine(Ink, Offset(f.left, f.eyeY - 1f), Offset(f.right, f.eyeY - 1f), LINE, StrokeCap.Round)
            for (x in listOf(f.left, f.right)) {
                if (a == Accessory.LENTES) {
                    drawRoundRect(Color(0xFF1B1B2F), Offset(x - 6f, f.eyeY - 4.5f), Size(12f, 8.5f), CornerRadius(3.5f))
                    drawRoundRect(Ink, Offset(x - 6f, f.eyeY - 4.5f), Size(12f, 8.5f), CornerRadius(3.5f), style = Stroke(LINE))
                } else {
                    filledPath(heartPath(Offset(x, f.eyeY), 5.2f), Color(0xFFFF4D7E))
                }
                drawLine(Color.White.copy(alpha = 0.6f), Offset(x - 3.5f, f.eyeY - 2f), Offset(x - 1f, f.eyeY - 3.2f), 1.2f, StrokeCap.Round)
            }
        }
        Accessory.REDONDOS -> {
            val brown = Color(0xFF8B5E34)
            drawPath(Path().apply { moveTo(f.left + 5f, f.eyeY - 1f); quadraticTo(50f, f.eyeY - 4f, f.right - 5f, f.eyeY - 1f) }, brown, style = Stroke(1.6f))
            for (x in listOf(f.left, f.right)) {
                drawCircle(Color.White.copy(alpha = 0.25f), 5.2f, Offset(x, f.eyeY))
                drawCircle(brown, 5.2f, Offset(x, f.eyeY), style = Stroke(1.8f))
            }
        }
        Accessory.MONOCULO -> {
            drawPath(Path().apply { moveTo(f.right + 3.7f, f.eyeY + 3.7f); quadraticTo(f.right + 8f, f.eyeY + 12f, f.right + 5f, f.eyeY + 19f) },
                Gold, style = Stroke(1f, cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.3f), 5.4f, Offset(f.right, f.eyeY))
            drawCircle(Gold, 5.4f, Offset(f.right, f.eyeY), style = Stroke(1.9f))
        }
        Accessory.BIGOTE -> {
            val y = f.noseY + 2.4f
            for (dir in listOf(-1f, 1f)) {
                drawPath(Path().apply {
                    moveTo(50f, y); cubicTo(50f + dir * 4f, y - 2.5f, 50f + dir * 9f, y - 1f, 50f + dir * 10f, y + 1.5f)
                    cubicTo(50f + dir * 7f, y + 2.5f, 50f + dir * 3f, y + 2f, 50f, y + 0.8f); close()
                }, Color(0xFF3B2A20))
            }
        }
        Accessory.ANTIFAZ -> {
            // Máscara de luchador: azul con ribete dorado, orificios para los ojos y un rombo rosa en la frente.
            val blue = Color(0xFF1D4ED8)
            val left = f.left - 10f
            val width = f.right - f.left + 20f
            for (dir in listOf(-1f, 1f)) {
                val edge = if (dir < 0) left else left + width
                filledPath(Path().apply { moveTo(edge, f.eyeY - 3f); lineTo(edge + dir * 5f, f.eyeY - 6f); lineTo(edge, f.eyeY + 2f); close() }, blue)
            }
            drawRoundRect(blue, Offset(left, f.eyeY - 7f), Size(width, 13f), CornerRadius(6.5f))
            drawRoundRect(Ink, Offset(left, f.eyeY - 7f), Size(width, 13f), CornerRadius(6.5f), style = Stroke(LINE))
            drawRoundRect(Gold, Offset(left + 1.8f, f.eyeY - 5.2f), Size(width - 3.6f, 9.4f), CornerRadius(4.7f), style = Stroke(0.9f))
            filledPath(Path().apply { moveTo(50f, f.eyeY - 6f); lineTo(52.5f, f.eyeY - 2.5f); lineTo(50f, f.eyeY + 1f); lineTo(47.5f, f.eyeY - 2.5f); close() },
                Color(0xFFFF3D7F), line = 0.9f)
            for (x in listOf(f.left, f.right)) {
                part(Color.White, x, f.eyeY, 4.2f, 3.6f)
                drawCircle(Ink, 2.2f, Offset(x, f.eyeY))
            }
        }
        Accessory.PALIACATE -> {
            filledPath(Path().apply { moveTo(33f, 60f); lineTo(67f, 60f); lineTo(50f, 75f); close() }, Color(0xFFD62828))
            for ((x, y) in listOf(42f to 63f, 50f to 63f, 58f to 63f, 46f to 67f, 54f to 67f, 50f to 71f)) drawCircle(Color.White, 1f, Offset(x, y))
        }
        Accessory.CASCABEL -> {
            drawRoundRect(Red, Offset(35f, 59.5f), Size(30f, 4f), CornerRadius(2f))
            drawRoundRect(Ink, Offset(35f, 59.5f), Size(30f, 4f), CornerRadius(2f), style = Stroke(1.4f))
            rotate(8f * wave, pivot = Offset(50f, 62f)) {
                part(Gold, 50f, 66f, 3.6f, 3.6f, shine = true)
                drawLine(Ink, Offset(50f, 66.5f), Offset(50f, 69.5f), 1f, StrokeCap.Round)
            }
        }
        Accessory.CORBATIN -> {
            val purple = Color(0xFF7C3AED)
            filledPath(Path().apply { moveTo(50f, 62f); lineTo(41f, 57.5f); lineTo(41f, 66.5f); close() }, purple)
            filledPath(Path().apply { moveTo(50f, 62f); lineTo(59f, 57.5f); lineTo(59f, 66.5f); close() }, purple)
            part(Color(0xFF9F67FF), 50f, 62f, 2.6f, 2.6f)
        }
        Accessory.BUFANDA -> {
            drawRoundRect(Red, Offset(56f, 64f), Size(7f, 12f), CornerRadius(2f))
            drawRoundRect(Ink, Offset(56f, 64f), Size(7f, 12f), CornerRadius(2f), style = Stroke(LINE))
            drawRoundRect(Red, Offset(33f, 61f), Size(34f, 7f), CornerRadius(3.5f))
            drawRoundRect(Ink, Offset(33f, 61f), Size(34f, 7f), CornerRadius(3.5f), style = Stroke(LINE))
            drawLine(Color.White.copy(alpha = 0.7f), Offset(37f, 64.5f), Offset(63f, 64.5f), 1.4f, StrokeCap.Round)
        }
        Accessory.COLLAR -> {
            // Collar de flores: cuentas de colores que cuelgan en arco.
            val colors = listOf(Color(0xFFFF6FA5), Color(0xFFFFD23F), Color.White, Color(0xFFB388FF))
            for (i in 0..8) {
                val t = i / 8f
                part(colors[i % colors.size], 32f + 36f * t, 60f + 8f * sin(PI.toFloat() * t), 3.2f, 3.2f)
            }
        }
        Accessory.CADENA -> {
            for (i in 0..14) {
                val t = i / 14f
                drawCircle(Gold, 1.3f, Offset(34f + 32f * t, 60f + 6f * sin(PI.toFloat() * t)))
            }
            filledPath(heartPath(Offset(50f, 69.5f), 3.2f), Gold, line = 1f)
        }
        Accessory.MEDALLA -> {
            val blue = Color(0xFF3A86FF)
            drawLine(blue, Offset(41f, 59f), Offset(48.5f, 70f), 3.2f, StrokeCap.Round)
            drawLine(blue, Offset(59f, 59f), Offset(51.5f, 70f), 3.2f, StrokeCap.Round)
            part(Gold, 50f, 73.5f, 5.5f, 5.5f, shine = true)
            filledPath(starPath(Offset(50f, 73.5f), 3f), Color(0xFFFFE08A), line = 0.8f)
        }
        Accessory.GLOBO -> {
            val c = Offset(82f + 3f * sin(TAU * beat - 0.3f), 24f + 2f * wave)
            drawPath(Path().apply { moveTo(66f, 78f); quadraticTo(78f, 52f, c.x, c.y + 11f) }, Ink, style = Stroke(0.9f))
            filledPath(Path().apply { moveTo(c.x, c.y + 9.5f); lineTo(c.x - 1.6f, c.y + 12f); lineTo(c.x + 1.6f, c.y + 12f); close() }, Color(0xFFFF4D6D), line = 0.9f)
            part(Color(0xFFFF4D6D), c.x, c.y, 8f, 10f, shine = true)
        }
        Accessory.MARIPOSA -> {
            // Vuela en un ocho alrededor de la cabeza y aletea.
            val c = Offset(50f + 34f * sin(TAU * beat), 22f + 8f * sin(2f * TAU * beat))
            val flap = 0.25f + 0.75f * abs(cos(6f * TAU * beat))
            for (side in listOf(-1f, 1f)) {
                part(Color(0xFFFF9F1C), c.x + side * 4f * flap, c.y - 2f, 4f * flap, 4.5f, outline = false)
                part(Color(0xFFB388FF), c.x + side * 3f * flap, c.y + 2.5f, 3f * flap, 3f, outline = false)
            }
            drawLine(Ink, Offset(c.x, c.y - 4f), Offset(c.x, c.y + 4f), 1.4f, StrokeCap.Round)
        }
        Accessory.LUCIERNAGAS -> {
            for (i in 0..2) {
                val angle = TAU * (beat + i / 3f)
                val c = Offset(50f + 40f * cos(angle), 56f + 16f * sin(angle))
                val glow = 0.5f + 0.5f * sin(3f * TAU * (beat + i * 0.2f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF176).copy(alpha = 0.7f * glow), Color.Transparent), c, 7f), 7f, c)
                drawCircle(Color(0xFFFFF59D), 1.7f, c)
            }
        }
    }
}

internal fun heartPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y + r * 0.9f)
    cubicTo(c.x - r * 1.7f, c.y - r * 0.2f, c.x - r * 0.6f, c.y - r * 1.5f, c.x, c.y - r * 0.5f)
    cubicTo(c.x + r * 0.6f, c.y - r * 1.5f, c.x + r * 1.7f, c.y - r * 0.2f, c.x, c.y + r * 0.9f)
    close()
}

internal fun starPath(c: Offset, r: Float) = Path().apply {
    for (i in 0 until 10) {
        val t = -PI.toFloat() / 2 + PI.toFloat() * i / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        if (i == 0) moveTo(c.x + rr * cos(t), c.y + rr * sin(t)) else lineTo(c.x + rr * cos(t), c.y + rr * sin(t))
    }
    close()
}
