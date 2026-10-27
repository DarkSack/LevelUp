package app.levelup.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import app.levelup.content.AchievementCatalog
import app.levelup.content.Catalog
import app.levelup.content.Categories
import app.levelup.content.MissionCatalog
import app.levelup.domain.Achievement
import app.levelup.domain.AchievementRules
import app.levelup.domain.AdaptiveDifficulty
import app.levelup.domain.Challenge
import app.levelup.domain.ChallengeEngine
import app.levelup.domain.ChallengeInstance
import app.levelup.domain.CustomCategory
import app.levelup.domain.CustomChallenge
import app.levelup.domain.CustomChallengeDraft
import app.levelup.domain.CustomChallengeRules
import app.levelup.domain.CustomStatus
import app.levelup.content.Tags
import app.levelup.domain.DayBlock
import app.levelup.domain.DayKind
import app.levelup.domain.DifficultyMode
import app.levelup.domain.DraftField
import app.levelup.domain.Favorite
import app.levelup.domain.GoalRules
import app.levelup.domain.GoalStatus
import app.levelup.domain.NOTE_MAX
import app.levelup.domain.PersonalGoal
import app.levelup.domain.ReviewSuggestion
import app.levelup.domain.Search
import app.levelup.domain.SearchHit
import app.levelup.domain.TodaySummary
import app.levelup.domain.WeeklyReview
import app.levelup.domain.WeeklySummary
import app.levelup.domain.InstanceStatus
import app.levelup.domain.Mission
import app.levelup.domain.MissionRules
import app.levelup.domain.MissionRun
import app.levelup.domain.MissionStatus
import app.levelup.domain.Pause
import app.levelup.domain.Preferences
import app.levelup.domain.Rating
import app.levelup.domain.ReminderMessage
import app.levelup.domain.Reminders
import app.levelup.domain.Stats
import app.levelup.domain.UnlockedAchievement
import app.levelup.domain.XpEntry
import app.levelup.domain.XpRules
import app.levelup.domain.XpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import app.levelup.i18n.tr

data class SaveResult(val errors: Map<DraftField, String>, val unlocked: List<Achievement> = emptyList())

data class CompletionResult(
    val xpGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val achievements: List<Achievement>,
    val missions: List<Mission>,
    /** Primer reto tras una temporada sin actividad: suma [XpRules.COMEBACK_XP]. */
    val comeback: Boolean = false,
)

/**
 * Única puerta a los datos. Toda operación que cambia XP, logros o misiones va en una transacción:
 * o se guarda entera o no se guarda nada.
 */
class LevelUpRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.System,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val dao = db.dao()

    fun today(): LocalDate = clock.now().toLocalDateTime(zone()).date
    fun dateOf(epochMillis: Long): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone()).date
    fun nowLocal(): LocalDateTime = clock.now().toLocalDateTime(zone())
    private fun now(): Long = clock.now().toEpochMilliseconds()

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()

    // Ids deterministas: el mismo hecho en dos teléfonos es el mismo registro, así la sincronización
    // nunca suma dos veces la XP de un reto, una misión o un logro.
    private fun xpEntry(source: XpSource, sourceId: String, amount: Int, category: String?) =
        XpEntry("${source.name}:$sourceId", source, sourceId, amount, category, now())

    private suspend fun <R> tx(block: suspend () -> R): R = db.useWriterConnection { it.immediateTransaction { block() } }

    // ── Lecturas ──────────────────────────────────────────────────────────────

    val preferences: Flow<Preferences> = dao.preferences().map { it ?: Preferences() }
    val totalXp: Flow<Int> = dao.totalXp()
    val xpByCategory: Flow<Map<String, Int>> = dao.xpByCategory().map { list -> list.associate { it.category to it.xp } }
    val xpEntries: Flow<List<XpEntry>> = dao.xpEntries()
    val completed: Flow<List<ChallengeInstance>> = dao.completed()
    val unlocked: Flow<List<UnlockedAchievement>> = dao.unlocked()
    val missionRuns: Flow<List<MissionRun>> = dao.missionRuns()
    val pauses: Flow<List<Pause>> = dao.pauses()
    val customChallenges: Flow<List<CustomChallenge>> = dao.customChallenges()
    val customCategories: Flow<List<CustomCategory>> = dao.customCategories()
    val favorites: Flow<List<Favorite>> = dao.favorites()
    val goals: Flow<List<PersonalGoal>> = dao.goals()

    /** Hoy, para el widget y el reloj. Se recalcula con cada cambio de retos o metas. */
    val todaySummary: Flow<TodaySummary> =
        combine(dao.instancesSinceFlow(today().minus(1, DateTimeUnit.DAY)), dao.goals()) { list, goals ->
            TodaySummary.from(today(), list, goals)
        }

    fun instancesOn(date: LocalDate): Flow<List<ChallengeInstance>> = dao.instancesOn(date)
    fun instance(id: String): Flow<ChallengeInstance?> = dao.instanceFlow(id)
    fun recentInstances(days: Int): Flow<List<ChallengeInstance>> = dao.instancesSinceFlow(today().minus(days, DateTimeUnit.DAY))

    suspend fun prefs(): Preferences = dao.preferencesNow() ?: Preferences()

    /** Catálogo + retos propios activos. */
    suspend fun allChallenges(): List<Challenge> =
        Catalog.challenges + dao.customChallengesNow().filter { it.status == CustomStatus.ACTIVE }.map { it.toChallenge() }

    suspend fun challenge(id: String): Challenge? = Catalog.byId[id] ?: dao.customChallenge(id)?.toChallenge()

    // ── Preferencias ──────────────────────────────────────────────────────────

    suspend fun updatePreferences(change: (Preferences) -> Preferences): List<Achievement> = tx {
        dao.upsertPreferences(change(prefs()))
        awardAchievements()
    }

    // ── Retos del día ─────────────────────────────────────────────────────────

    /** Genera los retos de hoy si aún no existen. Seguro de llamar muchas veces. */
    suspend fun ensureToday(): List<ChallengeInstance> = tx {
        val today = today()
        expireMissions(today)
        val existing = dao.instancesOnNow(today)
        val prefs = prefs()
        if (existing.isNotEmpty() || !prefs.onboardingDone) return@tx existing

        val adjusted = applyAutoDifficulty(prefs, today)
        val kind = ChallengeEngine.dayKind(today, adjusted, dao.pausesNow())
        val challenges = allChallenges()
        // Tras una temporada sin actividad, retos de recuperación: pocos, cortos y fáciles para retomar.
        val pickPrefs = if (isReturning(today)) {
            adjusted.copy(
                difficulty = 1, categoryDifficulty = emptyMap(),
                dailyCount = minOf(adjusted.dailyCount, 2), minutesPerDay = minOf(adjusted.minutesPerDay, 10),
            )
        } else {
            adjusted
        }
        val picks = if (kind == DayKind.ACTIVE) {
            ChallengeEngine.dailyPicks(engineInput(today, pickPrefs, challenges), Random(today.toEpochDays()))
        } else {
            emptyList()
        }
        val habits = if (kind == DayKind.ACTIVE || kind == DayKind.OFF) ChallengeEngine.habitsFor(today, challenges) else emptyList()
        (picks + habits).forEachIndexed { slot, c -> dao.insertInstance(newInstance(c, today, slot)) }
        dao.instancesOnNow(today)
    }

    /** Los retos que tendría hoy con las preferencias actuales, sin guardarlos (último paso del onboarding). */
    suspend fun previewToday(): List<Challenge> {
        val today = today()
        val prefs = prefs()
        if (ChallengeEngine.dayKind(today, prefs, dao.pausesNow()) != DayKind.ACTIVE) return emptyList()
        // Misma semilla que ensureToday: lo que se ve aquí es lo que se guardará al terminar.
        return ChallengeEngine.dailyPicks(engineInput(today, prefs, allChallenges()), Random(today.toEpochDays()))
    }

    /** true si el último reto completado fue hace [Stats.COMEBACK_GAP_DAYS] días o más (y hubo alguno). */
    suspend fun isReturning(today: LocalDate = today()): Boolean =
        dao.lastActiveBefore(today)?.let { it.daysUntil(today) >= Stats.COMEBACK_GAP_DAYS } ?: false

    /**
     * Un reto más para hoy, también en días de descanso o pausa si el usuario lo pide.
     * Con [place] (clave de Tags) solo elige retos que se pueden hacer ahí.
     */
    suspend fun addExtraToday(place: String? = null): ChallengeInstance? = tx {
        val today = today()
        val existing = dao.instancesOnNow(today)
        val pool = allChallenges().filter { place == null || place in it.tags }
        val input = engineInput(today, prefs(), pool)
        val pick = ChallengeEngine.dailyPicks(input, Random.Default, count = 1, exclude = existing.map { it.challengeId }.toSet())
            .firstOrNull() ?: return@tx null
        newInstance(pick, today, (existing.maxOfOrNull { it.slot } ?: -1) + 1).also { dao.insertInstance(it) }
    }

    /** Añade un reto concreto a hoy. Si ya estaba (aunque se hubiera omitido), lo reabre. */
    suspend fun addToToday(challengeId: String): ChallengeInstance? = tx {
        val c = challenge(challengeId) ?: return@tx null
        val today = today()
        val existing = dao.instancesOnNow(today)
        val same = existing.firstOrNull { it.challengeId == challengeId }
        when {
            same == null -> newInstance(c, today, (existing.maxOfOrNull { it.slot } ?: -1) + 1).also { dao.insertInstance(it) }
            same.isDone || same.isOpen -> same
            else -> same.copy(status = InstanceStatus.PENDING).also { dao.updateInstance(it) }
        }
    }

    /** Un reto del catálogo que nunca has hecho, sin guardarlo ni tocar el plan del día. */
    suspend fun surprise(): Challenge? {
        val today = today()
        val todayIds = dao.instancesOnNow(today).map { it.challengeId }.toSet()
        return ChallengeEngine.surprise(engineInput(today, prefs(), Catalog.challenges), todayIds, Random.Default)
    }

    /** Nota del día sobre un reto (también en los ya hechos). Vacía la borra. */
    suspend fun setNote(id: String, note: String): Result<Unit> = tx {
        val clean = note.trim()
        if (clean.length > NOTE_MAX) return@tx Result.failure(IllegalArgumentException(tr("La nota puede tener hasta {0} caracteres.", NOTE_MAX)))
        val inst = dao.instance(id) ?: return@tx Result.failure(IllegalStateException("Ese reto ya no existe."))
        dao.updateInstance(inst.copy(note = clean.ifEmpty { null }))
        Result.success(Unit)
    }

    suspend fun setBlock(id: String, block: DayBlock?) = tx {
        dao.instance(id)?.let { dao.updateInstance(it.copy(block = block)) }
    }

    suspend fun start(id: String) = updateOpen(id) { it.copy(status = InstanceStatus.STARTED, startedAt = it.startedAt ?: now()) }
    suspend fun pause(id: String) = updateOpen(id) { it.copy(status = InstanceStatus.PAUSED) }
    suspend fun skip(id: String) = updateOpen(id) { it.copy(status = InstanceStatus.SKIPPED) }
    suspend fun toggleStep(id: String, index: Int) = updateOpen(id) { it.copy(stepsDone = it.stepsDone xor (1 shl index)) }

    suspend fun rate(id: String, rating: Rating) = tx {
        val inst = dao.instance(id) ?: return@tx
        if (inst.isDone) dao.updateInstance(inst.copy(rating = rating))
    }

    private suspend fun updateOpen(id: String, change: (ChallengeInstance) -> ChallengeInstance) = tx {
        val inst = dao.instance(id) ?: return@tx
        if (inst.isOpen) dao.updateInstance(change(inst))
    }

    /** Cambia el reto por otro sin penalización. Devuelve el nuevo, o null si no hay alternativa. */
    suspend fun replace(id: String): ChallengeInstance? = tx {
        val current = dao.instance(id)?.takeIf { it.isOpen } ?: return@tx null
        val today = current.date
        val todayIds = dao.instancesOnNow(today).map { it.challengeId }.toSet()
        val input = engineInput(today, prefs(), allChallenges())
        val next = ChallengeEngine.replacementFor(input, current, todayIds, Random.Default) ?: return@tx null
        dao.updateInstance(current.copy(status = InstanceStatus.REPLACED))
        newInstance(next, today, current.slot).also { dao.insertInstance(it) }
    }

    /** Cambia el reto por su versión más pequeña. Error con texto para el usuario si no se puede. */
    suspend fun simplify(id: String): Result<ChallengeInstance> = tx {
        val current = dao.instance(id)?.takeIf { it.isOpen }
            ?: return@tx Result.failure(IllegalStateException("Este reto ya no se puede cambiar."))
        val simpler = challenge(current.challengeId)?.simplerId?.let { challenge(it) }
            ?: return@tx Result.failure(IllegalStateException("Este reto no tiene una versión más sencilla."))
        if (dao.instancesOnNow(current.date).any { it.challengeId == simpler.id }) {
            return@tx Result.failure(IllegalStateException(tr("Ya tienes «{0}» hoy.", simpler.title)))
        }
        val updated = current.copy(
            challengeId = simpler.id, title = simpler.title, difficulty = simpler.difficulty, minutes = simpler.minutes,
            xp = simpler.xp, stepsDone = 0, originalChallengeId = current.originalChallengeId ?: current.challengeId,
        )
        dao.updateInstance(updated)
        Result.success(updated)
    }

    /**
     * Completa un reto: marca la instancia, suma su XP una sola vez, cierra misiones y desbloquea logros,
     * todo en una transacción. Completar algo ya completado no hace nada (devuelve null).
     */
    suspend fun complete(id: String, minimal: Boolean = false): CompletionResult? = tx {
        val inst = dao.instance(id) ?: return@tx null
        if (inst.isDone || inst.status == InstanceStatus.REPLACED) return@tx null
        val before = dao.totalXpNow()
        // El primer reto de un día tras una temporada sin actividad se reconoce: se premia volver, no se castiga la ausencia.
        val firstThatDay = dao.completedOn(inst.date) == 0
        val comeback = firstThatDay && dao.lastActiveBefore(inst.date)?.let { it.daysUntil(inst.date) >= XpRules.COMEBACK_XP_GAP_DAYS } == true
        val xp = if (minimal) XpRules.xpFor(1) else inst.xp
        dao.updateInstance(inst.copy(status = InstanceStatus.COMPLETED, completedAt = now(), minimal = minimal, xp = xp))
        dao.insertXp(xpEntry(XpSource.CHALLENGE, inst.id, xp, inst.category))
        if (comeback) dao.insertXp(xpEntry(XpSource.COMEBACK, inst.date.toString(), XpRules.COMEBACK_XP, null))
        val missions = completeMissions(today())
        val achievements = awardAchievements()
        val after = dao.totalXpNow()
        CompletionResult(after - before, XpRules.levelFor(before), XpRules.levelFor(after), achievements, missions, comeback)
    }

    /**
     * Deshace un reto completado por error: vuelve a pendiente y quita su XP (y la de regreso si era el único
     * del día). Los logros ya ganados se quedan.
     */
    suspend fun undoComplete(id: String) = tx {
        val inst = dao.instance(id)?.takeIf { it.isDone } ?: return@tx
        dao.updateInstance(
            inst.copy(status = InstanceStatus.PENDING, completedAt = null, rating = null, minimal = false, xp = XpRules.xpFor(inst.difficulty)),
        )
        dao.deleteChallengeXp(inst.id)
        if (dao.completedOn(inst.date) == 0) dao.deleteXp(XpSource.COMEBACK.name, inst.date.toString())
    }

    // ── Favoritos y metas ─────────────────────────────────────────────────────

    suspend fun toggleFavorite(challengeId: String): Boolean = tx {
        val isFavorite = dao.favoritesNow().any { it.challengeId == challengeId }
        if (isFavorite) dao.deleteFavorite(challengeId) else dao.upsertFavorite(Favorite(challengeId, now()))
        !isFavorite
    }

    /** Crea o edita una meta. Error con texto para el usuario si no vale. */
    suspend fun saveGoal(id: String?, title: String, why: String, targetDate: LocalDate?): Result<PersonalGoal> = tx {
        GoalRules.validate(title, why)?.let { return@tx Result.failure(IllegalArgumentException(it)) }
        val existing = id?.let { dao.goal(it) }
        val goal = PersonalGoal(
            id = existing?.id ?: newId(), title = title.trim(), why = why.trim(), targetDate = targetDate,
            status = existing?.status ?: GoalStatus.ACTIVE, createdAt = existing?.createdAt ?: now(), updatedAt = now(),
            achievedAt = existing?.achievedAt,
        )
        dao.upsertGoal(goal)
        Result.success(goal)
    }

    suspend fun setGoalStatus(id: String, status: GoalStatus) = tx {
        dao.goal(id)?.let {
            dao.upsertGoal(it.copy(status = status, updatedAt = now(), achievedAt = if (status == GoalStatus.ACHIEVED) now() else null))
        }
    }

    suspend fun deleteGoal(id: String) = tx { dao.deleteGoal(id) }

    // ── Dificultad adaptativa ─────────────────────────────────────────────────

    suspend fun difficultySuggestions(): List<AdaptiveDifficulty.Suggestion> {
        val prefs = prefs()
        if (prefs.difficultyMode != DifficultyMode.SUGGEST) return emptyList()
        val today = today()
        val history = dao.instancesSince(today.minus(AdaptiveDifficulty.WINDOW_DAYS, DateTimeUnit.DAY))
        return selectedCategories(prefs).mapNotNull { AdaptiveDifficulty.suggest(it, prefs, history, today) }
    }

    suspend fun applyDifficulty(suggestion: AdaptiveDifficulty.Suggestion) {
        updatePreferences {
            it.copy(
                categoryDifficulty = it.categoryDifficulty + (suggestion.category to suggestion.to),
                lastDifficultyChange = today(),
            )
        }
    }

    private suspend fun applyAutoDifficulty(prefs: Preferences, today: LocalDate): Preferences {
        if (prefs.difficultyMode != DifficultyMode.AUTO) return prefs
        val history = dao.instancesSince(today.minus(AdaptiveDifficulty.WINDOW_DAYS, DateTimeUnit.DAY))
        // Un solo ajuste por vez: el periodo de espera evita encadenar cambios.
        val change = selectedCategories(prefs).firstNotNullOfOrNull { AdaptiveDifficulty.suggest(it, prefs, history, today) }
            ?: return prefs
        val updated = prefs.copy(
            categoryDifficulty = prefs.categoryDifficulty + (change.category to change.to),
            lastDifficultyChange = today,
        )
        dao.upsertPreferences(updated)
        return updated
    }

    private fun selectedCategories(prefs: Preferences): List<String> =
        prefs.categories.ifEmpty { Categories.builtIn.map { it.key } }

    // ── Misiones ──────────────────────────────────────────────────────────────

    suspend fun startMission(missionId: String): Result<MissionRun> = tx {
        val mission = MissionCatalog.byId[missionId] ?: return@tx Result.failure(IllegalArgumentException("Misión desconocida."))
        if (dao.missionRunsNow().any { it.status == MissionStatus.ACTIVE }) {
            return@tx Result.failure(IllegalStateException("Ya tienes una misión en marcha."))
        }
        val today = today()
        val run = MissionRun("${mission.id}|$today", mission.id, today, MissionRules.endsOn(mission, today), MissionStatus.ACTIVE)
        dao.upsertMissionRun(run)
        completeMissions(today)
        awardAchievements()
        Result.success(run)
    }

    suspend fun abandonMission(runId: String) = tx {
        dao.missionRunsNow().firstOrNull { it.id == runId && it.status == MissionStatus.ACTIVE }
            ?.let { dao.upsertMissionRun(it.copy(status = MissionStatus.EXPIRED)) }
    }

    private suspend fun completeMissions(today: LocalDate): List<Mission> {
        val active = dao.missionRunsNow().filter { it.status == MissionStatus.ACTIVE && !MissionRules.isExpired(it, today) }
        if (active.isEmpty()) return emptyList()
        val done = dao.completedNow()
        return active.mapNotNull { run ->
            val mission = MissionCatalog.byId[run.missionId] ?: return@mapNotNull null
            if (!MissionRules.isComplete(mission, run, done)) return@mapNotNull null
            dao.upsertMissionRun(run.copy(status = MissionStatus.COMPLETED, completedAt = now()))
            dao.insertXp(xpEntry(XpSource.MISSION, run.id, mission.xpReward, null))
            mission
        }
    }

    private suspend fun expireMissions(today: LocalDate) {
        dao.missionRunsNow().filter { MissionRules.isExpired(it, today) }
            .forEach { dao.upsertMissionRun(it.copy(status = MissionStatus.EXPIRED)) }
    }

    // ── Pausas ────────────────────────────────────────────────────────────────

    /** Pausa desde hoy. Con [until], termina sola ese día (incluido); sin él, hasta que se quite a mano. */
    suspend fun startPause(until: LocalDate? = null): List<Achievement> = tx {
        val today = today()
        if (dao.pausesNow().none { it.covers(today) }) dao.upsertPause(Pause(today.toString(), today, until?.takeIf { it >= today }))
        awardAchievements()
    }

    /** Termina la pausa: hoy vuelve a ser un día normal. */
    suspend fun endPause() = tx {
        val today = today()
        val open = dao.pausesNow().firstOrNull { it.covers(today) } ?: return@tx
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        if (yesterday < open.startOn) dao.deletePause(open.id) else dao.upsertPause(open.copy(endOn = yesterday))
    }

    /**
     * Al volver de una pausa: los hábitos que no se marcan quedan en pausa (sin historial perdido) y el resto
     * sigue como estaba. No hay retos atrasados que recuperar: los días de pausa no generan nada.
     */
    suspend fun resumeAfterPause(pauseId: String, keepHabits: Set<String>) = tx {
        dao.customChallengesNow()
            .filter { it.status == CustomStatus.ACTIVE && it.habitDays != 0 && it.id !in keepHabits }
            .forEach { dao.upsertCustomChallenge(it.copy(status = CustomStatus.PAUSED, updatedAt = now())) }
        dao.upsertPreferences(prefs().copy(lastPauseReviewed = pauseId))
        dao.instancesOnNow(today()).filter { it.isHabit && it.isOpen && it.challengeId !in keepHabits }
            .forEach { dao.updateInstance(it.copy(status = InstanceStatus.SKIPPED)) }
    }

    // ── Revisión semanal ──────────────────────────────────────────────────────

    suspend fun weeklySummary(): WeeklySummary {
        val today = today()
        val from = today.minus(35, DateTimeUnit.DAY)
        val history = dao.instancesSince(from)
        val recentDone = history.mapNotNull { it.completedAt }.filter { dateOf(it) >= today.minus(28, DateTimeUnit.DAY) }
        return WeeklyReview.compute(
            today = today,
            prefs = prefs(),
            history = history,
            xpByDay = dao.allXp().map { dateOf(it.createdAt) to it.amount }.filter { it.first >= from },
            unlocked = dao.unlockedNow().mapNotNull { u -> AchievementCatalog.byId[u.achievementId]?.let { it.name to dateOf(u.unlockedAt) } },
            completionMinutes = recentDone.map { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone()).let { t -> t.hour * 60 + t.minute } },
        )
    }

    suspend fun markWeeklyReviewed() = updatePreferences { it.copy(lastWeeklyReview = today()) }

    suspend fun applyReviewSuggestion(s: ReviewSuggestion) = updatePreferences {
        when (s) {
            is ReviewSuggestion.FewerPerDay -> it.copy(dailyCount = s.to)
            is ReviewSuggestion.MorePerDay -> it.copy(dailyCount = s.to)
            is ReviewSuggestion.MoveReminder -> it.copy(reminderMinute = s.minuteOfDay)
            is ReviewSuggestion.ReminderDays -> it.copy(reminderDays = s.mask)
        }
    }

    // ── Búsqueda y exportación ────────────────────────────────────────────────

    suspend fun search(query: String): List<SearchHit> = Search.run(
        query,
        Catalog.challenges + dao.customChallengesNow().filter { it.status != CustomStatus.ARCHIVED }.map { it.toChallenge() },
        dao.unlockedNow().map { it.achievementId }.toSet().let { got -> AchievementCatalog.all.filter { !it.hidden || it.id in got } },
        dao.goalsNow(),
        dao.withNotes(),
    )

    /** Historial en CSV para una hoja de cálculo. */
    suspend fun exportCsv(): String {
        val custom = dao.customCategoriesNow()
        return CsvExport.instances(dao.allInstances()) { Categories.name(it, custom) }
    }

    // ── Retos propios ─────────────────────────────────────────────────────────

    /** Guarda un reto propio. Con errores por campo no guarda nada. */
    suspend fun saveCustom(draft: CustomChallengeDraft, id: String? = null): SaveResult = tx {
        val customs = dao.customChallengesNow()
        val categories = Categories.builtIn.map { it.key }.toSet() + dao.customCategoriesNow().map { it.id }
        val otherTitles = customs.filter { it.id != id && it.status != CustomStatus.ARCHIVED }.map { it.title }
        val errors = CustomChallengeRules.validate(draft, categories, otherTitles)
        if (errors.isNotEmpty()) return@tx SaveResult(errors)

        val existing = id?.let { dao.customChallenge(it) }
        val saved = CustomChallenge(
            id = existing?.id ?: newId(),
            title = draft.title.trim(),
            description = draft.description.trim(),
            category = draft.category,
            goal = draft.goal.trim(),
            doneWhen = draft.doneWhen.trim(),
            difficulty = draft.difficulty,
            minutes = draft.minutes,
            frequency = draft.frequency,
            habitDays = draft.habitDays,
            steps = draft.steps.map { it.trim() }.filter { it.isNotEmpty() },
            status = existing?.status ?: CustomStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now(),
            updatedAt = now(),
            tags = draft.tags.filter { it in Tags.all }.distinct(),
            minimum = draft.minimum.trim(),
            block = draft.block,
            reminderMinute = draft.reminderMinute?.takeIf { draft.habitDays != 0 && it in 0 until 24 * 60 },
        )
        dao.upsertCustomChallenge(saved)
        addHabitToTodayIfScheduled(saved)
        SaveResult(emptyMap(), awardAchievements())
    }

    suspend fun setCustomStatus(id: String, status: CustomStatus) = tx {
        dao.customChallenge(id)?.let { dao.upsertCustomChallenge(it.copy(status = status, updatedAt = now())) }
    }

    /** Borra el reto propio. Lo ya completado sigue en el historial (cada instancia guarda su copia). */
    suspend fun deleteCustom(id: String) = tx { dao.deleteCustomChallenge(id) }

    suspend fun customChallenge(id: String): CustomChallenge? = dao.customChallenge(id)

    /** Crea una categoría propia. Devuelve su id, o el error para el usuario. */
    suspend fun addCustomCategory(name: String): Result<String> = tx {
        val clean = name.trim()
        val existing = Categories.builtIn.map { it.name } + dao.customCategoriesNow().map { it.name }
        when {
            clean.length !in 2..30 -> Result.failure(IllegalArgumentException("El nombre necesita entre 2 y 30 caracteres."))
            existing.any { CustomChallengeRules.normalize(it) == CustomChallengeRules.normalize(clean) } ->
                Result.failure(IllegalArgumentException("Ya existe una categoría con ese nombre."))
            else -> Result.success(newId().also { dao.upsertCustomCategory(CustomCategory(it, clean, now())) })
        }
    }

    private suspend fun addHabitToTodayIfScheduled(c: CustomChallenge) {
        val today = today()
        val challenge = c.toChallenge()
        if (c.status != CustomStatus.ACTIVE || !challenge.isHabit || !prefs().onboardingDone) return
        val kind = ChallengeEngine.dayKind(today, prefs(), dao.pausesNow())
        if (kind == DayKind.REST || kind == DayKind.PAUSED) return
        if (ChallengeEngine.habitsFor(today, listOf(challenge)).isEmpty()) return
        val existing = dao.instancesOnNow(today)
        if (existing.none { it.challengeId == c.id }) {
            dao.insertInstance(newInstance(challenge, today, (existing.maxOfOrNull { it.slot } ?: -1) + 1))
        }
    }

    // ── Recordatorios ─────────────────────────────────────────────────────────

    /** Lo que el aviso diario debería decir ahora, o null si no toca. Anota el aviso de regreso para no repetirlo. */
    suspend fun reminderNow(): ReminderMessage? {
        val prefs = prefs()
        if (!prefs.onboardingDone) return null
        val today = today()
        val pending = ensureToday().filter { it.isOpen }.map { it.title }
        val lastActive = dao.completedNow().maxOfOrNull { it.date }
        val kind = ChallengeEngine.dayKind(today, prefs, dao.pausesNow())
        val message = Reminders.decide(nowLocal(), prefs, kind, pending, lastActive) ?: return null
        if (message.isRecovery) tx { dao.upsertPreferences(prefs().copy(lastRecoveryNotice = today)) }
        return message
    }

    /** Minutos del día con aviso propio de algún hábito activo. */
    suspend fun habitReminderMinutes(): List<Int> =
        dao.customChallengesNow().filter { it.status == CustomStatus.ACTIVE && it.habitDays != 0 }.mapNotNull { it.reminderMinute }

    /** Hábitos de hoy sin hacer cuyo aviso propio toca ahora. */
    suspend fun dueHabitReminders(): List<String> {
        val prefs = prefs()
        if (!prefs.onboardingDone) return emptyList()
        val times = dao.customChallengesNow().filter { it.status == CustomStatus.ACTIVE && it.habitDays != 0 && it.reminderMinute != null }
            .associate { it.id to it.reminderMinute!! }
        if (times.isEmpty()) return emptyList()
        val pending = ensureToday().filter { it.isOpen && it.challengeId in times }.map { it.title to times.getValue(it.challengeId) }
        return Reminders.dueHabits(nowLocal(), prefs, ChallengeEngine.dayKind(today(), prefs, dao.pausesNow()), pending)
    }

    // ── Logros y estadísticas ─────────────────────────────────────────────────

    suspend fun stats(): Stats = Stats.compute(
        today = today(),
        completed = dao.completedNow(),
        totalXp = dao.totalXpNow(),
        prefs = prefs(),
        pauses = dao.pausesNow(),
        customCreated = dao.customChallengesNow().size,
        missionsCompleted = dao.missionRunsNow().count { it.status == MissionStatus.COMPLETED },
    )

    /** Evento de dominio: algo cambió. Desbloquea lo que toque y suma su XP (una sola vez por logro). */
    private suspend fun awardAchievements(): List<Achievement> {
        val unlocked = dao.unlockedNow().map { it.achievementId }.toSet()
        val new = AchievementRules.newlyUnlocked(AchievementCatalog.all, stats(), dao.totalXpNow(), unlocked)
        new.forEach { a ->
            dao.insertUnlocked(UnlockedAchievement(a.id, now()))
            if (a.xpReward > 0) dao.insertXp(xpEntry(XpSource.ACHIEVEMENT, a.id, a.xpReward, null))
        }
        return new
    }

    // ── Respaldo ──────────────────────────────────────────────────────────────

    suspend fun exportBackup(): String = tx {
        BackupCodec.encode(
            Backup(
                exportedAt = clock.now().toString(),
                preferences = dao.preferencesNow(),
                customCategories = dao.customCategoriesNow(),
                customChallenges = dao.customChallengesNow(),
                instances = dao.allInstances(),
                xp = dao.allXp(),
                achievements = dao.unlockedNow(),
                missions = dao.missionRunsNow(),
                pauses = dao.pausesNow(),
                favorites = dao.favoritesNow(),
                goals = dao.goalsNow(),
            ),
        )
    }

    /** Reemplaza todos los datos por los del archivo. Si el archivo no es válido, no toca nada. */
    suspend fun importBackup(text: String): Result<Unit> {
        val backup = BackupCodec.decode(text).getOrElse { return Result.failure(it) }
        tx {
            clearAll()
            backup.preferences?.let { dao.upsertPreferences(it) }
            dao.insertAllCustomCategories(backup.customCategories)
            dao.insertAllCustomChallenges(backup.customChallenges)
            dao.insertAllInstances(backup.instances)
            dao.insertAllXp(backup.xp)
            dao.insertAllUnlocked(backup.achievements)
            dao.insertAllMissionRuns(backup.missions)
            dao.insertAllPauses(backup.pauses)
            dao.insertAllFavorites(backup.favorites)
            dao.insertAllGoals(backup.goals)
        }
        return Result.success(Unit)
    }

    /** Borra todos los datos del teléfono. La app vuelve a la bienvenida. */
    suspend fun wipe() = tx { clearAll() }

    private suspend fun clearAll() {
        dao.clearPreferences()
        dao.clearCustomCategories()
        dao.clearCustomChallenges()
        dao.clearInstances()
        dao.clearXp()
        dao.clearUnlocked()
        dao.clearMissionRuns()
        dao.clearPauses()
        dao.clearFavorites()
        dao.clearGoals()
    }

    // ── Auxiliares ────────────────────────────────────────────────────────────

    private suspend fun engineInput(today: LocalDate, prefs: Preferences, challenges: List<Challenge>) = ChallengeEngine.Input(
        today = today,
        prefs = prefs,
        challenges = challenges,
        history = dao.instancesSince(today.minus(30, DateTimeUnit.DAY)),
        completedEver = dao.completedIds().toSet(),
    )

    private fun newInstance(c: Challenge, date: LocalDate, slot: Int) = ChallengeInstance(
        id = "$date|${c.id}", challengeId = c.id, source = c.source, title = c.title, category = c.category,
        difficulty = c.difficulty, minutes = c.minutes, xp = c.xp, date = date, slot = slot, isHabit = c.isHabit,
        createdAt = now(), block = c.block,
    )
}
