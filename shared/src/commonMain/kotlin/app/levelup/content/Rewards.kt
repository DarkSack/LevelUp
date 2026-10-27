package app.levelup.content

import app.levelup.domain.Achievement
import app.levelup.domain.Cosmetic
import app.levelup.domain.Mission
import app.levelup.domain.MissionGoal
import app.levelup.domain.TrophyTier.BRONZE
import app.levelup.domain.TrophyTier.GOLD
import app.levelup.domain.TrophyTier.PLATINUM
import app.levelup.domain.TrophyTier.SILVER

/**
 * Logros (cada uno es un trofeo de su colección), misiones y marcos cosméticos.
 * Ninguno premia excesos: no hay logros por hacer muchos retos en un día ni por interactuar con otras personas.
 */
object AchievementCatalog {
    private const val FIRST = "Primeros pasos"
    private const val RHYTHM = "Constancia"
    private const val EXPLORE = "Exploración"
    private const val GROWTH = "Crecimiento"
    private const val OWN_WAY = "A tu manera"
    private const val AFFINITY = "Afinidades"
    private const val SECRET = "Secretos"

    private val base = listOf(
        Achievement("first_step", "Primer paso", "Completa tu primer reto.", FIRST, BRONZE, 1,
            xpReward = 10) { it.totalCompleted },
        Achievement("steps_10", "Diez pasos", "Completa 10 retos.", FIRST, BRONZE, 10,
            requires = "first_step", xpReward = 20) { it.totalCompleted },
        Achievement("steps_50", "Camino largo", "Completa 50 retos.", FIRST, SILVER, 50,
            requires = "steps_10", xpReward = 40) { it.totalCompleted },
        Achievement("steps_100", "Cien pasos", "Completa 100 retos.", FIRST, GOLD, 100,
            requires = "steps_50", xpReward = 60) { it.totalCompleted },
        Achievement("steps_250", "Sendero propio", "Completa 250 retos.", FIRST, PLATINUM, 250,
            requires = "steps_100", xpReward = 100) { it.totalCompleted },

        Achievement("streak_3", "Tres días", "Completa retos tres días seguidos. Los días de descanso no cortan.",
            RHYTHM, BRONZE, 3, xpReward = 15) { it.bestStreak },
        Achievement("streak_7", "Una semana", "Completa retos siete días seguidos.", RHYTHM, SILVER, 7,
            requires = "streak_3", xpReward = 30) { it.bestStreak },
        Achievement("streak_30", "Un mes de ritmo", "Completa retos treinta días seguidos.", RHYTHM, GOLD, 30,
            requires = "streak_7", xpReward = 60) { it.bestStreak },
        Achievement("streak_100", "Cien días de ritmo", "Completa retos cien días seguidos. Los días de descanso tampoco cortan.",
            RHYTHM, PLATINUM, 100, requires = "streak_30", xpReward = 100) { it.bestStreak },
        Achievement("week_1", "Semana cumplida", "Cumple tu objetivo semanal.", RHYTHM, BRONZE, 1,
            xpReward = 15) { it.weeksGoalMet },
        Achievement("week_4", "Cuatro semanas", "Cumple tu objetivo semanal en 4 semanas, aunque no sean seguidas.",
            RHYTHM, SILVER, 4, requires = "week_1", xpReward = 30) { it.weeksGoalMet },
        Achievement("week_12", "Temporada completa", "Cumple tu objetivo semanal en 12 semanas.", RHYTHM, GOLD, 12,
            requires = "week_4", xpReward = 60) { it.weeksGoalMet },
        Achievement("week_52", "Un año a tu ritmo", "Cumple tu objetivo semanal en 52 semanas, aunque no sean seguidas.",
            RHYTHM, PLATINUM, 52, requires = "week_12", xpReward = 100) { it.weeksGoalMet },

        Achievement("explore_3", "Curiosidad", "Completa retos de 3 categorías distintas.", EXPLORE, BRONZE, 3,
            xpReward = 15) { it.distinctCategories },
        Achievement("explore_6", "Mapa amplio", "Completa retos de 6 categorías distintas.", EXPLORE, SILVER, 6,
            requires = "explore_3", xpReward = 30) { it.distinctCategories },
        Achievement("explore_9", "Todas las rutas", "Completa retos de 9 categorías distintas.", EXPLORE, GOLD, 9,
            requires = "explore_6", xpReward = 60) { it.distinctCategories },

        Achievement("level_5", "Nivel 5", "Llega al nivel 5.", GROWTH, BRONZE, 5, xpReward = 20) { it.level },
        Achievement("level_10", "Nivel 10", "Llega al nivel 10.", GROWTH, SILVER, 10,
            requires = "level_5", xpReward = 40) { it.level },
        Achievement("level_20", "Nivel 20", "Llega al nivel 20.", GROWTH, GOLD, 20,
            requires = "level_10", xpReward = 80) { it.level },
        Achievement("level_30", "Nivel 30", "Llega al nivel 30.", GROWTH, PLATINUM, 30,
            requires = "level_20", xpReward = 100) { it.level },

        Achievement("custom_1", "Reto propio", "Crea tu primer reto personalizado.", OWN_WAY, BRONZE, 1,
            xpReward = 10) { it.customCreated },
        Achievement("custom_5", "Diseño propio", "Crea 5 retos personalizados.", OWN_WAY, SILVER, 5,
            requires = "custom_1", xpReward = 25) { it.customCreated },
        Achievement("simplify_1", "Paso a paso", "Completa una versión simplificada de un reto. Adaptar también es avanzar.",
            OWN_WAY, BRONZE, 1, xpReward = 10) { it.simplifiedCompleted },
        Achievement("rest_1", "Descanso merecido", "Configura días de descanso o una pausa.", OWN_WAY, BRONZE, 1,
            xpReward = 10) { if (it.restConfigured) 1 else 0 },
        Achievement("comeback_1", "De vuelta", "Completa un reto después de 7 días o más sin actividad.", OWN_WAY, BRONZE, 1,
            xpReward = 20) { it.comebacks },
        Achievement("habit_4", "Hábito en marcha", "Cumple un mismo hábito en 4 semanas distintas.", OWN_WAY, SILVER, 4,
            xpReward = 40) { it.habitBestWeeks },
        Achievement("habit_12", "Hábito de temporada", "Cumple un mismo hábito en 12 semanas distintas.", OWN_WAY, GOLD, 12,
            requires = "habit_4", xpReward = 60) { it.habitBestWeeks },
        Achievement("mission_1", "Primera misión", "Completa una misión especial.", OWN_WAY, BRONZE, 1,
            xpReward = 15) { it.missionsCompleted },
        Achievement("mission_5", "Misiones cumplidas", "Completa 5 misiones especiales.", OWN_WAY, SILVER, 5,
            requires = "mission_1", xpReward = 40) { it.missionsCompleted },
        Achievement("mission_15", "Coleccionista de misiones", "Completa 15 misiones especiales.", OWN_WAY, GOLD, 15,
            requires = "mission_5", xpReward = 60) { it.missionsCompleted },

        // Secretos: se descubren al conseguirlos. Premian volver, adaptar y variar, nunca hacer más.
        Achievement("secret_comeback_3", "Volver siempre", "Vuelve tres veces tras una semana o más sin actividad. Volver también es constancia.",
            SECRET, SILVER, 3, xpReward = 30, hidden = true) { it.comebacks },
        Achievement("secret_simplify_10", "Ajuste fino", "Completa 10 versiones simplificadas. Adaptar es avanzar.",
            SECRET, SILVER, 10, xpReward = 30, hidden = true) { it.simplifiedCompleted },
        Achievement("secret_balance_5", "Equilibrio", "Completa al menos 5 retos en 5 categorías distintas.",
            SECRET, SILVER, 5, xpReward = 30, hidden = true) { s -> s.byCategory.count { it.value >= 5 } },
        Achievement("secret_year", "Un año contigo", "Sigue con LevelUp un año después de tu primer reto, con pausas o sin ellas.",
            SECRET, GOLD, 365, xpReward = 60, hidden = true) { it.daysSinceFirst },
    )

