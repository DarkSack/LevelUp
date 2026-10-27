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
/** COMEBACK: XP por retomar tras una pausa larga (una vez por regreso). */
enum class XpSource { CHALLENGE, ACHIEVEMENT, MISSION, COMEBACK }
enum class MissionStatus { ACTIVE, COMPLETED, EXPIRED }
enum class DifficultyMode { MANUAL, SUGGEST, AUTO }
enum class NotificationLevel { OFF, LOW, NORMAL }
enum class MotionPref { SYSTEM, REDUCED, FULL }
enum class ThemePref { SYSTEM, LIGHT, DARK }

/** FULL: XP, rachas y celebraciones. CALM: sin rachas ni celebraciones. OFF: ni XP ni niveles a la vista, solo hábitos. */
enum class GamificationStyle { FULL, CALM, OFF }

/** Momento del día en que se piensa hacer un reto. null = cuando sea. */
enum class DayBlock { MORNING, AFTERNOON, EVENING }

enum class GoalStatus { ACTIVE, ACHIEVED, ARCHIVED }

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
    /** Versión mínima de un reto propio («al menos ponerme las zapatillas»). Vacía = no tiene. */
    val minimum: String = "",
    /** Momento del día por defecto (hábitos propios). */
    val block: DayBlock? = null,
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
    /** Colores apagados, sin animaciones ni ventanas de celebración. */
    @ColumnInfo(defaultValue = "0") val lowStimulation: Boolean = false,
    /** Impide capturas de pantalla en las pantallas con datos personales (cuenta, datos, metas). */
    @ColumnInfo(defaultValue = "0") val secureScreens: Boolean = false,
    /** Funciones experimentales activadas (ver content/Lab.kt). */
    @ColumnInfo(defaultValue = "[]") val labFlags: List<String> = emptyList(),
    /** Días en que puede sonar el recordatorio (máscara de [Days]). */
    @ColumnInfo(defaultValue = "127") val reminderDays: Int = Days.ALL,
    /** Último día en que se hizo la revisión semanal. */
    val lastWeeklyReview: LocalDate? = null,
    /** Última pausa terminada cuya vuelta ya se repasó (qué hábitos retomar). */
    val lastPauseReviewed: String? = null,
    /** Avatar elegido (clave de content/Avatars.kt). Vacío = la inicial del nombre. */
    @ColumnInfo(defaultValue = "") val avatar: String = "",
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
    /** Lugar y «sin materiales» (claves de content/Tags.kt). */
    @ColumnInfo(defaultValue = "[]") val tags: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "") val minimum: String = "",
    val block: DayBlock? = null,
    /** Aviso propio del hábito (minuto del día). null = sin aviso; solo cuenta en hábitos. */
    val reminderMinute: Int? = null,
) {
    fun toChallenge() = Challenge(
        id = id, title = title, description = description, category = category, goal = goal,
        doneWhen = doneWhen, difficulty = difficulty, minutes = minutes, frequency = frequency,
        steps = steps, tags = tags, source = ChallengeSource.CUSTOM, habitDays = habitDays, minimum = minimum, block = block,
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
    /** Nota libre del usuario sobre ese día (se puede buscar). */
    val note: String? = null,
    val block: DayBlock? = null,
    /** Se completó en su versión mínima (da la XP de dificultad 1). */
    @ColumnInfo(defaultValue = "0") val minimal: Boolean = false,
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

object PauseRules {
    /** Días tras el final de una pausa en que todavía se ofrece elegir qué hábitos retomar. */
    const val RETURN_WINDOW_DAYS = 30

    /** La pausa terminada más reciente cuya vuelta aún no se repasó, si terminó hace poco. */
    fun pendingReturn(today: LocalDate, pauses: List<Pause>, lastReviewed: String?): Pause? {
        val latest = pauses.filter { it.endOn != null && it.endOn < today }.maxByOrNull { it.endOn!! } ?: return null
        if (latest.id == lastReviewed) return null
        if (latest.endOn!!.toEpochDays() < today.toEpochDays() - RETURN_WINDOW_DAYS) return null
        return latest
    }
}

/** Reto guardado para repetirlo. Sirve para retos del catálogo y propios. */
@Serializable
@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val challengeId: String, val createdAt: Long)

/** Meta personal: algo que importa aunque no sea un reto del catálogo («terminar el curso de inglés»). */
@Serializable
@Entity(tableName = "personal_goals")
data class PersonalGoal(
    @PrimaryKey val id: String,
    val title: String,
    val why: String = "",
    val targetDate: LocalDate? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long,
    val achievedAt: Long? = null,
)

object GoalRules {
    const val TITLE_MAX = 60
    const val WHY_MAX = 200

    /** Texto del error para el usuario, o null si vale. */
    fun validate(title: String, why: String): String? = when {
        title.trim().length < 3 -> "Escribe la meta con al menos 3 letras."
        title.trim().length > TITLE_MAX -> "La meta puede tener hasta $TITLE_MAX caracteres."
        why.trim().length > WHY_MAX -> "El motivo puede tener hasta $WHY_MAX caracteres."
        else -> null
    }
}

const val NOTE_MAX = 280
