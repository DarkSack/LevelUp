package app.levelup.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import app.levelup.App
import app.levelup.Platform
import app.levelup.PlusState
import kotlinx.coroutines.flow.MutableStateFlow
import app.levelup.data.AppDatabase
import app.levelup.data.LevelUpRepository
import app.levelup.data.TestClock
import app.levelup.data.buildDatabase
import app.levelup.domain.Preferences
import app.levelup.ui.welcome.WelcomeScreen
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

internal class FakePlatform(override val systemReducedMotion: Boolean = true) : Platform {
    override val appVersion = "test"
    override suspend fun exportFile(fileName: String, content: String, mimeType: String) = true
    override suspend fun importFile(): String? = null
    override fun notificationsAllowed() = true
    override suspend fun requestNotifications() = true
    override fun remindersChanged() {}
    override val plus = MutableStateFlow(PlusState())
    override fun buyPlus() {}
    override fun refreshPlus() {}
    override val deepLinks = MutableStateFlow<String?>(null)
    override fun consumeDeepLink() { deepLinks.value = null }
}

/** La app entera en la JVM: Room en memoria, interfaz real, sin teléfono. */
@RunWith(RobolectricTestRunner::class)
// Tamaño de un teléfono normal: con la pantalla por defecto (320×470) los botones quedan bajo la barra de pestañas.
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp")
class AppUiTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: LevelUpRepository

    @Before fun setUp() {
        db = buildDatabase(context, null)
        repo = LevelUpRepository(db, TestClock()) { TimeZone.UTC }
    }

    @After fun tearDown() = db.close()

    private fun launch(platform: Platform = FakePlatform()) {
        val vm = LevelUpViewModel(repo, platform)
        compose.setContent { App(vm, platform) }
    }

    // Room responde en otro hilo: esperar a que el texto aparezca en vez de suponer que ya está.
    private fun waitForText(text: String, substring: Boolean = false) = try {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    } catch (e: Throwable) {
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        repeat(roots.fetchSemanticsNodes().size) { println(roots[it].printToString()) } // queda en el informe de la prueba
        throw e
    }

    private fun prefs(change: (Preferences) -> Preferences) = runBlocking { repo.updatePreferences(change) }

    private fun onboarded() = prefs {
        it.copy(onboardingDone = true, categories = listOf("fisico", "mental", "hogar"), minutesPerDay = 30)
    }

    @Test fun welcomeLeadsToTheFirstOnboardingStep() {
        launch()
        waitForText("Cada pequeño paso cuenta")
        compose.onNodeWithText("Comenzar").performClick()
        waitForText("Paso 1 de 6")
        assertEquals(1, runBlocking { repo.prefs().onboardingStep })
    }

    @Test fun skipAnimationEndsTheWelcomeAtOnce() {
        // Reloj quieto: si avanzara solo, la animación terminaría antes de poder pulsar "Omitir".
        compose.mainClock.autoAdvance = false
        compose.setContent { WelcomeScreen(onStart = {}) }
        compose.onNodeWithText("Omitir animación").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, compose.onAllNodesWithText("Omitir animación").fetchSemanticsNodes().size)
        compose.onNodeWithText("Comenzar").assertExists()
    }

    @Test fun onboardingResumesWhereItWasLeft() {
        prefs { it.copy(onboardingStep = 4) }
        launch()
        waitForText("Tu ritmo")
        compose.onNodeWithText("Omitir").performClick()
        waitForText("Paso 5 de 6")
    }

    @Test fun completingTheMainChallengeCelebratesTheFirstAchievement() {
        onboarded()
        launch()
        waitForText("Reto principal")
        compose.onNodeWithText("Completar").performClick()
        waitForText("¡Hecho!", substring = true)
        waitForText("¡Logro desbloqueado!")
        compose.onNodeWithText("Seguir").performClick()
        assertEquals(1, runBlocking { repo.stats().totalCompleted })
    }

    @Test fun accessibilityTogglesAreSaved() {
        onboarded()
        launch()
        waitForText("Perfil")
        compose.onNodeWithText("Perfil").performClick()
        waitForText("Accesibilidad")
        // Al final de la lista, para que la entrada no quede debajo de la barra de pestañas.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Acerca de LevelUp", substring = true))
        compose.onNodeWithText("Accesibilidad").performClick()
        waitForText("Modo sencillo")
        compose.onNodeWithText("Modo sencillo").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { repo.prefs().simpleMode } }
    }

    @Test fun withoutCloudProfileOffersNoAccountNorFriends() {
        onboarded()
        launch() // sin CloudViewModel, como una compilación con cuenta=no
        waitForText("Perfil")
        compose.onNodeWithText("Perfil").performClick()
        waitForText("LevelUp Plus")
        assertEquals(0, compose.onAllNodesWithText("Cuenta y sincronización").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Amigos y grupos").fetchSemanticsNodes().size)
    }

    @Test fun withoutGameThereIsNoXpNorLevels() {
        onboarded()
        prefs { it.copy(gamification = app.levelup.domain.GamificationStyle.OFF) }
        launch()
        waitForText("Reto principal")
        assertEquals(0, compose.onAllNodesWithText("XP", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Nivel ", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Completar").performClick()
        waitForText("¡Hecho!")
    }

    @Test fun linkToAChallengeThatIsGoneSaysSo() {
        onboarded()
        val platform = FakePlatform()
        launch(platform)
        waitForText("Reto principal")
        platform.deepLinks.value = "reto/de-otro-dia"
        waitForText("Este reto ya no está")
        compose.onNodeWithText("Ir a Inicio").performClick()
        waitForText("Reto principal")
    }

    @Test fun searchFromHomeFindsCatalogChallenges() {
        onboarded()
        launch()
        waitForText("Reto principal")
        compose.onNodeWithContentDescription("Buscar").performClick()
        waitForText("Retos, hábitos, logros, metas o notas")
        compose.onNode(hasSetTextAction()).performTextInput("camina 10")
        waitForText("Camina 10 minutos")
    }

    @Test fun plusShowsTheStorePriceAndUnlocksColors() {
        onboarded()
        val platform = FakePlatform().apply { plus.value = PlusState(price = "MX\$59.00") }
        launch(platform)
        waitForText("Perfil")
        compose.onNodeWithText("Perfil").performClick()
        waitForText("LevelUp Plus")
        compose.onNodeWithText("LevelUp Plus").performClick()
        waitForText("Comprar por MX\$59.00")

        platform.plus.value = PlusState(owned = true)
        waitForText("Color de la app")
        compose.onNodeWithText("Violeta").performScrollTo().performClick()
        compose.waitUntil(5_000) { runBlocking { repo.prefs().accent } == "violeta" }
    }
}
