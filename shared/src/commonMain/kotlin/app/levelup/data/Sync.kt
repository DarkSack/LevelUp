package app.levelup.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import app.levelup.domain.ChallengeInstance
import app.levelup.domain.CustomCategory
import app.levelup.domain.CustomChallenge
import app.levelup.domain.Favorite
import app.levelup.domain.MissionRun
import app.levelup.domain.PersonalGoal
import app.levelup.domain.Pause
import app.levelup.domain.Preferences
import app.levelup.domain.UnlockedAchievement
import app.levelup.domain.XpEntry
import app.levelup.domain.XpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

/**
 * Cambio local pendiente de subir. Una fila por registro: si se edita tres veces sin conexión, se sube una
 * vez con su estado final. La escriben triggers de SQLite en la misma transacción que el cambio, así que no
 * se pierde aunque la app se cierre a medias.
 */
@Entity(tableName = "sync_outbox", primaryKeys = ["entity", "entityId"])
data class OutboxEntry(val entity: String, val entityId: String, val changedAt: Long)

/** Una sola fila. `userId` null = modo invitado: los triggers no anotan nada. */
@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val id: Int = 1,
    val userId: String? = null,
    /** Último `seq` del servidor ya descargado. */
    val cursor: Long = 0,
    /** 1 mientras se aplica lo descargado, para que eso no vuelva a la cola de subida. */
    val applying: Int = 0,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null,
)

/** Registro tal como viaja al servidor (`push_records`). */
@Serializable
data class SyncRecord(val entity: String, val id: String, val data: JsonObject, val updatedAt: Long, val deleted: Boolean)

/** Registro tal como lo devuelve el servidor (`user_records`). */
@Serializable
data class RemoteRecord(
    val entity: String,
    val id: String,
    val data: JsonObject,
    @SerialName("client_updated_at") val updatedAt: Long,
    val deleted: Boolean,
    val seq: Long,
)

/** Lo que el motor necesita del servidor. Supabase lo implementa en `cloud/`; las pruebas, en memoria. */
interface RemoteSync {
    suspend fun push(records: List<SyncRecord>)
    suspend fun pull(after: Long, limit: Int): List<RemoteRecord>
}

private class SyncedTable(val table: String, val entity: String, val idColumn: String = "id")

private val syncedTables = listOf(
    SyncedTable("preferences", "preferences"),
    SyncedTable("custom_categories", "custom_category"),
    SyncedTable("custom_challenges", "custom_challenge"),
    SyncedTable("challenge_instances", "instance"),
    SyncedTable("xp_ledger", "xp"),
    SyncedTable("unlocked_achievements", "achievement", "achievementId"),
    SyncedTable("mission_runs", "mission_run"),
    SyncedTable("pauses", "pause"),
    SyncedTable("favorites", "favorite", "challengeId"),
    SyncedTable("personal_goals", "goal"),
)

/** Milisegundos desde 1970 según SQLite (precisión de ms, a diferencia de strftime('%s')). */
private const val SQL_NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

/**
 * Crea los triggers que llenan la cola de subida. Idempotente: corre en cada apertura de la base.
 * Borrar y luego insertar (en vez de INSERT OR REPLACE): dentro de un trigger manda la política de conflicto
 * de la sentencia que lo dispara, y un INSERT OR IGNORE de Room haría que el cambio no se anotara.
 */
object SyncTriggers : RoomDatabase.Callback() {
    val statements: List<String> = syncedTables.flatMap { t ->
        listOf("INSERT" to "NEW", "UPDATE" to "NEW", "DELETE" to "OLD").map { (op, row) ->
            """
            CREATE TRIGGER IF NOT EXISTS sync_${t.table}_${op.lowercase()} AFTER $op ON ${t.table}
            WHEN EXISTS (SELECT 1 FROM sync_state WHERE id = 1 AND userId IS NOT NULL AND applying = 0)
            BEGIN
              DELETE FROM sync_outbox WHERE entity = '${t.entity}' AND entityId = CAST($row.${t.idColumn} AS TEXT);
              INSERT INTO sync_outbox (entity, entityId, changedAt)
              VALUES ('${t.entity}', CAST($row.${t.idColumn} AS TEXT), $SQL_NOW_MS);
            END
            """.trimIndent()
        }
    }

