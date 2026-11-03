package app.levelup.pets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

// Mismo lienzo y mismas reglas que Drawings.kt: cabeza hacia y = 44, cuerpo hacia y = 73, suelo en y = 92.

// Xoloitzcuintle: sin pelo, gris pizarra, orejas enormes y un copete.
private val XoBody = Color(0xFF5E6676)
private val XoLight = Color(0xFF808999)
private val XoDark = Color(0xFF363B47)
private val XoEar = Color(0xFFC9969A)

internal fun DrawScope.xolo(p: Pose) {
    rotate(p.sway, pivot = Offset(66f, 84f)) {
        inkedPath(Path().apply { moveTo(64f, 86f); cubicTo(82f, 86f, 90f, 74f, 88f, 60f) }, XoBody, 3.5f)
    }
    for (x in listOf(40f, 60f)) part(XoBody, x, 90f, 6f, 3.5f)
    part(XoBody, 50f, 72f, 20f, 19f, shine = true)
    part(XoLight, 50f, 77f, 11f, 11f, outline = false)
    part(XoBody, 42f, 85f, 4f, 4f)
    part(XoBody, 58f, 85f, 4f, 4f)
    head(p) {
        for (m in listOf(false, true)) {
            fun x(v: Float) = if (m) 100f - v else v
            rotate(if (m) p.earRight else -p.earLeft, pivot = Offset(x(37f), 32f)) {
                filledPath(Path().apply { moveTo(x(31f), 36f); lineTo(x(24f), 7f); lineTo(x(45f), 27f); close() }, XoBody)
                drawPath(Path().apply { moveTo(x(32f), 31f); lineTo(x(27.5f), 13f); lineTo(x(41f), 27f); close() }, XoEar)
            }
        }
        part(XoBody, 50f, 44f, 20f, 17f, shine = true)
        for ((x, y) in listOf(45f to 28.5f, 50f to 26.5f, 55f to 28.5f)) part(XoDark, x, y, 3f, 4f, outline = false)
        for (dy in listOf(0f, 3f)) drawLine(XoDark.copy(alpha = 0.6f), Offset(46f, 34f + dy), Offset(54f, 34f + dy), 1.2f, StrokeCap.Round)
        part(XoLight, 50f, 52f, 9.5f, 7f)
        mouth(p, 50f, 55.5f, 3f)
        part(Color(0xFF1E1E26), 50f, 49.5f, 2.8f, 2f)
        eyes(p, 41f, 59f, 42f)
        cheeks(p, 35f, 65f, 49f)
    }
}

// Conejo: crema, orejas largas que se caen mucho cuando está triste, dientitos y cola de algodón.
private val Bun = Color(0xFFF7F1EA)
private val BunPink = Color(0xFFF6A9B8)

internal fun DrawScope.conejo(p: Pose) {
    part(Bun, 69f + p.sway * 0.05f, 85f, 6.5f, 6f)
    for (x in listOf(39f, 61f)) part(Bun, x, 89.5f, 8f, 4f)
    part(Bun, 50f, 72f, 20f, 19f, shine = true)
    part(Color.White, 50f, 77f, 11f, 11f, outline = false)
    part(Bun, 43f, 82f, 3.8f, 4.5f)
    part(Bun, 57f, 82f, 3.8f, 4.5f)
    head(p) {
        for (m in listOf(false, true)) {
            val x = if (m) 58f else 42f
            rotate((if (m) p.earRight else -p.earLeft) * 1.6f + (if (m) 8f else -8f), pivot = Offset(x, 32f)) {
                part(Bun, x, 16f, 6.5f, 16f, shine = true)
                part(BunPink, x, 17f, 3.2f, 12f, outline = false)
            }
        }
        part(Bun, 50f, 45f, 20f, 17f, shine = true)
        part(Color.White, 46f, 52f, 5f, 4f, outline = false)
        part(Color.White, 54f, 52f, 5f, 4f, outline = false)
        whiskers(42f, 58f, 52f)
        if (p.sad) frown(50f, 56.5f, 3f)
        else {
            drawRect(Color.White, Offset(48.3f, 54.2f), Size(3.4f, 3.4f))
            drawRect(Ink, Offset(48.3f, 54.2f), Size(3.4f, 3.4f), style = Stroke(0.9f))
            drawLine(Ink, Offset(50f, 54.2f), Offset(50f, 57.6f), 0.8f)
        }
        filledPath(Path().apply { moveTo(48f, 49.5f); lineTo(52f, 49.5f); lineTo(50f, 51.8f); close() }, BunPink, line = 1.2f)
        eyes(p, 42f, 58f, 44f, 3.4f)
        cheeks(p, 36f, 64f, 50f)
    }
}