    private val affinities = Categories.builtIn.flatMap { cat ->
        listOf(
            Achievement("affinity_${cat.key}", "Afinidad: ${cat.name}", "Completa 10 retos de ${cat.name.lowercase()}.",
                AFFINITY, BRONZE, 10, xpReward = 20) { it.byCategory[cat.key] ?: 0 },
            Achievement("mastery_${cat.key}", "Dominio: ${cat.name}", "Completa 30 retos de ${cat.name.lowercase()}.",
                AFFINITY, SILVER, 30, requires = "affinity_${cat.key}", xpReward = 40) { it.byCategory[cat.key] ?: 0 },
        )
    }

    private val masteryAll = Achievement("mastery_all", "Maestría completa", "Llega a Dominio en las 9 categorías.",
        AFFINITY, PLATINUM, Categories.builtIn.size, xpReward = 100) { s ->
        Categories.builtIn.count { (s.byCategory[it.key] ?: 0) >= 30 }
    }

    // Los secretos al final: su colección es la última de la pantalla.
    val all: List<Achievement> = base.filter { it.collection != SECRET } + affinities + masteryAll + base.filter { it.collection == SECRET }
    val byId: Map<String, Achievement> = all.associateBy { it.id }
    val collections: List<String> = all.map { it.collection }.distinct()
}

