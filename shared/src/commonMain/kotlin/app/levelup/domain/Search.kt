package app.levelup.domain

import kotlinx.datetime.LocalDate
import app.levelup.i18n.tr

enum class HitKind { CHALLENGE, HABIT, ACHIEVEMENT, GOAL, NOTE }

/** [id]: reto, logro, meta o instancia (en las notas), según [kind]. */
data class SearchHit(val kind: HitKind, val id: String, val title: String, val detail: String, val date: LocalDate? = null)

/** Búsqueda en todo lo que la app guarda en el teléfono. Sin acentos ni mayúsculas: «medita» encuentra «Meditá». */
object Search {
    const val MIN_QUERY = 2
    const val LIMIT_PER_KIND = 20

    fun normalize(text: String): String = buildString(text.length) {
        for (c in text.lowercase()) {
            append(
                when (c) {
                    'á', 'à', 'ä', 'â' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    'ñ' -> 'n'
                    else -> c
                },
            )
        }
    }

    fun run(
        query: String,
        challenges: List<Challenge>,
        achievements: List<Achievement>,
        goals: List<PersonalGoal>,
        notes: List<ChallengeInstance>,
    ): List<SearchHit> {
        val q = normalize(query.trim())
        if (q.length < MIN_QUERY) return emptyList()
        // También en el idioma de la app: quien la usa en inglés busca "walk", no "caminar".
        fun matches(vararg fields: String) = fields.any { normalize(it).contains(q) || normalize(tr(it)).contains(q) }

        val (habits, others) = challenges.filter { matches(it.title, it.description, it.goal) }.partition { it.isHabit }
        return buildList {
            others.take(LIMIT_PER_KIND).forEach { add(SearchHit(HitKind.CHALLENGE, it.id, it.title, it.goal)) }
            habits.take(LIMIT_PER_KIND).forEach { add(SearchHit(HitKind.HABIT, it.id, it.title, it.goal)) }
            achievements.filter { matches(it.name, it.description) }.take(LIMIT_PER_KIND)
                .forEach { add(SearchHit(HitKind.ACHIEVEMENT, it.id, it.name, it.description)) }
            goals.filter { matches(it.title, it.why) }.take(LIMIT_PER_KIND)
                .forEach { add(SearchHit(HitKind.GOAL, it.id, it.title, it.why)) }
            notes.filter { it.note != null && matches(it.note) }.sortedByDescending { it.date }.take(LIMIT_PER_KIND)
                .forEach { add(SearchHit(HitKind.NOTE, it.id, it.title, it.note.orEmpty(), it.date)) }
        }
    }
}

/** Lo que muestran el widget y el reloj: el reto principal y cuántos van. */
data class TodaySummary(
    val date: LocalDate,
    val mainId: String?,
    val mainTitle: String?,
    val done: Int,
    val total: Int,
    val items: List<Item>,
    val goals: List<String>,
) {
    data class Item(val id: String, val title: String, val done: Boolean)

    companion object {
        fun from(date: LocalDate, instances: List<ChallengeInstance>, goals: List<PersonalGoal> = emptyList()): TodaySummary {
            val visible = instances.filter { it.date == date && it.status != InstanceStatus.REPLACED }.sortedBy { it.slot }
            val main = visible.firstOrNull { it.isOpen }
            return TodaySummary(
                date = date,
                mainId = main?.id,
                mainTitle = main?.title,
                done = visible.count { it.isDone },
                total = visible.size,
                items = visible.map { Item(it.id, it.title, it.isDone) },
                goals = goals.filter { it.status == GoalStatus.ACTIVE }.sortedBy { it.createdAt }.map { it.title },
            )
        }
    }
}