// Panda: blanco con orejas, brazos, patas y antifaz negros.
private val PandaW = Color(0xFFF7F7F2)
private val PandaB = Color(0xFF2A2830)

internal fun DrawScope.panda(p: Pose) {
    part(PandaW, 67f + p.sway * 0.05f, 87f, 4f, 3.5f)
    for (x in listOf(39f, 61f)) part(PandaB, x, 89f, 7.5f, 5f)
    part(PandaW, 50f, 72f, 21f, 19f, shine = true)
    part(PandaB, 31f, 75f, 5f, 8f)
    part(PandaB, 69f, 75f, 5f, 8f)
    head(p) {
        for ((x, left) in listOf(32f to true, 68f to false)) {
            rotate(if (left) -p.earLeft else p.earRight, pivot = Offset(if (left) 36f else 64f, 34f)) { part(PandaB, x, 29f, 7.5f, 7.5f) }
        }
        part(PandaW, 50f, 45f, 22f, 18f, shine = true)
        for ((x, angle) in listOf(40f to -20f, 60f to 20f)) {
            rotate(angle, pivot = Offset(x, 44f)) { part(PandaB, x, 44f, 6.5f, 8f, outline = false) }
            part(Color.White, x, 43f, 3.8f, 3.8f, outline = false)
        }
        part(PandaW, 50f, 52f, 7f, 5f, outline = false)
        part(PandaB, 50f, 50.5f, 3f, 2f)
        mouth(p, 50f, 54.5f, 2.6f)
        eyes(p, 40f, 60f, 43f, 2.6f)
        cheeks(p, 33f, 67f, 52f)
    }
}

// Rana: verde con ojos saltones arriba de la cabeza, boca de lado a lado y patas dobladas.
private val Frog = Color(0xFF7BC96F)
private val FrogDark = Color(0xFF4E9A4A)
private val FrogBelly = Color(0xFFDDF2B8)

internal fun DrawScope.rana(p: Pose) {
    part(Frog, 30f, 85f, 9f, 6f)
    part(Frog, 70f, 85f, 9f, 6f)
    part(FrogDark, 26f, 91f, 6f, 2.5f)
    part(FrogDark, 74f, 91f, 6f, 2.5f)
    part(Frog, 50f, 74f, 21f, 17f, shine = true)
    part(FrogBelly, 50f, 78f, 13f, 11f, outline = false)
    part(Frog, 41f, 88f, 3.5f, 4f)
    part(Frog, 59f, 88f, 3.5f, 4f)
    head(p) {
        part(Frog, 38f, 31f, 8.5f, 8.5f)
        part(Frog, 62f, 31f, 8.5f, 8.5f)
        part(Frog, 50f, 44f, 25f, 14f, shine = true)
        part(Color.White, 38f, 31f, 6f, 6f, outline = false)
        part(Color.White, 62f, 31f, 6f, 6f, outline = false)
        for ((x, y) in listOf(34f to 46f, 66f to 47f, 59f to 39f)) drawCircle(FrogDark, 1.6f, Offset(x, y))
        drawCircle(Ink, 0.8f, Offset(47f, 42f))
        drawCircle(Ink, 0.8f, Offset(53f, 42f))
        when {
            p.glad -> mouth(p, 50f, 48f, 6f)
            p.sad -> frown(50f, 49f, 7f)
            else -> drawPath(Path().apply { moveTo(38f, 47f); quadraticTo(50f, 53f, 62f, 47f) }, Ink, style = Stroke(LINE, cap = StrokeCap.Round))
        }
        eyes(p, 38f, 62f, 31f, 3.8f)
        cheeks(p, 32f, 68f, 48f)
    }
}

