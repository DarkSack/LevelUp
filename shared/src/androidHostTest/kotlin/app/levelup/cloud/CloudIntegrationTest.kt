package app.levelup.cloud

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.levelup.data.LevelUpRepository
import app.levelup.data.SyncEngine
import app.levelup.data.buildDatabase
import io.github.jan.supabase.auth.MemorySessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
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

    private fun cloud() = Cloud(CloudConfig(API, key!!), MemorySessionManager())

    /** Un teléfono con la base de datos real de la app y la nube de verdad. */
    private inner class Phone(val cloud: Cloud = cloud()) {
        val db = buildDatabase(context, null)
        val repo = LevelUpRepository(db) { TimeZone.UTC }
        val service = CloudService(cloud, SyncEngine(db, cloud), repo, kotlinx.coroutines.GlobalScope) {}
    }

    @Test fun signUpSyncAndFriends() = runBlocking {
        assumeTrue("Sin -PsupabaseLocalKey: prueba contra el Supabase local omitida", key != null)
        val anaMail = "ana-$run@levelup.test"
        val betoMail = "beto-$run@levelup.test"

        // Registro: el código de 6 dígitos llega por correo (Mailpit) y confirma la cuenta.
        val a1 = Phone()
        a1.repo.updatePreferences { it.copy(onboardingDone = true, categories = listOf("fisico", "mental"), minutesPerDay = 30) }
        assertTrue(a1.service.signUp(anaMail, "nivel2026").isSuccess)
        assertTrue(a1.service.confirmSignUp(anaMail, codeFor(anaMail)).isSuccess, "confirmar con el código")
        val ana = a1.cloud.currentUserId()!!

        // Lo hecho en un teléfono llega al otro de la misma cuenta.
        val inst = a1.repo.ensureToday().first()
        a1.repo.complete(inst.id)
        assertTrue(a1.service.syncNow().isSuccess)
        val a2 = Phone()
        assertTrue(a2.service.signIn(anaMail, "nivel2026").isSuccess)
        assertTrue(a2.service.syncNow().isSuccess)
        assertEquals(a1.repo.totalXp.first(), a2.repo.totalXp.first())
        assertTrue(a2.repo.prefs().onboardingDone)

        // Otra persona no ve nada de Ana (RLS).
        val beto = cloud()
        beto.signUp(betoMail, "nivel2026")
        beto.confirmSignUp(betoMail, codeFor(betoMail))
        val betoId = beto.currentUserId()!!
        assertTrue(beto.pull(0, 100).isEmpty(), "los datos de Ana no se ven desde otra cuenta")

        // Perfiles, búsqueda y amistad.
        val anaUser = "ana_$run".take(20)
        val betoUser = "beto_$run".take(20)
        a1.cloud.saveProfile(MyProfile(ana, anaUser, "Ana", leaderboardOptIn = true))
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

        // Reacción y avisos (SyncWork lee la fecha del aviso con OffsetDateTime).
        a1.cloud.react(betoId, "aplauso")
        val notices = beto.notices()
        assertTrue(notices.any { it.kind == "reaction" && it.refId == "aplauso" && it.actor?.username == anaUser })
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

        // Clasificación semanal: los dos se apuntaron.
        a1.service.syncNow() // también actualiza racha y marco del perfil (touch_profile)
        val board = a1.cloud.leaderboard()
        assertEquals(setOf(anaUser, betoUser), board.map { it.username }.toSet())
        assertTrue(board.first { it.isMe }.weeklyXp > 0)

        // Borrar la cuenta borra lo del servidor; el teléfono se queda como invitado.
        beto.deleteAccount()
        assertTrue(a1.cloud.friends().isEmpty())
        assertTrue(a1.service.deleteAccount().isSuccess)
        assertEquals(null, a1.cloud.currentUserId())
        assertTrue(a1.repo.totalXp.first() > 0, "lo local sigue en el teléfono")
        listOf(a1, a2).forEach { it.db.close() }
    }

    /** Lee el código de 6 dígitos del correo que recibió [email] en Mailpit. */
    private suspend fun codeFor(email: String): String {
        val query = URLEncoder.encode("to:\"$email\"", "UTF-8")
        repeat(50) {
            val messages = Json.parseToJsonElement(get("$MAILPIT/api/v1/search?query=$query")).jsonObject["messages"]!!.jsonArray
            val id = messages.firstOrNull()?.jsonObject?.get("ID")?.jsonPrimitive?.content
            if (id != null) {
                val text = Json.parseToJsonElement(get("$MAILPIT/api/v1/message/$id")).jsonObject["HTML"]!!.jsonPrimitive.content
                return Regex("""\b(\d{6})\b""").find(text)!!.groupValues[1]
            }
            delay(200)
        }
        error("No llegó el correo a $email")
    }

    private fun get(url: String): String {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        return c.inputStream.use { it.readBytes().decodeToString() }
    }

    private companion object {
        const val API = "http://127.0.0.1:54321"
        const val MAILPIT = "http://127.0.0.1:54324"
    }
}
