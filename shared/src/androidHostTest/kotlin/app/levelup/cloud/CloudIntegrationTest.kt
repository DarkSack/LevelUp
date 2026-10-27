package app.levelup.cloud

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.levelup.data.LevelUpRepository
import app.levelup.data.SyncEngine
import app.levelup.data.buildDatabase
import io.github.jan.supabase.auth.MemorySessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La app contra un Supabase de verdad (el local de `supabase start`, con las migraciones del repo): registro con el
 * código del correo, sincronización entre dos teléfonos y lo social. Solo corre si se le pasa la clave publicable local:
 * `./gradlew :shared:testAndroidHostTest -PsupabaseLocalKey=<PUBLISHABLE_KEY de supabase status>`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CloudIntegrationTest {
    private val key: String? = System.getProperty("supabaseLocalKey")?.takeIf { it.isNotBlank() }
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val run = System.currentTimeMillis().toString(36)

    private fun cloud(device: String = "Robolectric") = Cloud(CloudConfig(API, key!!, "LevelUp test ($device; Android 15)"), MemorySessionManager())

    /** Un teléfono con la base de datos real de la app y la nube de verdad. */
    private inner class Phone(val cloud: Cloud = cloud()) {
        val db = buildDatabase(context, null)
        val repo = LevelUpRepository(db) { TimeZone.UTC }
        val service = CloudService(cloud, SyncEngine(db, cloud), repo, kotlinx.coroutines.GlobalScope) {}
    }

    @Test fun usernameAccountsNeedNoEmail() = runBlocking {
        assumeTrue("Sin -PsupabaseLocalKey: prueba contra el Supabase local omitida", key != null)
        val user = "u_$run"
        val p1 = Phone()
        assertTrue(p1.service.signUp(user, "nivel2026").isSuccess)
        val id = p1.cloud.currentUserId()!!
        val p2 = Phone(cloud("Tableta"))
        assertTrue(p2.service.signIn(" ${user.uppercase()} ", "nivel2026").isSuccess, "con mayúsculas y espacios es el mismo usuario")
        assertEquals(id, p2.cloud.currentUserId())
        assertTrue(Phone().service.signUp(user, "otra2026").isFailure, "el nombre ya tiene cuenta")
        assertTrue(Phone().service.signIn(user, "otra2026").isFailure, "contraseña equivocada")
        assertTrue(p1.service.changePassword("otra2026").isSuccess)
        assertTrue(Phone().service.signIn(user, "otra2026").isSuccess, "entra con la contraseña nueva")
    }

    @Test fun signUpSyncAndFriends() = runBlocking {
        assumeTrue("Sin -PsupabaseLocalKey: prueba contra el Supabase local omitida", key != null)
        val anaMail = "ana-$run@levelup.test"
        val betoMail = "beto-$run@levelup.test"

        // Registro sin verificación de correo: la sesión queda iniciada al momento.
        val a1 = Phone()
        a1.repo.updatePreferences { it.copy(onboardingDone = true, categories = listOf("fisico", "mental"), minutesPerDay = 30) }
        assertTrue(a1.service.signUp(anaMail, "nivel2026").isSuccess)
        val ana = a1.cloud.currentUserId()!!

        // Lo hecho en un teléfono llega al otro de la misma cuenta.
        val inst = a1.repo.ensureToday().first()
        a1.repo.complete(inst.id)
        a1.repo.toggleFavorite(inst.challengeId)
        a1.repo.saveGoal(null, "Correr 5 km", "", null).getOrThrow()
        a1.repo.updatePreferences { it.copy(avatar = "zorro") }
        assertTrue(a1.service.syncNow().isSuccess)
        val a2 = Phone(cloud("Tableta"))
        assertTrue(a2.service.signIn(anaMail, "nivel2026").isSuccess)
        assertTrue(a2.service.syncNow().isSuccess)
        assertEquals(a1.repo.totalXp.first(), a2.repo.totalXp.first())
        assertTrue(a2.repo.prefs().onboardingDone)
        assertEquals(listOf(inst.challengeId), a2.repo.favorites.first().map { it.challengeId }, "favoritos al otro teléfono")
        assertEquals(listOf("Correr 5 km"), a2.repo.goals.first().map { it.title }, "metas al otro teléfono")

        // Otra persona no ve nada de Ana (RLS).
        val beto = cloud()
        beto.signUp(betoMail, "nivel2026")
        val betoId = beto.currentUserId()!!
        assertTrue(beto.pull(0, 100).isEmpty(), "los datos de Ana no se ven desde otra cuenta")

        // Perfiles, búsqueda y amistad.
        val anaUser = "ana_$run".take(20)
        val betoUser = "beto_$run".take(20)
        val trophy = a1.repo.unlocked.first().first().achievementId
        a1.cloud.saveProfile(MyProfile(ana, anaUser, "Ana", leaderboardOptIn = true, avatar = "zorro", titleLevel = 1, trophies = listOf(trophy)))
        beto.saveProfile(MyProfile(betoId, betoUser, "Beto", leaderboardOptIn = true))
        assertEquals("Ana", a1.cloud.myProfile()?.displayName)
        assertEquals(listOf(betoUser), a1.cloud.search(betoUser).map { it.username })
        a1.cloud.sendFriendRequest(betoUser)
        val request = beto.pendingRequests().single()
        assertEquals(true, request.incoming)
        beto.respondFriendRequest(request.requestId!!, accept = true)
        val friend = a1.cloud.friends().single()
        assertEquals(betoUser, friend.username)
        assertTrue(friend.isFriend)
        val anaCard = beto.friends().single()
        assertEquals("zorro", anaCard.avatar)
        assertEquals(1, anaCard.title)
        assertEquals(listOf(trophy), anaCard.trophies, "solo trofeos desbloqueados y sincronizados")

        // Reacción y avisos (SyncWork lee la fecha del aviso con OffsetDateTime).
        a1.cloud.react(betoId, "aplauso")
        a1.cloud.react(betoId, "animo_puedes")
        val notices = beto.notices()
        assertTrue(notices.any { it.kind == "reaction" && it.refId == "aplauso" && it.actor?.username == anaUser })
        assertTrue(notices.any { it.refId == "animo_puedes" }, "mensaje de ánimo")
        notices.forEach { OffsetDateTime.parse(it.createdAt) }
        beto.markNoticesRead()
        assertTrue(beto.notices().all { it.read })

        // Reto en grupo: el servidor cuenta lo completado desde lo sincronizado.
        val today = a1.repo.today()
        a1.cloud.createGroup(inst.challengeId, "coop", 3, today, today.plus(6, DateTimeUnit.DAY), listOf(betoId))
        val invite = beto.groups().single()
        assertEquals("invited", invite.myStatus)
        beto.respondGroup(invite.id, accept = true)
        val group = a1.cloud.groups().single()
        assertEquals(1, group.members.single { it.userId == ana }.completed)
        assertEquals("joined", group.members.single { it.userId == betoId }.status)
        assertTrue(a1.cloud.groupHistory().isEmpty(), "un reto en marcha aún no es historial")

        // Clasificación semanal: los dos se apuntaron.
        a1.service.syncNow() // también actualiza racha y marco del perfil (touch_profile)
        val board = a1.cloud.leaderboard()
        assertEquals(setOf(anaUser, betoUser), board.map { it.username }.toSet())
        assertTrue(board.first { it.isMe }.weeklyXp > 0)

        // Dispositivos: Ana ve sus dos sesiones con su nombre y cierra la de la tableta desde el teléfono.
        val sessions = a1.cloud.sessions()
        assertEquals(setOf("Robolectric · Android 15", "Tableta · Android 15"), sessions.map { it.device }.toSet())
        assertEquals("Robolectric · Android 15", sessions.single { it.current }.device)
        a1.cloud.endSession(sessions.single { !it.current }.id)
        assertEquals(1, a1.cloud.sessions().size)

        // Borrar la cuenta borra lo del servidor; el teléfono se queda como invitado.
        beto.deleteAccount()
        assertTrue(a1.cloud.friends().isEmpty())
        assertTrue(a1.service.deleteAccount().isSuccess)
        assertEquals(null, a1.cloud.currentUserId())
        assertTrue(a1.repo.totalXp.first() > 0, "lo local sigue en el teléfono")
        listOf(a1, a2).forEach { it.db.close() }
    }
}

/** El Supabase local de `supabase start`. */
internal const val API = "http://127.0.0.1:54321"