object MissionCatalog {
    val all = listOf(
        Mission("mis.exploracion", "Semana de exploración", "Completa retos de 3 categorías distintas en 7 días.",
            7, MissionGoal.Categories(3), xpReward = 50),
        Mission("mis.ritmo", "Cinco días de ritmo", "Completa al menos un reto en 5 días distintos dentro de 10 días.",
            10, MissionGoal.ActiveDays(5), xpReward = 60),
        Mission("mis.cuerpo", "Cuerpo en movimiento", "Completa 4 retos físicos en 7 días.",
            7, MissionGoal.InCategory(Categories.FISICO, 4), xpReward = 50),
        Mission("mis.mente", "Mente curiosa", "Completa 4 retos de mente y aprendizaje en 7 días.",
            7, MissionGoal.InCategory(Categories.MENTAL, 4), xpReward = 50),
        Mission("mis.conexiones", "Conexiones", "Completa 3 retos sociales en 10 días.",
            10, MissionGoal.InCategory(Categories.SOCIAL, 3), xpReward = 50),
        Mission("mis.hogar", "Hogar en orden", "Completa 3 retos de hogar en 7 días.",
            7, MissionGoal.InCategory(Categories.HOGAR, 3), xpReward = 40),
        Mission("mis.calma", "Pausa consciente", "Completa 4 retos de bienestar en 7 días.",
            7, MissionGoal.InCategory(Categories.BIENESTAR, 4), xpReward = 50),
        Mission("mis.maraton", "Maratón tranquilo", "Completa 15 retos en 14 días, a tu ritmo.",
            14, MissionGoal.Completions(15), xpReward = 100),
    )
    val byId = all.associateBy { it.id }
}

object Cosmetics {
    const val DEFAULT = "frame.basic"

    /** `unlockedBy` de los marcos que trae LevelUp Plus. */
    const val PLUS = "plus"

    val frames = listOf(
        Cosmetic(DEFAULT, "Básico", listOf(0xFF0F766E)),
        Cosmetic("frame.bronze", "Bronce", listOf(0xFFB07A3B, 0xFFE0B07A), "first_step"),
        Cosmetic("frame.silver", "Plata", listOf(0xFF7D868E, 0xFFD5DBE0), "steps_50"),
        Cosmetic("frame.gold", "Oro", listOf(0xFFB8860B, 0xFFF2CC5B), "steps_100"),
        Cosmetic("frame.wave", "Ola", listOf(0xFF2563EB, 0xFF38BDF8), "streak_7"),
        Cosmetic("frame.calendar", "Temporada", listOf(0xFF7C3AED, 0xFFC084FC), "week_12"),
        Cosmetic("frame.sprout", "Brote", listOf(0xFF15803D, 0xFF86EFAC), "explore_3"),
        Cosmetic("frame.compass", "Brújula", listOf(0xFF0E7490, 0xFFF59E0B), "explore_9"),
        Cosmetic("frame.star", "Estrella", listOf(0xFFD97706, 0xFFFDE68A), "level_10"),
        Cosmetic("frame.aurora", "Aurora", listOf(0xFF0EA5E9, 0xFFA855F7, 0xFF22C55E), "level_20"),
        Cosmetic("frame.moon", "Luna", listOf(0xFF334155, 0xFF94A3B8), "rest_1"),
        Cosmetic("frame.leaf", "Hoja", listOf(0xFF4D7C0F, 0xFFBEF264), "habit_4"),
        Cosmetic("frame.prism", "Prisma", listOf(0xFFDB2777, 0xFF6366F1), "mis.exploracion"),
        Cosmetic("frame.ember", "Brasa", listOf(0xFFDC2626, 0xFFF97316), "mis.ritmo"),
        Cosmetic("frame.laurel", "Laurel", listOf(0xFF166534, 0xFFCA8A04), "mis.maraton"),
        Cosmetic("frame.platinum", "Platino", listOf(0xFF475569, 0xFFE2E8F0, 0xFFBAE6FD), "steps_250"),
        Cosmetic("frame.year", "Año", listOf(0xFF0F766E, 0xFFFACC15, 0xFFF472B6), "week_52"),
        Cosmetic("frame.secret", "Secreto", listOf(0xFF1E1B4B, 0xFF6D28D9, 0xFF22D3EE), "secret_year"),
        // LevelUp Plus: solo decoración, nunca ventajas.
        Cosmetic("frame.plus.galaxy", "Galaxia", listOf(0xFF312E81, 0xFF7C3AED, 0xFF0EA5E9), PLUS),
        Cosmetic("frame.plus.coral", "Coral", listOf(0xFFFB7185, 0xFFFDBA74), PLUS),
        Cosmetic("frame.plus.forest", "Bosque", listOf(0xFF166534, 0xFF84CC16, 0xFF14B8A6), PLUS),
        Cosmetic("frame.plus.dawn", "Amanecer", listOf(0xFFF59E0B, 0xFFEC4899, 0xFF8B5CF6), PLUS),
    )
    val byId = frames.associateBy { it.id }

    /** Marco que da un logro o una misión, si da alguno. */
    fun rewardOf(sourceId: String): Cosmetic? = frames.firstOrNull { it.unlockedBy == sourceId }

    /** Marcos disponibles según logros desbloqueados, misiones completadas y LevelUp Plus. */
    fun unlocked(achievements: Set<String>, missions: Set<String>, plus: Boolean = false): List<Cosmetic> =
        frames.filter {
            it.unlockedBy == null || it.unlockedBy in achievements || it.unlockedBy in missions || (plus && it.unlockedBy == PLUS)
        }
}
