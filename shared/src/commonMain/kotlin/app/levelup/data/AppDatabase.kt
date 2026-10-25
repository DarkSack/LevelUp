package app.levelup.data

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.levelup.domain.ChallengeInstance
import app.levelup.domain.CustomCategory
import app.levelup.domain.CustomChallenge
import app.levelup.domain.MissionRun
import app.levelup.domain.Pause
import app.levelup.domain.Preferences
import app.levelup.domain.UnlockedAchievement
import app.levelup.domain.XpEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

/** Cambiar el esquema: subir `version`, añadir una Migration y su prueba (docs/ARQUITECTURA.md). */
@Database(
    entities = [
        Preferences::class, CustomCategory::class, CustomChallenge::class, ChallengeInstance::class,
        XpEntry::class, UnlockedAchievement::class, MissionRun::class, Pause::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): LevelUpDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

class Converters {
    @TypeConverter fun fromDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun fromList(value: List<String>): String = Json.encodeToString(value)
    @TypeConverter fun toList(value: String): List<String> = Json.decodeFromString(value)
    @TypeConverter fun fromMap(value: Map<String, Int>): String = Json.encodeToString(value)
    @TypeConverter fun toMap(value: String): Map<String, Int> = Json.decodeFromString(value)
}

data class CategoryXp(val category: String, val xp: Int)

@Dao
interface LevelUpDao {
    @Query("SELECT * FROM preferences WHERE id = 1") fun preferences(): Flow<Preferences?>
    @Query("SELECT * FROM preferences WHERE id = 1") suspend fun preferencesNow(): Preferences?
    @Upsert suspend fun upsertPreferences(value: Preferences)

    @Query("SELECT * FROM challenge_instances WHERE date = :date ORDER BY slot, createdAt")
    fun instancesOn(date: LocalDate): Flow<List<ChallengeInstance>>

    @Query("SELECT * FROM challenge_instances WHERE date = :date ORDER BY slot, createdAt")
    suspend fun instancesOnNow(date: LocalDate): List<ChallengeInstance>

    @Query("SELECT * FROM challenge_instances WHERE date >= :from ORDER BY date, slot")
    suspend fun instancesSince(from: LocalDate): List<ChallengeInstance>

    @Query("SELECT * FROM challenge_instances WHERE date >= :from ORDER BY date DESC, slot")
    fun instancesSinceFlow(from: LocalDate): Flow<List<ChallengeInstance>>

    @Query("SELECT * FROM challenge_instances WHERE id = :id") suspend fun instance(id: String): ChallengeInstance?
    @Query("SELECT * FROM challenge_instances WHERE id = :id") fun instanceFlow(id: String): Flow<ChallengeInstance?>

    @Query("SELECT * FROM challenge_instances WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun completed(): Flow<List<ChallengeInstance>>

    @Query("SELECT * FROM challenge_instances WHERE status = 'COMPLETED'")
    suspend fun completedNow(): List<ChallengeInstance>

    @Query("SELECT DISTINCT challengeId FROM challenge_instances WHERE status = 'COMPLETED'")
    suspend fun completedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertInstance(value: ChallengeInstance): Long
    @Update suspend fun updateInstance(value: ChallengeInstance)

    /** Devuelve -1 si esa acción ya había sumado XP (`UNIQUE(sourceType, sourceId)`). */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertXp(value: XpEntry): Long
    @Query("DELETE FROM xp_ledger WHERE sourceType = 'CHALLENGE' AND sourceId = :instanceId")
    suspend fun deleteChallengeXp(instanceId: String)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger") fun totalXp(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger") suspend fun totalXpNow(): Int
    @Query("SELECT category, SUM(amount) AS xp FROM xp_ledger WHERE category IS NOT NULL GROUP BY category")
    fun xpByCategory(): Flow<List<CategoryXp>>
    @Query("SELECT * FROM xp_ledger ORDER BY createdAt") fun xpEntries(): Flow<List<XpEntry>>

    @Query("SELECT * FROM unlocked_achievements") fun unlocked(): Flow<List<UnlockedAchievement>>
    @Query("SELECT * FROM unlocked_achievements") suspend fun unlockedNow(): List<UnlockedAchievement>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertUnlocked(value: UnlockedAchievement): Long

    @Query("SELECT * FROM mission_runs ORDER BY startedOn DESC") fun missionRuns(): Flow<List<MissionRun>>
    @Query("SELECT * FROM mission_runs") suspend fun missionRunsNow(): List<MissionRun>
    @Upsert suspend fun upsertMissionRun(value: MissionRun)

    @Query("SELECT * FROM pauses ORDER BY startOn") fun pauses(): Flow<List<Pause>>
    @Query("SELECT * FROM pauses ORDER BY startOn") suspend fun pausesNow(): List<Pause>
    @Upsert suspend fun upsertPause(value: Pause)
    @Query("DELETE FROM pauses WHERE id = :id") suspend fun deletePause(id: String)

    @Query("SELECT * FROM custom_challenges ORDER BY createdAt DESC") fun customChallenges(): Flow<List<CustomChallenge>>
    @Query("SELECT * FROM custom_challenges") suspend fun customChallengesNow(): List<CustomChallenge>
    @Query("SELECT * FROM custom_challenges WHERE id = :id") suspend fun customChallenge(id: String): CustomChallenge?
    @Upsert suspend fun upsertCustomChallenge(value: CustomChallenge)
    @Query("DELETE FROM custom_challenges WHERE id = :id") suspend fun deleteCustomChallenge(id: String)

    @Query("SELECT * FROM custom_categories ORDER BY name") fun customCategories(): Flow<List<CustomCategory>>
    @Query("SELECT * FROM custom_categories") suspend fun customCategoriesNow(): List<CustomCategory>
    @Upsert suspend fun upsertCustomCategory(value: CustomCategory)

    // Respaldo
    @Query("SELECT * FROM challenge_instances") suspend fun allInstances(): List<ChallengeInstance>
    @Query("SELECT * FROM xp_ledger") suspend fun allXp(): List<XpEntry>
    @Insert suspend fun insertAllInstances(values: List<ChallengeInstance>)
    @Insert suspend fun insertAllXp(values: List<XpEntry>)
    @Insert suspend fun insertAllUnlocked(values: List<UnlockedAchievement>)
    @Insert suspend fun insertAllMissionRuns(values: List<MissionRun>)
    @Insert suspend fun insertAllPauses(values: List<Pause>)
    @Insert suspend fun insertAllCustomChallenges(values: List<CustomChallenge>)
    @Insert suspend fun insertAllCustomCategories(values: List<CustomCategory>)

    @Query("DELETE FROM preferences") suspend fun clearPreferences()
    @Query("DELETE FROM custom_categories") suspend fun clearCustomCategories()
    @Query("DELETE FROM custom_challenges") suspend fun clearCustomChallenges()
    @Query("DELETE FROM challenge_instances") suspend fun clearInstances()
    @Query("DELETE FROM xp_ledger") suspend fun clearXp()
    @Query("DELETE FROM unlocked_achievements") suspend fun clearUnlocked()
    @Query("DELETE FROM mission_runs") suspend fun clearMissionRuns()
    @Query("DELETE FROM pauses") suspend fun clearPauses()
}
