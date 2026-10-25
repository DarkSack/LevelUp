package app.levelup.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable

enum class Frequency { DAILY, WEEKLY, ONCE }
enum class ChallengeSource { CATALOG, CUSTOM }
enum class InstanceStatus { PENDING, STARTED, PAUSED, COMPLETED, SKIPPED, REPLACED }
enum class Rating { EASY, RIGHT, HARD }
enum class CustomStatus { ACTIVE, PAUSED, ARCHIVED }
enum class XpSource { CHALLENGE, ACHIEVEMENT, MISSION }
enum class MissionStatus { ACTIVE, COMPLETED, EXPIRED }
enum class DifficultyMode { MANUAL, SUGGEST, AUTO }
enum class NotificationLevel { OFF, LOW, NORMAL }
enum class MotionPref { SYSTEM, REDUCED, FULL }
enum class ThemePref { SYSTEM, LIGHT, DARK }

/** FULL: XP, rachas y celebraciones. CALM: sin rachas ni celebraciones. */
enum class GamificationStyle { FULL, CALM }

/** Definición de un reto (catálogo o propio). Lo que hay que hacer un día concreto es [ChallengeInstance]. */
data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    /** Objetivo medible. */
    val goal: String,
    /** Cómo sabes que está hecho. */
    val doneWhen: String,
    val difficulty: Int,
    val minutes: Int,
    val frequency: Frequency = Frequency.DAILY,
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val safety: String? = null,
    /** Versión más pequeña del mismo reto (simplificar sin penalización). */
    val simplerId: String? = null,
    /** Reto que conviene haber completado antes. */
    val requires: String? = null,
    val source: ChallengeSource = ChallengeSource.CATALOG,
    /** Días de un hábito (máscara de [Days]); 0 = no es hábito. */
    val habitDays: Int = 0,
    val availableFrom: LocalDate? = null,
    val contentVersion: Int = 1,
) {
    val xp: Int get() = XpRules.xpFor(difficulty)
    val isHabit: Boolean get() = habitDays != 0
}

/** Conjuntos de días de la semana como máscara de bits (lunes = bit 0). */
object Days {
    const val ALL = 0b1111111
    fun bit(day: DayOfWeek): Int = 1 shl (day.isoDayNumber - 1)
    fun has(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
    fun of(vararg days: DayOfWeek): Int = days.fold(0) { acc, d -> acc or bit(d) }
    fun list(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { has(mask, it) }
}

const val DEFAULT_ACCENT = "teal"

@Serializable
@Entity(tableName = "preferences")
data class Preferences(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "",
    val categories: List<String> = emptyList(),
    val priorityCategories: List<String> = emptyList(),
    val difficulty: Int = 2,
    val difficultyMode: DifficultyMode = DifficultyMode.SUGGEST,
    /** Ajustes por categoría que salen de la dificultad adaptativa; si no hay, se usa [difficulty]. */
    val categoryDifficulty: Map<String, Int> = emptyMap(),
    val lastDifficultyChange: LocalDate? = null,
    val minutesPerDay: Int = 15,
    val dailyCount: Int = 3,
    val challengeDays: Int = Days.ALL,
    val weeklyGoalDays: Int = 3,
    val restDays: Int = 0,
    val remindersEnabled: Boolean = false,
    val reminderMinute: Int = 19 * 60,
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 8 * 60,
    val notificationLevel: NotificationLevel = NotificationLevel.LOW,
    val recoveryReminders: Boolean = true,
    val lastRecoveryNotice: LocalDate? = null,
    val textScale: Float = 1f,
    val simpleMode: Boolean = false,
    val motion: MotionPref = MotionPref.SYSTEM,
    val highContrast: Boolean = false,
    val theme: ThemePref = ThemePref.SYSTEM,
    val haptics: Boolean = true,
    val showSteps: Boolean = true,
    val focusTimer: Boolean = false,
    val gamification: GamificationStyle = GamificationStyle.FULL,
    val equippedFrame: String = "frame.basic",
    val onboardingStep: Int = 0,
    val onboardingDone: Boolean = false,
    /** Avisos en el teléfono cuando un amigo te escribe algo (solicitud, reacción, reto en grupo). */
    @ColumnInfo(defaultValue = "1") val socialNotifications: Boolean = true,
    /** Color de acento. Los distintos de [DEFAULT_ACCENT] son de LevelUp Plus. */
    @ColumnInfo(defaultValue = "teal") val accent: String = DEFAULT_ACCENT,
) {
    fun difficultyFor(category: String): Int = categoryDifficulty[category] ?: difficulty
}

@Serializable
@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Serializable
@Entity(tableName = "custom_challenges")
data class CustomChallenge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val goal: String,
    val doneWhen: String,
    val difficulty: Int,
    val minutes: Int,
    val frequency: Frequency,
    val habitDays: Int,
    val steps: List<String>,
    val status: CustomStatus,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toChallenge() = Challenge(
        id = id, title = title, description = description, category = category, goal = goal,
        doneWhen = doneWhen, difficulty = difficulty, minutes = minutes, frequency = frequency,
        steps = steps, source = ChallengeSource.CUSTOM, habitDays = habitDays,
    )
}

/** Un reto concreto de un día. Guarda copia de título, categoría y XP para que el historial no cambie. */
@Serializable
@Entity(
    tableName = "challenge_instances",
    indices = [Index(value = ["challengeId", "date"], unique = true), Index("date")],
)
data class ChallengeInstance(
    @PrimaryKey val id: String,
    val challengeId: String,
    val source: ChallengeSource,
    val title: String,
    val category: String,
    val difficulty: Int,
    val minutes: Int,
    val xp: Int,
    val date: LocalDate,
    val slot: Int,
    val isHabit: Boolean = false,
    val status: InstanceStatus = InstanceStatus.PENDING,
    /** Pasos marcados (bit i = paso i). */
    val stepsDone: Int = 0,
    /** Reto original si se simplificó. */
    val originalChallengeId: String? = null,
    val rating: Rating? = null,
    val createdAt: Long,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
) {
    val isDone: Boolean get() = status == InstanceStatus.COMPLETED
    val isOpen: Boolean get() = status == InstanceStatus.PENDING || status == InstanceStatus.STARTED || status == InstanceStatus.PAUSED
    val simplified: Boolean get() = originalChallengeId != null
}

@Serializable
@Entity(tableName = "xp_ledger", indices = [Index(value = ["sourceType", "sourceId"], unique = true)])
data class XpEntry(
    @PrimaryKey val id: String,
    val sourceType: XpSource,
    val sourceId: String,
    val amount: Int,
    /** Solo los retos suman a una categoría. */
    val category: String?,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievement(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
)

@Serializable
@Entity(tableName = "mission_runs")
data class MissionRun(
    @PrimaryKey val id: String,
    val missionId: String,
    val startedOn: LocalDate,
    val endsOn: LocalDate,
    val status: MissionStatus,
    val completedAt: Long? = null,
)

/** Pausa de objetivos: los días dentro no rompen la racha ni generan retos. `endOn` null = sigue abierta. */
@Serializable
@Entity(tableName = "pauses")
data class Pause(
    @PrimaryKey val id: String,
    val startOn: LocalDate,
    val endOn: LocalDate? = null,
) {
    fun covers(day: LocalDate): Boolean = day >= startOn && (endOn == null || day <= endOn)
}