// Zorro: naranja con la cara y la punta de la cola blancas y las puntas de las orejas oscuras.
private val Fox = Color(0xFFF08A3C)
private val FoxWhite = Color(0xFFFFF4E8)
private val FoxDark = Color(0xFF3A2A2A)

internal fun DrawScope.zorro(p: Pose) {
    rotate(p.sway, pivot = Offset(64f, 84f)) {
        filledPath(Path().apply { moveTo(62f, 88f); cubicTo(80f, 94f, 98f, 76f, 88f, 54f); cubicTo(84f, 64f, 76f, 78f, 62f, 80f); close() }, Fox)
        filledPath(Path().apply { moveTo(88f, 54f); cubicTo(92f, 60f, 93f, 66f, 90.5f, 70f); quadraticTo(86f, 64f, 84.5f, 62f); close() }, FoxWhite)
    }
    for (x in listOf(40f, 60f)) part(FoxDark, x, 89.5f, 6f, 3.5f)
    part(Fox, 50f, 72f, 20f, 19f, shine = true)
    part(FoxWhite, 50f, 77f, 10f, 12f, outline = false)
    head(p) {
        for (m in listOf(false, true)) {
            fun x(v: Float) = if (m) 100f - v else v
            rotate(if (m) p.earRight else -p.earLeft, pivot = Offset(x(37f), 30f)) {
                filledPath(Path().apply { moveTo(x(29f), 36f); lineTo(x(28f), 12f); lineTo(x(46f), 27f); close() }, Fox)
                drawPath(Path().apply { moveTo(x(32f), 31f); lineTo(x(31f), 18f); lineTo(x(42f), 28f); close() }, FoxWhite)
                filledPath(Path().apply { moveTo(x(28f), 12f); lineTo(x(28.6f), 20f); lineTo(x(34.5f), 17f); close() }, FoxDark, line = 1f)
            }
        }
        part(Fox, 50f, 44f, 22f, 18f, shine = true)
        drawPath(Path().apply {
            moveTo(30f, 46f); quadraticTo(36f, 60f, 50f, 60f); quadraticTo(64f, 60f, 70f, 46f)
            quadraticTo(60f, 52f, 50f, 50f); quadraticTo(40f, 52f, 30f, 46f); close()
        }, FoxWhite)
        part(FoxDark, 50f, 53f, 3.2f, 2.3f)
        mouth(p, 50f, 56.5f, 2.6f)
        whiskers(42f, 58f, 55f)
        eyes(p, 41f, 59f, 42f)
        cheeks(p, 34f, 66f, 50f)
    }
}

// Alebrije: criatura de fantasía de colores vivos, con alas, cuernos y patrones. Solo la tienen los betatesters.
private val AlTeal = Color(0xFF14B8A6)
private val AlMint = Color(0xFF5EEAD4)
private val AlPink = Color(0xFFFF3D7F)
private val AlYellow = Color(0xFFFFD23F)
private val AlPurple = Color(0xFF8B5CF6)
private val AlOrange = Color(0xFFFF8A00)