    override fun onOpen(connection: SQLiteConnection) {
        statements.forEach(connection::execSQL)
    }
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_state WHERE id = 1") fun state(): Flow<SyncState?>
    @Query("SELECT * FROM sync_state WHERE id = 1") suspend fun stateNow(): SyncState?
    @Upsert suspend fun putState(value: SyncState)
    @Query("UPDATE sync_state SET applying = :value WHERE id = 1") suspend fun setApplying(value: Int)
    @Query("UPDATE sync_state SET cursor = :cursor WHERE id = 1") suspend fun setCursor(cursor: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox") fun pendingCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM sync_outbox") suspend fun pendingCountNow(): Int
    @Query("SELECT * FROM sync_outbox ORDER BY changedAt LIMIT :limit") suspend fun outbox(limit: Int): List<OutboxEntry>
    @Query("SELECT COUNT(*) FROM sync_outbox WHERE entity = :entity AND entityId = :id") suspend fun isPending(entity: String, id: String): Int
    /** Solo si no volvió a cambiar mientras se subía. */
    @Query("DELETE FROM sync_outbox WHERE entity = :entity AND entityId = :id AND changedAt <= :changedAt")
    suspend fun confirm(entity: String, id: String, changedAt: Long)
    @Query("DELETE FROM sync_outbox") suspend fun clearOutbox()

    // Al entrar en una cuenta, todo lo local se sube para fusionarlo con lo que ya hubiera en ella. Cada
    // registro lleva la hora real de su último cambio (0 si no se sabe): así lo local más viejo no pisa lo
    // más nuevo de la cuenta, y lo que solo existe en este teléfono se sube igual.
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'preferences', CAST(id AS TEXT), 0 FROM preferences")
    suspend fun dirtyPreferences()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'custom_category', id, createdAt FROM custom_categories")
    suspend fun dirtyCustomCategories()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'custom_challenge', id, updatedAt FROM custom_challenges")
    suspend fun dirtyCustomChallenges()
    @Query(
        "INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) " +
            "SELECT 'instance', id, MAX(createdAt, COALESCE(startedAt, 0), COALESCE(completedAt, 0)) FROM challenge_instances",
    )
    suspend fun dirtyInstances()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'xp', id, createdAt FROM xp_ledger")
    suspend fun dirtyXp()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'achievement', achievementId, unlockedAt FROM unlocked_achievements")
    suspend fun dirtyAchievements()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'mission_run', id, COALESCE(completedAt, 0) FROM mission_runs")
    suspend fun dirtyMissionRuns()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'pause', id, 0 FROM pauses")
    suspend fun dirtyPauses()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'favorite', challengeId, createdAt FROM favorites")
    suspend fun dirtyFavorites()
    @Query("INSERT OR REPLACE INTO sync_outbox (entity, entityId, changedAt) SELECT 'goal', id, updatedAt FROM personal_goals")
    suspend fun dirtyGoals()

    @Query("SELECT * FROM preferences WHERE id = :id") suspend fun preferences(id: Int): Preferences?
    @Query("SELECT * FROM custom_categories WHERE id = :id") suspend fun customCategory(id: String): CustomCategory?
    @Query("SELECT * FROM custom_challenges WHERE id = :id") suspend fun customChallenge(id: String): CustomChallenge?
    @Query("SELECT * FROM challenge_instances WHERE id = :id") suspend fun instance(id: String): ChallengeInstance?
    @Query("SELECT * FROM xp_ledger WHERE id = :id") suspend fun xp(id: String): XpEntry?
    @Query("SELECT * FROM unlocked_achievements WHERE achievementId = :id") suspend fun achievement(id: String): UnlockedAchievement?
    @Query("SELECT * FROM mission_runs WHERE id = :id") suspend fun missionRun(id: String): MissionRun?
    @Query("SELECT * FROM pauses WHERE id = :id") suspend fun pause(id: String): Pause?
    @Query("SELECT * FROM favorites WHERE challengeId = :id") suspend fun favorite(id: String): Favorite?
    @Query("SELECT * FROM personal_goals WHERE id = :id") suspend fun goal(id: String): PersonalGoal?

    @Upsert suspend fun putPreferences(value: Preferences)
    @Upsert suspend fun putCustomCategory(value: CustomCategory)
    @Upsert suspend fun putCustomChallenge(value: CustomChallenge)
    @Upsert suspend fun putInstance(value: ChallengeInstance)
    @Upsert suspend fun putXp(value: XpEntry)
    @Upsert suspend fun putAchievement(value: UnlockedAchievement)
    @Upsert suspend fun putMissionRun(value: MissionRun)
    @Upsert suspend fun putPause(value: Pause)
    @Upsert suspend fun putFavorite(value: Favorite)
    @Upsert suspend fun putGoal(value: PersonalGoal)

