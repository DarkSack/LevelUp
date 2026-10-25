package app.levelup.domain

/** Lo que el usuario escribe en el formulario de un reto propio. */
data class CustomChallengeDraft(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val goal: String = "",
    val doneWhen: String = "",
    val difficulty: Int = 2,
    val minutes: Int = 10,
    val frequency: Frequency = Frequency.DAILY,
    /** Días fijos (máscara de [Days]). Distinto de 0 = es un hábito. */
    val habitDays: Int = 0,
    val steps: List<String> = emptyList(),
    /** Lugar y «sin materiales» (claves de content/Tags.kt). */
    val tags: List<String> = emptyList(),
    /** La versión más pequeña que también cuenta. */
    val minimum: String = "",
    val block: DayBlock? = null,
)

enum class DraftField { TITLE, DESCRIPTION, CATEGORY, GOAL, DONE_WHEN, DIFFICULTY, MINUTES, STEPS, MINIMUM }

object CustomChallengeRules {
    const val TITLE_MAX = 60
    const val TEXT_MAX = 300
    const val GOAL_MAX = 140
    const val MAX_STEPS = 10
    const val STEP_MAX = 80

    fun normalize(title: String): String = title.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Errores por campo, con el texto que ve el usuario. Vacío = válido. */
    fun validate(
        draft: CustomChallengeDraft,
        validCategories: Set<String>,
        otherTitles: Collection<String>,
    ): Map<DraftField, String> {
        val errors = mutableMapOf<DraftField, String>()
        val title = draft.title.trim()
        when {
            title.length < 3 -> errors[DraftField.TITLE] = "Escribe un título de al menos 3 letras."
            title.length > TITLE_MAX -> errors[DraftField.TITLE] = "El título puede tener hasta $TITLE_MAX caracteres."
            otherTitles.any { normalize(it) == normalize(title) } -> errors[DraftField.TITLE] = "Ya tienes un reto con ese título."
        }
        if (draft.description.trim().length > TEXT_MAX) {
            errors[DraftField.DESCRIPTION] = "La descripción puede tener hasta $TEXT_MAX caracteres."
        }
        if (draft.category !in validCategories) errors[DraftField.CATEGORY] = "Elige una categoría."
        val goal = draft.goal.trim()
        when {
            goal.length < 3 -> errors[DraftField.GOAL] = "Describe qué hay que hacer (por ejemplo: «Leer 10 páginas»)."
            goal.length > GOAL_MAX -> errors[DraftField.GOAL] = "El objetivo puede tener hasta $GOAL_MAX caracteres."
        }
        if (draft.doneWhen.trim().length > GOAL_MAX) {
            errors[DraftField.DONE_WHEN] = "Puede tener hasta $GOAL_MAX caracteres."
        }
        if (draft.minimum.trim().length > GOAL_MAX) {
            errors[DraftField.MINIMUM] = "La versión mínima puede tener hasta $GOAL_MAX caracteres."
        }
        if (draft.difficulty !in 1..5) errors[DraftField.DIFFICULTY] = "Elige una dificultad del 1 al 5."
        if (draft.minutes !in 1..240) errors[DraftField.MINUTES] = "La duración va de 1 a 240 minutos."
        val steps = draft.steps.map { it.trim() }.filter { it.isNotEmpty() }
        when {
            steps.size > MAX_STEPS -> errors[DraftField.STEPS] = "Hasta $MAX_STEPS pasos."
            steps.any { it.length > STEP_MAX } -> errors[DraftField.STEPS] = "Cada paso puede tener hasta $STEP_MAX caracteres."
        }
        return errors
    }
}
