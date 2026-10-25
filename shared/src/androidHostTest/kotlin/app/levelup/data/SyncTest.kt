package app.levelup.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.levelup.domain.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Servidor en memoria con las mismas reglas que `push_records`: gana el cambio más reciente (en empate, lo guardado). */
class FakeServer {
    private var seq = 0L
    val rows = linkedMapOf<Pair<String, String>, RemoteRecord>()

    fun client() = object : RemoteSync {
        override suspend fun push(records: List<SyncRecord>) {
            records.forEach { r ->
                val current = rows[r.entity to r.id]
                if (current == null || r.updatedAt > current.updatedAt) {
                    rows[r.entity to r.id] = RemoteRecord(r.entity, r.id, r.data, r.updatedAt, r.deleted, ++seq)
                }
            }
        }

        override suspend fun pull(after: Long, limit: Int) = rows.values.filter { it.seq > after }.sortedBy { it.seq }.take(limit)
    }
}

/** Un teléfono: su base de datos, su repositorio y su motor de sincronización. */
class Device(context: Context, server: FakeServer, clock: TestClock) {
    val db = buildDatabase(context, null)
    val repo = LevelUpRepository(db, clock) { TimeZone.UTC }
    val sync = SyncEngine(db, server.client(), clock)

    suspend fun onboard() = repo.updatePreferences {
        it.copy(onboardingDone = true, categories = listOf("fisico", "mental", "hogar"), minutesPerDay = 30)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = TestClock()
    private val server = FakeServer()
    private val devices = mutableListOf<Device>()

    private fun device() = Device(context, server, clock).also { devices += it }

    @After fun tearDown() = devices.forEach { it.db.close() }

    @Test fun guestChangesAreNotQueued() = runTest {
        val a = device()
        a.onboard()
        a.repo.complete(a.repo.ensureToday().first().id)
        assertEquals(0, a.sync.pending.first())
    }

    @Test fun signingInUploadsEverythingAndAnotherPhoneGetsIt() = runTest {
        val a = device()
        a.onboard()
        a.repo.complete(a.repo.ensureToday().first().id)
        a.sync.attach("u1")
        assertTrue(a.sync.sync().isSuccess)
        assertEquals(0, a.sync.pending.first())

        val b = device()
        b.sync.attach("u1")
        assertTrue(b.sync.sync().isSuccess)
        assertEquals(a.repo.totalXp.first(), b.repo.totalXp.first())
        assertTrue(b.repo.prefs().onboardingDone)
        assertEquals(a.repo.ensureToday().map { it.id }, b.repo.ensureToday().map { it.id })
    }

    @Test fun theSameChallengeDoneOnTwoPhonesCountsOnce() = runTest {
        val a = device()
        val b = device()
        a.onboard(); b.onboard()
        a.sync.attach("u1"); b.sync.attach("u1")
        val inst = a.repo.ensureToday().first()
        assertEquals(inst.id, b.repo.ensureToday().first().id, "mismo reto, mismo id en los dos teléfonos")

        a.repo.complete(inst.id) // los dos sin conexión
        b.repo.complete(inst.id)
        a.sync.sync(); b.sync.sync(); a.sync.sync()

        val expected = inst.xp + 10 // el reto y el logro "Primer paso", una sola vez
        assertEquals(expected, a.repo.totalXp.first())
        assertEquals(expected, b.repo.totalXp.first())
    }

    @Test fun undoReachesTheOtherPhone() = runTest {
        val a = device()
        val b = device()
        a.onboard()
        a.sync.attach("u1"); b.sync.attach("u1")
        val inst = a.repo.ensureToday().first()
        a.repo.complete(inst.id)
        a.sync.sync(); b.sync.sync()
        assertEquals(inst.xp + 10, b.repo.totalXp.first())

        a.repo.undoComplete(inst.id)
        a.sync.sync(); b.sync.sync()
        assertEquals(10, b.repo.totalXp.first(), "queda solo la XP del logro, que no se pierde al deshacer")
    }

    @Test fun theNewestEditWins() = runTest {
        val a = device()
        val b = device()
        a.onboard()
        a.sync.attach("u1"); b.sync.attach("u1")
        a.sync.sync(); b.sync.sync()

        b.repo.updatePreferences { it.copy(displayName = "Desde B") }
        Thread.sleep(5)
        a.repo.updatePreferences { it.copy(displayName = "Desde A") }
        b.sync.sync(); a.sync.sync(); b.sync.sync()
        assertEquals("Desde A", a.repo.prefs().displayName)
        assertEquals("Desde A", b.repo.prefs().displayName)
    }

    @Test fun aChangeMadeWhileOfflineIsNotOverwrittenByTheDownload() = runTest {
        val a = device()
        val b = device()
        a.onboard()
        a.sync.attach("u1"); b.sync.attach("u1")
        a.repo.updatePreferences { it.copy(displayName = "Viejo") }
        a.sync.sync()
        b.repo.updatePreferences { it.copy(displayName = "Nuevo") }
        b.sync.sync()
        assertEquals("Nuevo", b.repo.prefs().displayName)
        assertEquals(0, b.sync.pending.first())
    }

    @Test fun signingInOnANewPhoneKeepsTheAccountSettings() = runTest {
        val a = device()
        a.onboard()
        a.repo.updatePreferences { it.copy(displayName = "Ana") }
        a.sync.attach("u1")
        a.sync.sync()

        val b = device() // teléfono nuevo: hizo el onboarding como invitado antes de entrar en la cuenta
        b.repo.updatePreferences { it.copy(onboardingDone = true, displayName = "Invitado") }
        b.repo.addCustomCategory("Solo en B")
        b.sync.attach("u1")
        b.sync.sync()
        assertEquals("Ana", b.repo.prefs().displayName)
        assertTrue(server.rows.values.any { it.entity == "custom_category" }, "lo que solo existía en B también sube")
    }

    @Test fun signingOutWipesThisPhoneButNotTheAccount() = runTest {
        val a = device()
        a.onboard()
        a.repo.complete(a.repo.ensureToday().first().id)
        a.sync.attach("u1")
        a.sync.sync()
        val rowsBefore = server.rows.size

        a.sync.detach(wipeLocal = true)
        assertEquals(Preferences(), a.repo.prefs())
        assertEquals(0, a.repo.totalXp.first())
        assertEquals(rowsBefore, server.rows.size)
        assertTrue(server.rows.values.none { it.deleted }, "cerrar sesión no borra nada en la cuenta")

        a.repo.updatePreferences { it.copy(displayName = "Invitado") }
        assertEquals(0, a.sync.pending.first(), "tras cerrar sesión ya no se anota nada")
    }

    @Test fun importingABackupWhileSignedInReplacesTheAccountData() = runTest {
        val a = device()
        a.onboard()
        a.sync.attach("u1")
        a.repo.addCustomCategory("Música")
        a.sync.sync()
        val backup = a.repo.exportBackup()
        a.repo.addCustomCategory("Pintura")
        a.sync.sync()

        assertTrue(a.repo.importBackup(backup).isSuccess)
        a.sync.sync()
        val categories = server.rows.values.filter { it.entity == "custom_category" && !it.deleted }
            .map { it.data["name"]?.jsonPrimitive?.content }
        assertEquals(listOf("Música"), categories)
    }
}

/** La base de datos de la versión 1 (la primera publicada) pasa a la 2 sin perder nada. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun version1DataSurvivesTheUpgrade() = runTest {
        val name = "migracion.db"
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(File("schemas/app.levelup.data.AppDatabase/1.json").readText()).jsonObject["database"]!!.jsonObject
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
            schema["entities"]!!.jsonArray.forEach { e ->
                val table = e.jsonObject["tableName"]!!.jsonPrimitive.content
                old.execSQL(e.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                e.jsonObject["indices"]?.jsonArray?.forEach { i ->
                    old.execSQL(i.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["setupQueries"]!!.jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.execSQL("INSERT INTO custom_categories (id, name, createdAt, archived) VALUES ('c1', 'Música', 1, 0)")
            old.version = 1
        }

        val db = buildDatabase(context, name)
        try {
            assertEquals(listOf("Música"), db.dao().customCategoriesNow().map { it.name })
            val repo = LevelUpRepository(db)
            repo.updatePreferences { it.copy(displayName = "Ana") }
            assertEquals("teal", repo.prefs().accent)
            assertEquals(0, db.syncDao().pendingCountNow())
        } finally {
            db.close()
        }
    }
}
