package app.levelup.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlin.math.abs
import kotlin.random.Random

enum class DayKind { ACTIVE, OFF, REST, PAUSED }

/** Elige los retos de cada día. Puro: lo mismo de entrada da lo mismo de salida con la misma semilla. */
object ChallengeEngine {
    /** Minutos que se reservan por cada reto que aún falta elegir. */
    const val RESERVE_MINUTES = 5

    data class Input(
        val today: LocalDate,
        val prefs: Preferences,
        /** Catálogo + retos propios activos. */
        val challenges: List<Challenge>,
        /** Instancias de los últimos ~30 días, hoy incluido. */
        val history: List<ChallengeInstance>,
        /** IDs de retos completados alguna vez (requisitos y retos de una sola vez). */
        val completedEver: Set<String>,
    )

    fun dayKind(day: LocalDate, prefs: Preferences, pauses: List<Pause>): DayKind = when {
        pauses.any { it.covers(day) } -> DayKind.PAUSED
        Days.has(prefs.restDays, day.dayOfWeek) -> DayKind.REST
        !Days.has(prefs.challengeDays, day.dayOfWeek) -> DayKind.OFF
        else -> DayKind.ACTIVE
    }

    /** Hábitos propios que tocan ese día. */
    fun habitsFor(day: LocalDate, challenges: List<Challenge>): List<Challenge> =
        challenges.filter { it.isHabit && Days.has(it.habitDays, day.dayOfWeek) }

    /**
     * Hasta [count] retos que quepan en el tiempo diario del usuario. El primero es el reto principal.
     * Si no hay suficientes, relaja las reglas por etapas (repetir recientes, ampliar dificultad) antes que devolver nada.
     */
    fun dailyPicks(
        input: Input,
        random: Random,
        count: Int = input.prefs.dailyCount,
        exclude: Set<String> = emptySet(),
    ): List<Challenge> {
        val picks = mutableListOf<Challenge>()
        var budget = input.prefs.minutesPerDay
        for (relax in 0..2) {
            while (picks.size < count) {
                val taken = exclude + picks.map { it.id }
                val usedCategories = picks.map { it.category }.toSet()
                // Deja tiempo para los retos que faltan, así uno largo no se come todo el día.
                val remaining = count - picks.size
                val cap = maxOf(budget - RESERVE_MINUTES * (remaining - 1), budget / remaining)
                val pool = input.challenges.filter { it.minutes <= cap && eligible(it, input, relax, taken) }
                if (pool.isEmpty()) break
                val chosen = weightedPick(pool, random) {
                    weight(it, input) * if (it.category in usedCategories) 0.25 else 1.0
                }
                picks += chosen
                budget -= chosen.minutes
            }
            if (picks.size >= count) break
        }
        if (picks.isEmpty()) {
            // Nada cabe en el tiempo elegido: mejor un reto corto que un día vacío.
            input.challenges.filter { eligible(it, input, 2, exclude) }.minByOrNull { it.minutes }?.let { picks += it }
        }
        val main = picks.firstOrNull { it.category in input.prefs.priorityCategories } ?: return picks
        return listOf(main) + (picks - main)
    }

    /** Otro reto en lugar de [current]: misma categoría si se puede, nunca más difícil ni más largo. */
    fun replacementFor(input: Input, current: ChallengeInstance, todayIds: Set<String>, random: Random): Challenge? {
        val exclude = todayIds + current.challengeId + listOfNotNull(current.originalChallengeId)
        for (relax in 0..2) {
            val pool = input.challenges.filter {
                it.difficulty <= current.difficulty && it.minutes <= maxOf(current.minutes, 5) &&
                    eligible(it, input, relax, exclude)
            }
            val sameCategory = pool.filter { it.category == current.category }
            val options = sameCategory.ifEmpty { pool }
            if (options.isNotEmpty()) return weightedPick(options, random) { weight(it, input) }
        }
        return null
    }

    private fun eligible(c: Challenge, input: Input, relax: Int, exclude: Set<String>): Boolean {
        val prefs = input.prefs
        if (c.id in exclude || c.isHabit) return false
        if (prefs.categories.isNotEmpty() && c.category !in prefs.categories) return false
        if (c.availableFrom != null && c.availableFrom > input.today) return false
        if (c.requires != null && c.requires !in input.completedEver) return false
        when (c.frequency) {
            Frequency.ONCE -> if (c.id in input.completedEver) return false
            Frequency.WEEKLY -> if (c.id in doneThisWeek(input)) return false
            Frequency.DAILY -> Unit
        }
        val target = prefs.difficultyFor(c.category)
        // Nunca más de un paso por encima de lo elegido; por debajo, se amplía si faltan retos.
        if (c.difficulty > target + 1) return false
        if (c.difficulty < target - if (relax >= 1) 2 else 1) return false
        if (relax == 0 && c.id in recentlyShown(input)) return false
        if (relax <= 1 && c.id in skippedOften(input)) return false
        return true
    }

    private fun weight(c: Challenge, input: Input): Double {
        val prefs = input.prefs
        var w = when (abs(c.difficulty - prefs.difficultyFor(c.category))) {
            0 -> 3.0
            1 -> 1.0
            else -> 0.4
        }
        if (c.category in prefs.priorityCategories) w *= 2
        if (c.source == ChallengeSource.CUSTOM) w *= 1.5
        if (c.id in doneRecently(input)) w *= 0.5
        return w
    }

    private fun recentlyShown(input: Input): Set<String> {
        val from = input.today.minus(3, DateTimeUnit.DAY)
        return input.history.filter { it.date >= from && it.date < input.today }.mapTo(HashSet()) { it.challengeId }
    }

    private fun doneRecently(input: Input): Set<String> {
        val from = input.today.minus(7, DateTimeUnit.DAY)
        return input.history.filter { it.isDone && it.date >= from }.mapTo(HashSet()) { it.challengeId }
    }

    private fun skippedOften(input: Input): Set<String> {
        val from = input.today.minus(14, DateTimeUnit.DAY)
        return input.history
            .filter { it.date >= from && (it.status == InstanceStatus.SKIPPED || it.status == InstanceStatus.REPLACED) }
            .groupingBy { it.challengeId }.eachCount()
            .filterValues { it >= 2 }.keys
    }

    private fun doneThisWeek(input: Input): Set<String> {
        val weekStart = input.today.minus(input.today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        return input.history.filter { it.isDone && it.date >= weekStart }.mapTo(HashSet()) { it.challengeId }
    }

    private fun <T> weightedPick(items: List<T>, random: Random, weight: (T) -> Double): T {
        val weights = items.map(weight)
        var r = random.nextDouble() * weights.sum()
        for (i in items.indices) {
            r -= weights[i]
            if (r <= 0) return items[i]
        }
        return items.last()
    }
}