    // Filas locales antiguas (de antes de los ids deterministas) que representan lo mismo con otro id.
    @Query("DELETE FROM challenge_instances WHERE challengeId = :challengeId AND date = :date AND id <> :id")
    suspend fun dropInstanceTwin(challengeId: String, date: LocalDate, id: String)
    @Query("DELETE FROM xp_ledger WHERE sourceType = :type AND sourceId = :sourceId AND id <> :id")
    suspend fun dropXpTwin(type: XpSource, sourceId: String, id: String)

    @Query("DELETE FROM preferences WHERE id = :id") suspend fun deletePreferences(id: Int)
    @Query("DELETE FROM custom_categories WHERE id = :id") suspend fun deleteCustomCategory(id: String)
    @Query("DELETE FROM custom_challenges WHERE id = :id") suspend fun deleteCustomChallenge(id: String)
    @Query("DELETE FROM challenge_instances WHERE id = :id") suspend fun deleteInstance(id: String)
    @Query("DELETE FROM xp_ledger WHERE id = :id") suspend fun deleteXp(id: String)
    @Query("DELETE FROM unlocked_achievements WHERE achievementId = :id") suspend fun deleteAchievement(id: String)
    @Query("DELETE FROM mission_runs WHERE id = :id") suspend fun deleteMissionRun(id: String)
    @Query("DELETE FROM pauses WHERE id = :id") suspend fun deletePause(id: String)
    @Query("DELETE FROM favorites WHERE challengeId = :id") suspend fun deleteFavorite(id: String)
    @Query("DELETE FROM personal_goals WHERE id = :id") suspend fun deleteGoal(id: String)
}

/** Cómo leer, escribir y borrar cada tipo de registro sincronizado. */
private class Kind<T : Any>(
    val serializer: KSerializer<T>,
    val read: suspend SyncDao.(String) -> T?,
    val write: suspend SyncDao.(T) -> Unit,
    val delete: suspend SyncDao.(String) -> Unit,
)

private val kinds: Map<String, Kind<*>> = mapOf(
    "preferences" to Kind(Preferences.serializer(), { preferences(it.toInt()) }, { putPreferences(it) }, { deletePreferences(it.toInt()) }),
    "custom_category" to Kind(CustomCategory.serializer(), { customCategory(it) }, { putCustomCategory(it) }, { deleteCustomCategory(it) }),
    "custom_challenge" to Kind(CustomChallenge.serializer(), { customChallenge(it) }, { putCustomChallenge(it) }, { deleteCustomChallenge(it) }),
    // `verified` sale de Health Connect y no se sube (ver `record`): al bajar, se conserva el de este teléfono.
    "instance" to Kind(ChallengeInstance.serializer(), { instance(it) },
        { dropInstanceTwin(it.challengeId, it.date, it.id); putInstance(it.copy(verified = it.isDone && instance(it.id)?.verified == true)) },
        { deleteInstance(it) }),
    "xp" to Kind(XpEntry.serializer(), { xp(it) }, { dropXpTwin(it.sourceType, it.sourceId, it.id); putXp(it) }, { deleteXp(it) }),
    "achievement" to Kind(UnlockedAchievement.serializer(), { achievement(it) }, { putAchievement(it) }, { deleteAchievement(it) }),
    "mission_run" to Kind(MissionRun.serializer(), { missionRun(it) }, { putMissionRun(it) }, { deleteMissionRun(it) }),
    "pause" to Kind(Pause.serializer(), { pause(it) }, { putPause(it) }, { deletePause(it) }),
    "favorite" to Kind(Favorite.serializer(), { favorite(it) }, { putFavorite(it) }, { deleteFavorite(it) }),
    "goal" to Kind(PersonalGoal.serializer(), { goal(it) }, { putGoal(it) }, { deleteGoal(it) }),
)

/**
 * Sube la cola y baja lo nuevo del servidor. Reglas (docs/ARQUITECTURA.md → Sincronización):
 * - Gana la edición más reciente de cada registro (lo decide el servidor con la hora de cada cambio).
 * - Lo pendiente de subir nunca se pisa con lo que baja: primero se sube y el servidor decide.
 * - La XP y los logros tienen ids deterministas: completar lo mismo en dos teléfonos no suma dos veces.
 * - Si algo falla a medias, la cola sigue intacta y el siguiente intento continúa.
 */