internal fun DrawScope.alebrije(p: Pose) {
    val flap = 12f * sin(2 * PI.toFloat() * 2f * p.beat)
    for (side in listOf(-1f, 1f)) {
        val root = Offset(50f + side * 12f, 64f)
        rotate(side * (-12f + flap), pivot = root) {
            filledPath(Path().apply {
                moveTo(root.x, root.y); quadraticTo(50f + side * 42f, 36f, 50f + side * 38f, 60f)
                quadraticTo(50f + side * 30f, 70f, root.x, root.y + 6f); close()
            }, AlPurple)
            drawPath(Path().apply { moveTo(root.x, root.y + 2f); quadraticTo(50f + side * 30f, 48f, 50f + side * 34f, 58f) },
                AlYellow, style = Stroke(1.6f, cap = StrokeCap.Round))
            drawCircle(AlPink, 1.8f, Offset(50f + side * 31f, 62f))
        }
    }
    rotate(p.sway, pivot = Offset(64f, 84f)) {
        inkedPath(Path().apply { moveTo(64f, 86f); cubicTo(84f, 90f, 94f, 72f, 84f, 64f); cubicTo(78f, 60f, 74f, 68f, 80f, 71f) }, AlPink, 4.5f)
        drawCircle(AlYellow, 1.4f, Offset(78f, 87f))
        drawCircle(AlYellow, 1.4f, Offset(88f, 77f))
    }
    for (x in listOf(40f, 60f)) part(AlOrange, x, 90f, 6f, 3.5f)
    part(AlTeal, 50f, 72f, 20f, 19f, shine = true)
    part(AlYellow, 50f, 77f, 11f, 11f, outline = false)
    drawPath(Path().apply { moveTo(41f, 75f); lineTo(44f, 72f); lineTo(47f, 75f); lineTo(50f, 72f); lineTo(53f, 75f); lineTo(56f, 72f); lineTo(59f, 75f) },
        AlPurple, style = Stroke(1.6f, cap = StrokeCap.Round))
    for ((x, y) in listOf(36f to 68f, 64f to 68f, 38f to 61f, 62f to 61f)) drawCircle(AlPink, 1.8f, Offset(x, y))
    part(AlOrange, 42f, 85f, 4f, 4f)
    part(AlOrange, 58f, 85f, 4f, 4f)
    head(p) {
        for (m in listOf(false, true)) {
            fun x(v: Float) = if (m) 100f - v else v
            filledPath(Path().apply { moveTo(x(41f), 30f); quadraticTo(x(31f), 17f, x(38f), 9f); quadraticTo(x(38f), 20f, x(46f), 28f); close() }, AlYellow)
            rotate(if (m) p.earRight else -p.earLeft, pivot = Offset(x(33f), 36f)) {
                filledPath(Path().apply { moveTo(x(31f), 41f); lineTo(x(19f), 27f); lineTo(x(37f), 32f); close() }, AlPurple)
            }
        }
        part(AlTeal, 50f, 44f, 21f, 17f, shine = true)
        drawPath(Path().apply {
            moveTo(38f, 33f); lineTo(42f, 29.5f); lineTo(46f, 33f); lineTo(50f, 29.5f); lineTo(54f, 33f); lineTo(58f, 29.5f); lineTo(62f, 33f)
        }, AlPink, style = Stroke(2f, cap = StrokeCap.Round))
        for ((x, y) in listOf(33f to 45f, 67f to 45f, 35.5f to 50f, 64.5f to 50f)) drawCircle(AlYellow, 1.6f, Offset(x, y))
        part(AlMint, 50f, 52f, 9f, 6.5f)
        mouth(p, 50f, 55f, 3f)
        part(AlPink, 50f, 49.5f, 3.4f, 2.4f)
        eyes(p, 41f, 59f, 42f)
        cheeks(p, 35f, 65f, 49f)
    }
}

// Perrito: café claro con orejas caídas, una mancha en el ojo y la cola corta que se menea.
private val Dog = Color(0xFFD9A066)
private val DogLight = Color(0xFFF3DDBF)
private val DogDark = Color(0xFF8B5A2B)

