package app.levelup.domain

import kotlin.math.sqrt

/** Fórmulas de XP y niveles. Los números están explicados en docs/REGLAS.md. */
object XpRules {
    /** XP por completar un reto, según su dificultad (1 a 5). */
    val XP_BY_DIFFICULTY = listOf(10, 15, 25, 40, 60)

    /** XP total para llegar al nivel L: step × L × (L − 1). Nivel 2 = 100, 3 = 300, 4 = 600… */
    const val LEVEL_STEP = 50

    /**
     * XP por retomar: el primer reto completado tras [COMEBACK_XP_GAP_DAYS] días o más sin actividad. Dos semanas, no
     * una: así no compensa desaparecer cada semana para cobrarla. Los retos suaves de regreso empiezan antes (7 días).
     */
    const val COMEBACK_XP = 20
    const val COMEBACK_XP_GAP_DAYS = 14

    /** Lo mismo para el nivel de cada categoría, con la mitad de paso. */
    const val CATEGORY_STEP = 25

    fun xpFor(difficulty: Int): Int = XP_BY_DIFFICULTY[difficulty.coerceIn(1, 5) - 1]

    fun totalForLevel(level: Int, step: Int = LEVEL_STEP): Int = step * level * (level - 1)

    fun levelFor(xp: Int, step: Int = LEVEL_STEP): Int {
        if (xp <= 0) return 1
        var level = ((1 + sqrt(1 + 4.0 * xp / step)) / 2).toInt().coerceAtLeast(1)
        // Corrige el redondeo de la raíz en los bordes exactos.
        while (totalForLevel(level + 1, step) <= xp) level++
        while (level > 1 && totalForLevel(level, step) > xp) level--
        return level
    }

    data class LevelProgress(val level: Int, val xpInLevel: Int, val xpForNext: Int, val totalXp: Int) {
        val fraction: Float get() = if (xpForNext == 0) 0f else xpInLevel.toFloat() / xpForNext
    }

    fun progress(xp: Int, step: Int = LEVEL_STEP): LevelProgress {
        val total = xp.coerceAtLeast(0)
        val level = levelFor(total, step)
        val start = totalForLevel(level, step)
        return LevelProgress(level, total - start, totalForLevel(level + 1, step) - start, total)
    }

    /** Títulos por nivel global. Frases neutras: no miden el valor de nadie, solo el camino recorrido. */
    val TITLES = listOf(
        1 to "Primeros pasos",
        3 to "En marcha",
        5 to "Con ritmo",
        8 to "Constancia",
        12 to "Rumbo firme",
        16 to "Huella propia",
        20 to "Inspiración",
        30 to "Leyenda tranquila",
    )

    fun titleFor(level: Int): String = TITLES.last { level >= it.first }.second

    fun nextTitle(level: Int): Pair<Int, String>? = TITLES.firstOrNull { it.first > level }
}