class SyncEngine(
    private val db: AppDatabase,
    private val remote: RemoteSync,
    private val clock: Clock = Clock.System,
) {
    private val dao = db.syncDao()
    private val data = db.dao()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val state: Flow<SyncState?> = dao.state()
    val pending: Flow<Int> = dao.pendingCount()

    private suspend fun <R> tx(block: suspend () -> R): R = db.useWriterConnection { it.immediateTransaction { block() } }

    /** Entrar en una cuenta: todo lo de este teléfono se marca para subir y se descarga la cuenta entera. */
    suspend fun attach(userId: String) = tx {
        val previous = dao.stateNow()
        dao.putState(SyncState(userId = userId, lastSyncedAt = previous?.lastSyncedAt.takeIf { previous?.userId == userId }))
        dao.dirtyPreferences()
        dao.dirtyCustomCategories()
        dao.dirtyCustomChallenges()
        dao.dirtyInstances()
        dao.dirtyXp()
        dao.dirtyAchievements()
        dao.dirtyMissionRuns()
        dao.dirtyPauses()
        dao.dirtyFavorites()
        dao.dirtyGoals()
    }

    /** Salir de la cuenta. Con `wipeLocal` se borra todo lo de este teléfono (sigue en la cuenta). */
    suspend fun detach(wipeLocal: Boolean) = tx {
        dao.putState(SyncState(applying = 1))
        if (wipeLocal) {
            data.clearPreferences(); data.clearCustomCategories(); data.clearCustomChallenges(); data.clearInstances()
            data.clearXp(); data.clearUnlocked(); data.clearMissionRuns(); data.clearPauses()
            data.clearFavorites(); data.clearGoals()
        }
        dao.clearOutbox()
        dao.putState(SyncState())
    }

    suspend fun userId(): String? = dao.stateNow()?.userId

    suspend fun hasPending(): Boolean = dao.pendingCountNow() > 0

    /** Una vuelta completa. Seguro de llamar a la vez desde varios sitios: se ejecuta de una en una. */
    suspend fun sync(): Result<Unit> = mutex.withLock {
        val user = dao.stateNow()?.userId ?: return@withLock Result.failure(IllegalStateException("Sin cuenta"))
        runCatching {
            push()
            pull()
        }.onSuccess {
            tx { dao.stateNow()?.takeIf { it.userId == user }?.let { dao.putState(it.copy(lastSyncedAt = clock.now().toEpochMilliseconds(), lastError = null)) } }
        }.onFailure { e ->
            tx { dao.stateNow()?.takeIf { it.userId == user }?.let { dao.putState(it.copy(lastError = e::class.simpleName ?: "Error")) } }
        }
    }

    private suspend fun push() {
        repeat(MAX_PAGES) {
            val batch = dao.outbox(PUSH_BATCH)
            if (batch.isEmpty()) return
            remote.push(batch.map { record(it) })
            tx { batch.forEach { dao.confirm(it.entity, it.entityId, it.changedAt) } }
        }
    }

    private suspend fun record(e: OutboxEntry): SyncRecord {
        @Suppress("UNCHECKED_CAST")
        val kind = kinds[e.entity] as Kind<Any>?
        val row = kind?.read?.invoke(dao, e.entityId)
        return if (kind == null || row == null) {
            SyncRecord(e.entity, e.entityId, JsonObject(emptyMap()), e.changedAt, deleted = true)
        } else {
            val data = json.encodeToJsonElement(kind.serializer, row).jsonObject
            // Lo derivado de Health Connect no sale del teléfono (política de privacidad y declaración de Play).
            SyncRecord(e.entity, e.entityId, if (e.entity == "instance") JsonObject(data - "verified") else data, e.changedAt, deleted = false)
        }
    }

    private suspend fun pull() {
        repeat(MAX_PAGES) {
            val cursor = dao.stateNow()?.cursor ?: 0
            val page = remote.pull(cursor, PULL_PAGE)
            if (page.isEmpty()) return
            tx {
                dao.setApplying(1)
                page.forEach { r -> if (dao.isPending(r.entity, r.id) == 0) apply(r) }
                dao.setCursor(page.maxOf { it.seq })
                dao.setApplying(0)
            }
            if (page.size < PULL_PAGE) return
        }
    }

    private suspend fun apply(r: RemoteRecord) {
        @Suppress("UNCHECKED_CAST")
        val kind = kinds[r.entity] as Kind<Any>? ?: return // de una versión más nueva de la app: se ignora
        if (r.deleted) {
            kind.delete(dao, r.id)
            return
        }
        // Un registro que esta versión no entiende se salta en vez de bloquear la sincronización entera.
        val row = runCatching { json.decodeFromJsonElement(kind.serializer, r.data) }.getOrNull() ?: return
        kind.write(dao, row)
    }

    private companion object {
        const val PUSH_BATCH = 200
        const val PULL_PAGE = 500
        const val MAX_PAGES = 50
    }
}
