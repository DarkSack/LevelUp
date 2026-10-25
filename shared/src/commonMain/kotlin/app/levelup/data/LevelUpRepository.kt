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
import app.levelup.domain.DayKind
import app.levelup.domain.DifficultyMode
import app.levelup.domain.DraftField
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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class SaveResult(val errors: Map<DraftField, String>, val unlocked: List<Achievement> = emptyList())

data class CompletionResult(
    val xpGained: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val achievements: List<Achievement>,
    val missions: List<Mission>,
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
        val picks = if (kind == DayKind.ACTIVE) {
            ChallengeEngine.dailyPicks(engineInput(today, adjusted, challenges), Random(today.toEpochDays()))
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

    /** Un reto más para hoy, también en días de descanso o pausa si el usuario lo pide. */
    suspend fun addExtraToday(): ChallengeInstance? = tx {
        val today = today()
        val existing = dao.instancesOnNow(today)
        val input = engineInput(today, prefs(), allChallenges())
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
            return@tx Result.failure(IllegalStateException("Ya tienes «${simpler.title}» hoy."))
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
    suspend fun complete(id: String): CompletionResult? = tx {
        val inst = dao.instance(id) ?: return@tx null
        if (inst.isDone || inst.status == InstanceStatus.REPLACED) return@tx null
        val before = dao.totalXpNow()
        dao.updateInstance(inst.copy(status = InstanceStatus.COMPLETED, completedAt = now()))
        dao.insertXp(xpEntry(XpSource.CHALLENGE, inst.id, inst.xp, inst.category))
        val missions = completeMissions(today())
        val achievements = awardAchievements()
        val after = dao.totalXpNow()
        CompletionResult(after - before, XpRules.levelFor(before), XpRules.levelFor(after), achievements, missions)
    }

    /** Deshace un reto completado por error: vuelve a pendiente y quita su XP. Los logros ya ganados se quedan. */
    suspend fun undoComplete(id: String) = tx {
        val inst = dao.instance(id)?.takeIf { it.isDone } ?: return@tx
        dao.updateInstance(inst.copy(status = InstanceStatus.PENDING, completedAt = null, rating = null))
        dao.deleteChallengeXp(inst.id)
    }

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

    suspend fun startPause(): List<Achievement> = tx {
        if (dao.pausesNow().none { it.endOn == null }) dao.upsertPause(Pause(today().toString(), today()))
        awardAchievements()
    }

    /** Termina la pausa: hoy vuelve a ser un día normal. */
    suspend fun endPause() = tx {
        val open = dao.pausesNow().firstOrNull { it.endOn == null } ?: return@tx
        val yesterday = today().minus(1, DateTimeUnit.DAY)
        if (yesterday < open.startOn) dao.deletePause(open.id) else dao.upsertPause(open.copy(endOn = yesterday))
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
        createdAt = now(),
    )
}
