package app.levelup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levelup.Platform
import app.levelup.content.AchievementCatalog
import app.levelup.content.Cosmetics
import app.levelup.content.MissionCatalog
import app.levelup.data.CompletionResult
import app.levelup.data.LevelUpRepository
import app.levelup.domain.Achievement
import app.levelup.domain.AchievementRules
import app.levelup.domain.AchievementState
import app.levelup.domain.AdaptiveDifficulty
import app.levelup.domain.Challenge
import app.levelup.domain.ChallengeEngine
import app.levelup.domain.ChallengeInstance
import app.levelup.domain.Cosmetic
import app.levelup.domain.CustomChallengeDraft
import app.levelup.domain.CustomStatus
import app.levelup.domain.DayKind
import app.levelup.domain.DraftField
import app.levelup.domain.Mission
import app.levelup.domain.MissionRules
import app.levelup.domain.MissionRun
import app.levelup.domain.MissionStatus
import app.levelup.domain.Preferences
import app.levelup.domain.Rating
import app.levelup.domain.Stats
import app.levelup.domain.Streaks
import app.levelup.domain.XpRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

sealed interface UiEvent {
    data class Completed(val instanceId: String, val result: CompletionResult) : UiEvent
    data class Unlocked(val achievements: List<Achievement>) : UiEvent
    data class Message(val text: String) : UiEvent
}

data class AchievementView(val achievement: Achievement, val state: AchievementState, val progress: Int, val unlockedOn: LocalDate?)

data class ProgressState(
    val stats: Stats = Stats(),
    val level: XpRules.LevelProgress = XpRules.progress(0),
    val categoryLevels: Map<String, XpRules.LevelProgress> = emptyMap(),
    val weeks: List<Streaks.Week> = emptyList(),
    /** XP de las últimas 8 semanas, de la más antigua a la actual. */
    val weeklyXp: List<Pair<LocalDate, Int>> = emptyList(),
    val dayKind: DayKind = DayKind.ACTIVE,
    val activeMission: Pair<Mission, MissionRun>? = null,
    val missionProgress: Int = 0,
    val missionsCompleted: Set<String> = emptySet(),
    val achievements: List<AchievementView> = emptyList(),
    val frames: List<Cosmetic> = listOf(Cosmetics.byId.getValue(Cosmetics.DEFAULT)),
)

