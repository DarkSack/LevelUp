package app.levelup.cloud

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.levelup.App
import app.levelup.data.AppDatabase
import app.levelup.data.FakeServer
import app.levelup.data.LevelUpRepository
import app.levelup.data.RemoteRecord
import app.levelup.data.RemoteSync
import app.levelup.data.SyncEngine
import app.levelup.data.SyncRecord
import app.levelup.data.buildDatabase
import app.levelup.ui.FakePlatform
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.account.CloudViewModel
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * La cuenta por sus pantallas, contra el Supabase local: registro con el código del correo, cambios sin conexión que
 * suben al volver, una amistad y borrar la cuenta. La sincronización va a un servidor en memoria que se puede
 * «desconectar»; registro, amigos y borrado, al Supabase local. Solo corre con -PsupabaseLocalKey, como CloudIntegrationTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp")
class AccountUiTest {
    @get:Rule val compose = createComposeRule()

    private val key: String? = System.getProperty("supabaseLocalKey")?.takeIf { it.isNotBlank() }
    private val run = System.currentTimeMillis().toString(36)
    private lateinit var db: AppDatabase
    private var online = true

    @Before fun setUp() {
        assumeTrue("Sin -PsupabaseLocalKey: prueba contra el Supabase local omitida", key != null)
        db = buildDatabase(ApplicationProvider.getApplicationContext<Context>(), null)
        // En Android, supabase-kt carga la sesión cuando la app pasa a primer plano; en Robolectric nadie mueve ese ciclo de vida.
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    /** Espera a [condition]; si no llega, deja la pantalla en el informe de la prueba. */
    private fun until(condition: () -> Boolean) = try {
        compose.waitUntil(20_000, condition)
    } catch (e: Throwable) {
        val roots = compose.onAllNodes(isRoot())
        repeat(roots.fetchSemanticsNodes().size) { println(roots[it].printToString()) }
        throw e
    }

    private fun waitFor(text: String, substring: Boolean = false) =
        until { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    /** Espera a que aparezca (la pantalla puede estar llegando) y lo trae a la vista. */
    private fun node(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        until { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        return compose.onAllNodes(matcher)[0].also { n -> runCatching { n.performScrollTo() } } // en un diálogo no hay nada que desplazar
    }

    private fun type(label: String, value: String) = node(hasSetTextAction() and hasText(label)).performTextInput(value)

    // La acción directa y no un toque: un aviso abajo (snackbar) taparía el botón unos segundos.
    private fun press(label: String) = node(hasText(label, substring = true) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)

    private fun back() = compose.onNodeWithContentDescription("Volver").performClick()

    /** Lo que lanza el cliente de Supabase sin red. */
    private fun offline() = HttpRequestException("sin conexión", HttpRequestBuilder())

    /** Abre una entrada de Perfil (está en una lista: primero se trae a la vista). */
    private fun profileEntry(title: String) {
        compose.onAllNodesWithText("Perfil")[0].performClick()
        waitFor("LevelUp Plus")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
    }

    @Test fun signUpOfflineFriendsAndDeleteThroughTheScreens() {
        val repo = LevelUpRepository(db) { TimeZone.UTC }
        runBlocking {
            repo.updatePreferences { it.copy(onboardingDone = true, displayName = "Ana", categories = listOf("fisico", "mental"), minutesPerDay = 30) }
        }
        val server = FakeServer()
        val remote = object : RemoteSync {
            override suspend fun push(records: List<SyncRecord>) {
                if (!online) throw offline()
                server.client().push(records)
            }

            override suspend fun pull(after: Long, limit: Int): List<RemoteRecord> {
                if (!online) throw offline()
                return server.client().pull(after, limit)
            }
        }
        val cloud = Cloud(CloudConfig(API, key!!, "LevelUp test (Robolectric; Android 15)"), MemorySessionManager())
        val service = CloudService(cloud, SyncEngine(db, remote), repo, GlobalScope) {}
        val platform = FakePlatform()
        val cloudVm = CloudViewModel(service)
        val notices = mutableListOf<String>()
        GlobalScope.launch { cloudVm.messages.collect { notices += it } } // pasan rápido por la pantalla
        val vm = LevelUpViewModel(repo, platform)
        compose.setContent { App(vm, platform, cloudVm) }
        waitFor("Reto principal")

        // Registro con nombre de usuario y contraseña: sin correo ni código, la sesión queda iniciada.
        val login = "ana_ui_$run".take(20)
        profileEntry("Cuenta y sincronización")
        press("Crear una cuenta")
        type("Correo o nombre de usuario", login)
        type("Contraseña", "nivel2026")
        type("Repite la contraseña", "nivel2026")
        press("Crear cuenta")
        waitFor("Sesión iniciada como")
        waitFor(login) // se ve el nombre, no el correo interno
        waitFor("Todo sincronizado.")
        waitFor("Robolectric · Android 15") // la lista de dispositivos

        // Sin conexión: lo hecho se queda pendiente en el teléfono y sube al volver la conexión.
        online = false
        back()
        compose.onNodeWithText("Inicio").performClick()
        waitFor("Todo sincronizado")
        press("Completar")
        waitFor("Seguir")
        press("Seguir")
        waitFor(" por subir", substring = true)
        press("por subir") // la línea de Inicio lleva a la cuenta
        press("Sincronizar ahora")
        waitFor("cambios pendientes de subir", substring = true)
        until { notices.lastOrNull() == "Sin conexión. Inténtalo cuando tengas internet." }
        online = true
        press("Sincronizar ahora")
        waitFor("Todo sincronizado.")
        back()

        // Amigos: Ana crea su perfil social y envía la solicitud; Beto (por la API) la acepta.
        val anaUser = "ana_ui_$run".take(20)
        val betoUser = "beto_ui_$run".take(20)
        val betoMail = "beto-ui-$run@levelup.test"
        val beto = Cloud(CloudConfig(API, key), MemorySessionManager())
        runBlocking {
            beto.signUp(betoMail, "nivel2026")
            beto.saveProfile(MyProfile(beto.currentUserId()!!, betoUser, "Beto"))
        }
        profileEntry("Amigos y grupos")
        waitFor("Crea tu perfil social")
        type("Nombre de usuario", anaUser)
        press("Guardar")
        waitFor("Buscar por nombre de usuario")
        type("Usuario", betoUser)
        press("Buscar")
        waitFor("Beto")
        press("Añadir")
        waitFor("Solicitudes enviadas")
        runBlocking { beto.respondFriendRequest(beto.pendingRequests().single().requestId!!, accept = true) }
        back()
        compose.onNodeWithText("Amigos y grupos").performClick()
        // Al volver se recarga: la solicitud enviada pasa a ser una amistad.
        until { compose.onAllNodesWithText("Solicitudes enviadas").fetchSemanticsNodes().isEmpty() }
        waitFor("Beto")
        back()

        // Borrar la cuenta: dos confirmaciones y el teléfono vuelve a invitado, con su progreso.
        compose.onNodeWithText("Cuenta y sincronización").performClick()
        press("Borrar mi cuenta")
        press("Continuar")
        press("Borrar para siempre")
        waitFor("La cuenta es opcional")
        runBlocking {
            assertTrue(beto.friends().isEmpty(), "la amistad desaparece con la cuenta")
            assertTrue(repo.stats().totalCompleted > 0, "lo hecho sigue en el teléfono")
            beto.deleteAccount()
        }
    }
}