internal fun DrawScope.perro(p: Pose) {
    rotate(p.sway * 1.4f, pivot = Offset(66f, 80f)) {
        inkedPath(Path().apply { moveTo(66f, 80f); quadraticTo(76f, 74f, 78f, 64f) }, Dog, 5f)
    }
    for (x in listOf(40f, 60f)) part(DogLight, x, 90f, 6.5f, 3.8f)
    part(Dog, 50f, 72f, 20f, 19f, shine = true)
    part(DogLight, 50f, 77f, 11f, 11f, outline = false)
    part(DogLight, 43f, 85f, 4.2f, 4f)
    part(DogLight, 57f, 85f, 4.2f, 4f)
    head(p) {
        part(Dog, 50f, 44f, 21f, 18f, shine = true)
        part(DogDark.copy(alpha = 0.8f), 59f, 42f, 6.5f, 6f, outline = false)
        for ((x, left) in listOf(30f to true, 70f to false)) {
            rotate((if (left) -p.earLeft else p.earRight) * 0.7f + (if (left) 12f else -12f), pivot = Offset(if (left) 34f else 66f, 32f)) {
                part(DogDark, x, 43f, 6.5f, 12f)
            }
        }
        part(DogLight, 50f, 52f, 9.5f, 7f)
        part(Ink, 50f, 49f, 3.4f, 2.4f, outline = false)
        mouth(p, 50f, 54.5f, 3f)
        if (p.glad) drawOval(Color(0xFFFF7A9C), Offset(48.2f, 55.5f), Size(3.6f, 4.5f))
        eyes(p, 41f, 59f, 43f)
        cheeks(p, 35f, 65f, 50f)
    }
}

/**
 * Huevo que se mece y a ratos tiembla, como si algo quisiera salir.
 * Crema con manchas, o [alebrije]: turquesa con zigzag y manchas de colores (el de los betatesters).
 */
internal fun DrawScope.egg(phase: Float, alebrije: Boolean = false, crack: Float = 0f) {
    val tau = 2 * PI.toFloat()
    val shake = (if (phase in 0.7f..0.85f) 6f * sin(tau * 6f * (phase - 0.7f) / 0.15f) else 0f) + crack * 9f * sin(crack * 70f)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(30f, 88f), Size(40f, 7f))
    rotate(6f * sin(tau * phase) + shake, pivot = Offset(50f, 90f)) {
        val shell = Path().apply {
            moveTo(50f, 18f)
            cubicTo(70f, 18f, 76f, 58f, 74f, 70f)
            cubicTo(72f, 86f, 60f, 92f, 50f, 92f)
            cubicTo(40f, 92f, 28f, 86f, 26f, 70f)
            cubicTo(24f, 58f, 30f, 18f, 50f, 18f)
            close()
        }
        filledPath(shell, if (alebrije) AlTeal else Color(0xFFFFF4E0))
        val spots = if (alebrije) listOf(AlPink, AlYellow, AlPurple, AlOrange, AlYellow) else List(5) { Color(0xFF7CC6B4) }
        for ((i, s) in listOf(Triple(42f, 40f, 4f), Triple(60f, 52f, 5f), Triple(46f, 70f, 3.5f), Triple(62f, 76f, 2.6f), Triple(36f, 58f, 2.6f)).withIndex()) {
            drawCircle(spots[i], s.third, Offset(s.first, s.second))
        }
        if (alebrije) {
            drawPath(Path().apply { moveTo(29f, 62f); listOf(35f, 41f, 47f, 53f, 59f, 65f, 71f).forEachIndexed { k, x -> lineTo(x, if (k % 2 == 0) 57f else 62f) } },
                AlPink, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(Path().apply { moveTo(36f, 30f); listOf(41f, 46f, 51f, 56f, 61f, 64f).forEachIndexed { k, x -> lineTo(x, if (k % 2 == 0) 26f else 30f) } },
                AlMint, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawOval(Color.White.copy(alpha = 0.5f), Offset(34f, 30f), Size(8f, 14f))
        // La grieta avanza de izquierda a derecha mientras [crack] va de 0 a 0.7.
        if (crack > 0f) {
            val reach = (crack / 0.7f).coerceAtMost(1f)
            val xs = listOf(27f, 33f, 39f, 45f, 51f, 57f, 63f, 69f, 73f)
            drawPath(Path().apply {
                moveTo(xs[0], 46f)
                for (k in 1 until xs.size) {
                    val t = k / (xs.size - 1f)
                    if (t > reach) break
                    lineTo(xs[k], if (k % 2 == 0) 46f else 40f)
                }
            }, Ink, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
    // Al final, un destello que tapa el cambio de huevo a mascota.
    if (crack > 0.7f) {
        val t = (crack - 0.7f) / 0.3f
        drawCircle(Color.White.copy(alpha = t), 20f + 60f * t, Offset(50f, 55f))
    }
}
