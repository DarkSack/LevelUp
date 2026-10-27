package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** Foto del progreso de la que dependen logros y misiones. Se calcula, no se guarda. */
data class Stats(
    val totalCompleted: Int = 0,
    val byCategory: Map<String, Int> = emptyMap(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weeksGoalMet: Int = 0,
    val level: Int = 1,
    val customCreated: Int = 0,
    val simplifiedCompleted: Int = 0,
    val restConfigured: Boolean = false,
    /** Veces que se volvió a completar un reto tras 7 días o más sin actividad. */
    val comebacks: Int = 0,
    /** Máximo de semanas distintas en que se cumplió un mismo hábito. */
    val habitBestWeeks: Int = 0,
    val missionsCompleted: Int = 0,
    /** Días desde el primer reto completado (0 sin ninguno). */
    val daysSinceFirst: Int = 0,
) {
    val distinctCategories: Int get() = byCategory.count { it.value > 0 }

    companion object {
        const val COMEBACK_GAP_DAYS = 7

        fun compute(
            today: LocalDate,
            completed: List<ChallengeInstance>,
            totalXp: Int,
            prefs: Preferences,
            pauses: List<Pause>,
            customCreated: Int,
            missionsCompleted: Int,
        ): Stats {
            val done = completed.filter { it.isDone }
            val active = done.mapTo(HashSet()) { it.date }
            val dates = active.sorted()
            val comebacks = dates.zipWithNext().count { (a, b) -> a.daysUntil(b) >= COMEBACK_GAP_DAYS }
            val habitBestWeeks = done.filter { it.isHabit }
                .groupBy { it.challengeId }
                .maxOfOrNull { (_, list) -> list.map { Streaks.weekStart(it.date) }.distinct().size } ?: 0
            return Stats(
                totalCompleted = done.size,
                byCategory = done.groupingBy { it.category }.eachCount(),
                currentStreak = Streaks.current(today, active, prefs.restDays, pauses),
                bestStreak = Streaks.best(today, active, prefs.restDays, pauses),
                weeksGoalMet = Streaks.weeks(today, active, prefs.weeklyGoalDays).count { it.met },
                level = XpRules.levelFor(totalXp),
                customCreated = customCreated,
                simplifiedCompleted = done.count { it.simplified },
                restConfigured = prefs.restDays != 0 || pauses.isNotEmpty(),
                comebacks = comebacks,
                habitBestWeeks = habitBestWeeks,
                missionsCompleted = missionsCompleted,
                daysSinceFirst = dates.firstOrNull()?.daysUntil(today) ?: 0,
            )
        }
    }
}

enum class AchievementState { LOCKED, AVAILABLE, UNLOCKED }
enum class TrophyTier { BRONZE, SILVER, GOLD, PLATINUM }

/**
 * Un logro se cumple cuando `metric(stats) >= target`. Se evalúa con eventos de dominio (completar, crear un
 * reto, configurar descanso…), nunca desde la pantalla. `requires` encadena series: el segundo nivel queda
 * bloqueado hasta tener el primero. `hidden`: logro secreto, no se ve su nombre ni cómo conseguirlo hasta tenerlo.
 */
class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val collection: String,
    val tier: TrophyTier,
    val target: Int,
    val requires: String? = null,
    val xpReward: Int = 0,
    val hidden: Boolean = false,
    val metric: (Stats) -> Int,
) {
    fun progress(stats: Stats): Int = metric(stats).coerceIn(0, target)
}

object AchievementRules {
    fun state(a: Achievement, unlocked: Set<String>): AchievementState = when {
        a.id in unlocked -> AchievementState.UNLOCKED
        a.requires != null && a.requires !in unlocked -> AchievementState.LOCKED
        else -> AchievementState.AVAILABLE
    }

    /**
     * Logros que se desbloquean ahora. Repite hasta que no cambie nada: un logro da XP, eso puede subir el
     * nivel, y el siguiente de una serie puede cumplirse en la misma pasada.
     */
    fun newlyUnlocked(all: List<Achievement>, stats: Stats, totalXp: Int, unlocked: Set<String>): List<Achievement> {
        val got = unlocked.toMutableSet()
        val result = mutableListOf<Achievement>()
        var bonus = 0
        while (true) {
            val s = stats.copy(level = XpRules.levelFor(totalXp + bonus))
            val now = all.filter { a ->
                a.id !in got && (a.requires == null || a.requires in got) && a.metric(s) >= a.target
            }
            if (now.isEmpty()) return result
            now.forEach { got += it.id; bonus += it.xpReward }
            result += now
        }
    }
}

sealed interface MissionGoal {
    val target: Int

    data class Completions(override val target: Int) : MissionGoal
    data class Categories(override val target: Int) : MissionGoal
    data class InCategory(val category: String, override val target: Int) : MissionGoal
    data class ActiveDays(override val target: Int) : MissionGoal
}

/** Misión especial: opcional, con plazo y sin castigo si vence (se puede volver a empezar). */
data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val days: Int,
    val goal: MissionGoal,
    val xpReward: Int,
)

object MissionRules {
    fun endsOn(mission: Mission, startedOn: LocalDate): LocalDate = startedOn.plus(mission.days - 1, DateTimeUnit.DAY)

    fun progress(mission: Mission, run: MissionRun, instances: List<ChallengeInstance>): Int {
        val done = instances.filter { it.isDone && it.date in run.startedOn..run.endsOn }
        val value = when (val g = mission.goal) {
            is MissionGoal.Completions -> done.size
            is MissionGoal.Categories -> done.map { it.category }.distinct().size
            is MissionGoal.InCategory -> done.count { it.category == g.category }
            is MissionGoal.ActiveDays -> done.map { it.date }.distinct().size
        }
        return value.coerceAtMost(mission.goal.target)
    }

    fun isComplete(mission: Mission, run: MissionRun, instances: List<ChallengeInstance>): Boolean =
        progress(mission, run, instances) >= mission.goal.target

    fun isExpired(run: MissionRun, today: LocalDate): Boolean = run.status == MissionStatus.ACTIVE && today > run.endsOn
}

/** Recompensa cosmética: marcos para el avatar del perfil. Se desbloquean con logros o misiones. */
data class Cosmetic(
    val id: String,
    val name: String,
    /** Colores ARGB del marco. */
    val colors: List<Long>,
    /** ID del logro o misión que lo da; null = disponible desde el inicio. */
    val unlockedBy: String? = null,
)