/**
 * Un solo ViewModel para toda la app: los datos ya viven en Room y llegan como flujos.
 * Las acciones corren en viewModelScope, así que no se cortan si la pantalla rota o se cierra.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LevelUpViewModel(private val repo: LevelUpRepository, private val platform: Platform) : ViewModel() {
    private val day = MutableStateFlow(repo.today())
    val today: StateFlow<LocalDate> = day

    /** null mientras carga la primera vez. */
    val prefs: StateFlow<Preferences?> = repo.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val todayInstances: StateFlow<List<ChallengeInstance>> =
        day.flatMapLatest { repo.instancesOn(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recent: StateFlow<List<ChallengeInstance>> =
        day.flatMapLatest { repo.recentInstances(60) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customChallenges = repo.customChallenges.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customCategories = repo.customCategories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val suggestions = MutableStateFlow<List<AdaptiveDifficulty.Suggestion>>(emptyList())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events

    private val xpData = combine(repo.totalXp, repo.xpByCategory, repo.xpEntries) { total, byCat, entries -> Triple(total, byCat, entries) }
    private val rewardData = combine(repo.unlocked, repo.missionRuns, repo.pauses) { u, m, p -> Triple(u, m, p) }

    val progress: StateFlow<ProgressState> = combine(
        day, repo.preferences, repo.completed, xpData, combine(rewardData, repo.customChallenges) { r, c -> r to c.size },
    ) { today, prefs, completed, (total, byCat, entries), (rewards, customCount) ->
        val (unlocked, runs, pauses) = rewards
        val stats = Stats.compute(
            today, completed, total, prefs, pauses, customCount, runs.count { it.status == MissionStatus.COMPLETED },
        )
        val unlockedIds = unlocked.associate { it.achievementId to repo.dateOf(it.unlockedAt) }
        val doneMissions = runs.filter { it.status == MissionStatus.COMPLETED }.map { it.missionId }.toSet()
        val active = runs.firstOrNull { it.status == MissionStatus.ACTIVE && !MissionRules.isExpired(it, today) }
            ?.let { run -> MissionCatalog.byId[run.missionId]?.let { it to run } }
        val thisWeek = Streaks.weekStart(today)
        val weekStarts = (7 downTo 0).map { thisWeek.minus(it * 7, DateTimeUnit.DAY) }
        val xpByWeek = entries.groupBy { Streaks.weekStart(repo.dateOf(it.createdAt)) }.mapValues { (_, l) -> l.sumOf { it.amount } }
        ProgressState(
            stats = stats,
            level = XpRules.progress(total),
            categoryLevels = byCat.mapValues { XpRules.progress(it.value, XpRules.CATEGORY_STEP) },
            weeks = Streaks.weeks(today, completed.mapTo(HashSet()) { it.date }, prefs.weeklyGoalDays),
            weeklyXp = weekStarts.map { it to (xpByWeek[it] ?: 0) },
            dayKind = ChallengeEngine.dayKind(today, prefs, pauses),
            activeMission = active,
            missionProgress = active?.let { (m, r) -> MissionRules.progress(m, r, completed) } ?: 0,
            missionsCompleted = doneMissions,
            achievements = AchievementCatalog.all.map { a ->
                AchievementView(a, AchievementRules.state(a, unlockedIds.keys), a.progress(stats), unlockedIds[a.id])
            },
            frames = Cosmetics.unlocked(unlockedIds.keys, doneMissions),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProgressState())

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun announce(achievements: List<Achievement>) {
        if (achievements.isNotEmpty()) _events.emit(UiEvent.Unlocked(achievements))
    }

    /** Al abrir o volver a la app: cambia de día si hace falta y genera los retos de hoy. */
    fun refreshDay() = act {
        day.value = repo.today()
        repo.ensureToday()
        suggestions.value = repo.difficultySuggestions()
    }

    // ── Retos ─────────────────────────────────────────────────────────────────

    fun complete(id: String) = act { repo.complete(id)?.let { _events.emit(UiEvent.Completed(id, it)) } }
    fun undo(id: String) = act { repo.undoComplete(id) }
    fun start(id: String) = act { repo.start(id) }
    fun pause(id: String) = act { repo.pause(id) }
    fun skip(id: String) = act {
        repo.skip(id)
        _events.emit(UiEvent.Message("Lo dejamos para otro día. Sin problema."))
    }
    fun toggleStep(id: String, index: Int) = act { repo.toggleStep(id, index) }
    fun rate(id: String, rating: Rating) = act { repo.rate(id, rating) }

    fun replace(id: String) = act {
        val next = repo.replace(id)
        _events.emit(UiEvent.Message(if (next != null) "Nuevo reto: ${next.title}" else "No encontramos otro reto parecido ahora."))
    }

    fun simplify(id: String) = act {
        repo.simplify(id).fold(
            onSuccess = { _events.emit(UiEvent.Message("Versión más sencilla: ${it.title}")) },
            onFailure = { _events.emit(UiEvent.Message(it.message ?: "No se pudo simplificar.")) },
        )
    }

    fun addToToday(challengeId: String) = act {
        val inst = repo.addToToday(challengeId)
        _events.emit(UiEvent.Message(if (inst != null) "Añadido a hoy: ${inst.title}" else "No se pudo añadir."))
    }

    fun addExtraToday() = act {
        val inst = repo.addExtraToday()
        _events.emit(UiEvent.Message(if (inst != null) "Reto añadido: ${inst.title}" else "No hay más retos disponibles hoy."))
    }

    suspend fun challenge(id: String): Challenge? = repo.challenge(id)

    suspend fun previewToday(): List<Challenge> = repo.previewToday()

    fun hourNow(): Int = repo.nowLocal().hour

    // ── Retos propios ─────────────────────────────────────────────────────────

    fun saveCustom(draft: CustomChallengeDraft, id: String?, onResult: (Map<DraftField, String>) -> Unit) = act {
        val result = repo.saveCustom(draft, id)
        onResult(result.errors)
        if (result.errors.isEmpty()) {
            _events.emit(UiEvent.Message(if (id == null) "Reto creado" else "Cambios guardados"))
            announce(result.unlocked)
        }
    }

    fun setCustomStatus(id: String, status: CustomStatus) = act { repo.setCustomStatus(id, status) }
    fun deleteCustom(id: String) = act {
        repo.deleteCustom(id)
        _events.emit(UiEvent.Message("Reto eliminado. Lo que ya completaste sigue en tu historial."))
    }

    fun addCustomCategory(name: String, onResult: (Result<String>) -> Unit) = act { onResult(repo.addCustomCategory(name)) }

    suspend fun customChallenge(id: String) = repo.customChallenge(id)

    // ── Preferencias ──────────────────────────────────────────────────────────

    fun updatePrefs(change: (Preferences) -> Preferences) = act {
        announce(repo.updatePreferences(change))
        platform.remindersChanged()
    }

    fun setReminders(enabled: Boolean) = act {
        if (enabled && !platform.notificationsAllowed() && !platform.requestNotifications()) {
            _events.emit(UiEvent.Message("Sin permiso de notificaciones no podemos avisarte. Puedes darlo en los ajustes del teléfono."))
            return@act
        }
        repo.updatePreferences { it.copy(remindersEnabled = enabled) }
        platform.remindersChanged()
    }

    fun applySuggestion(s: AdaptiveDifficulty.Suggestion) = act {
        repo.applyDifficulty(s)
        suggestions.value = suggestions.value - s
    }

    fun dismissSuggestion(s: AdaptiveDifficulty.Suggestion) {
        suggestions.value = suggestions.value - s
    }

    fun finishOnboarding() = act {
        repo.updatePreferences { it.copy(onboardingDone = true, onboardingStep = 0) }
        day.value = repo.today()
        repo.ensureToday()
        platform.remindersChanged()
    }

    fun replayTutorial() = act { repo.updatePreferences { it.copy(onboardingDone = false, onboardingStep = 1) } }

    // ── Misiones y pausas ─────────────────────────────────────────────────────

    fun startMission(id: String) = act {
        repo.startMission(id).onFailure { _events.emit(UiEvent.Message(it.message ?: "No se pudo empezar.")) }
    }

    fun abandonMission(runId: String) = act { repo.abandonMission(runId) }

    fun setPaused(paused: Boolean) = act {
        if (paused) announce(repo.startPause()) else repo.endPause()
        day.value = repo.today()
        repo.ensureToday()
    }

    // ── Datos ─────────────────────────────────────────────────────────────────

    fun exportData() = act {
        val ok = platform.exportFile("levelup-respaldo-${repo.today()}.json", repo.exportBackup())
        if (ok) _events.emit(UiEvent.Message("Respaldo guardado."))
    }

    fun importData() = act {
        val text = platform.importFile() ?: return@act
        repo.importBackup(text).fold(
            onSuccess = {
                day.value = repo.today()
                repo.ensureToday()
                platform.remindersChanged()
                _events.emit(UiEvent.Message("Datos restaurados."))
            },
            onFailure = { _events.emit(UiEvent.Message(it.message ?: "No se pudo importar.")) },
        )
    }

    fun wipe() = act {
        repo.wipe()
        platform.remindersChanged()
    }
}
