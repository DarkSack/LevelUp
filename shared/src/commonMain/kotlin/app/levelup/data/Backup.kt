package app.levelup.data

import app.levelup.domain.ChallengeInstance
import app.levelup.domain.CustomCategory
import app.levelup.domain.CustomChallenge
import app.levelup.domain.Favorite
import app.levelup.domain.GoalRules
import app.levelup.domain.MissionRun
import app.levelup.domain.NOTE_MAX
import app.levelup.domain.PersonalGoal
import app.levelup.domain.Pause
import app.levelup.domain.Preferences
import app.levelup.domain.UnlockedAchievement
import app.levelup.domain.XpEntry
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import app.levelup.i18n.tr

/** Archivo de respaldo: JSON legible con todas las tablas. */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val preferences: Preferences? = null,
    val customCategories: List<CustomCategory> = emptyList(),
    val customChallenges: List<CustomChallenge> = emptyList(),
    val instances: List<ChallengeInstance> = emptyList(),
    val xp: List<XpEntry> = emptyList(),
    val achievements: List<UnlockedAchievement> = emptyList(),
    val missions: List<MissionRun> = emptyList(),
    val pauses: List<Pause> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val goals: List<PersonalGoal> = emptyList(),
) {
    companion object {
        const val FORMAT = "levelup-backup"
        /** Igual a la versión de la base de datos. Un archivo más nuevo que la app no se importa. */
        const val SCHEMA_VERSION = 2
        const val MAX_CHARS = 20_000_000
    }
}

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(backup: Backup): String = json.encodeToString(backup)

    /** Valida el archivo entero antes de que el repositorio toque nada. El error es el texto que ve el usuario. */
    fun decode(text: String): Result<Backup> {
        if (text.length > Backup.MAX_CHARS) return failure("El archivo es demasiado grande para ser un respaldo de LevelUp.")
        val backup = try {
            json.decodeFromString<Backup>(text)
        } catch (e: SerializationException) {
            return failure("El archivo no es un respaldo válido de LevelUp.")
        } catch (e: IllegalArgumentException) {
            return failure("El archivo no es un respaldo válido de LevelUp.")
        }
        if (backup.format != Backup.FORMAT) return failure("El archivo no es un respaldo de LevelUp.")
        if (backup.schemaVersion > Backup.SCHEMA_VERSION) {
            return failure("Este respaldo es de una versión más nueva de LevelUp. Actualiza la app e inténtalo de nuevo.")
        }
        val problems = buildList {
            if (backup.instances.any { it.difficulty !in 1..5 || it.xp !in 0..1000 }) add("retos")
            if (backup.xp.any { it.amount !in 0..1000 }) add("experiencia")
            if (backup.instances.map { it.id }.toSet().size != backup.instances.size) add("retos duplicados")
            if (backup.instances.groupBy { it.challengeId to it.date }.any { it.value.size > 1 }) add("retos repetidos el mismo día")
            if (backup.xp.groupBy { it.sourceType to it.sourceId }.any { it.value.size > 1 }) add("experiencia duplicada")
            if (backup.customChallenges.any { it.title.isBlank() || it.difficulty !in 1..5 || (it.reminderMinute ?: 0) !in 0 until 24 * 60 }) add("retos propios")
            if (backup.instances.any { (it.note?.length ?: 0) > NOTE_MAX }) add("notas")
            if (backup.goals.any { GoalRules.validate(it.title, it.why) != null }) add("metas")
            if (backup.goals.map { it.id }.toSet().size != backup.goals.size) add("metas duplicadas")
            if (backup.favorites.map { it.challengeId }.toSet().size != backup.favorites.size) add("favoritos duplicados")
        }
        if (problems.isNotEmpty()) return failure(tr("El respaldo tiene datos dañados ({0}). No se importó nada.", problems.joinToString { tr(it) }))
        return Result.success(backup)
    }

    private fun failure(message: String): Result<Backup> = Result.failure(IllegalArgumentException(message))
}
