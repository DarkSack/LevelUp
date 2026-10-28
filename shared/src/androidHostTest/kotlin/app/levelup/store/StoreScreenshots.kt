package app.levelup.store

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import app.levelup.domain.AchievementState
import app.levelup.domain.AppStyle
import app.levelup.domain.Preferences
import app.levelup.content.AchievementCatalog
import app.levelup.ui.components.TrophyBadge
import app.levelup.ui.theme.LevelUpTheme
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.levelup.App
import app.levelup.content.MissionCatalog
import app.levelup.data.AppDatabase
import app.levelup.data.LevelUpRepository
import app.levelup.data.TestClock
import app.levelup.data.buildDatabase
import app.levelup.ui.FakePlatform
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.components.LevelUpLogo
import app.levelup.ui.theme.Brand
import app.levelup.ui.welcome.WELCOME_ANIMATION_MS
import app.levelup.ui.welcome.WelcomeScreen
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.time.Instant

/**
 * Capturas para la ficha de Google Play: la app real con tres semanas inventadas, a 1080 × 1920.
 * No corre con las pruebas normales:
 * `./gradlew :shared:testAndroidHostTest --tests "app.levelup.store.StoreScreenshots" -Pcapturas=E:/Dev/levelup/docs/play/capturas --rerun`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshots {
    @get:Rule val compose = createComposeRule()

    private val out = System.getProperty("levelup.capturas").orEmpty()
    private val clock = TestClock(Instant.parse("2026-10-05T15:00:00Z"))
    private lateinit var db: AppDatabase
    private lateinit var repo: LevelUpRepository

    @Before fun setUp() = runBlocking {
        assumeTrue("Solo con -Pcapturas=<carpeta>", out.isNotEmpty())
        db = buildDatabase(ApplicationProvider.getApplicationContext(), null)
        repo = LevelUpRepository(db, clock) { TimeZone.UTC }
        seed()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    /** Una persona inventada: tres semanas haciendo dos retos casi cada día, hoy uno hecho y una meta. */
    private suspend fun seed() {
        repo.updatePreferences {
            it.copy(onboardingDone = true, displayName = "Ana", categories = listOf("fisico", "mental", "bienestar", "hogar"), minutesPerDay = 30)
        }
        // Una misión en marcha desde la segunda semana, para que Logros la muestre.
        repeat(21) { day ->
            if (day == 10) repo.startMission(MissionCatalog.all.first().id)
            val today = repo.ensureToday()
            if (day % 7 != 3) today.take(2).forEach { repo.complete(it.id) } // un día libre por semana
            clock.advanceDays(1)
        }
        repo.ensureToday().first().let { repo.complete(it.id) }
        repo.saveGoal(null, "Correr 5 km sin parar", "Para tener más energía", LocalDate(2026, 12, 15))
        repo.saveGoal(null, "Leer 12 libros este año", "Me gusta y lo dejé", null)
    }

    private fun waitFor(text: String) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun launch() {
        val platform = FakePlatform()
        val vm = LevelUpViewModel(repo, platform)
        compose.setContent { App(vm, platform) }
        waitFor("Nivel ") // Room ya entregó el progreso
    }

    private fun save(name: String) {
        compose.waitForIdle()
        write(name, compose.onRoot().captureToImage().asAndroidBitmap())
    }

    private fun write(name: String, bitmap: Bitmap) {
        val file = File(out, "$name.png").also { it.parentFile.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("Captura $name: ${bitmap.width}x${bitmap.height}")
    }

    /** Pulsa la pestaña y espera a su pantalla: su título repite el nombre de la pestaña. */
    private fun tab(label: String) {
        compose.onAllNodesWithText(label)[0].performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().size >= 2 }
    }

    @Test fun inicio() {
        launch()
        save("1-inicio")
    }

    @Test fun retos() {
        launch()
        tab("Retos")
        save("2-retos")
    }

    @Test fun progreso() {
        launch()
        tab("Progreso")
        // Un poco hacia arriba, para que el gráfico semanal no quede bajo la barra de pestañas (sin inercia).
        compose.onNode(hasScrollToNodeAction()).performTouchInput { swipeUp(startY = centerY, endY = centerY - 450f, durationMillis = 1_000) }
        save("3-progreso")
    }

    /** Inicio y trofeos en cada estilo, para revisar el aspecto (no van a la ficha). */
    @Test fun estilos() {
        launch()
        AppStyle.entries.forEach { style ->
            runBlocking { repo.updatePreferences { it.copy(style = style) } }
            tab("Inicio")
            save("estilo-${style.name.lowercase()}-inicio")
            tab("Progreso")
            compose.onNodeWithText("Logros y trofeos").performClick()
            waitFor(" trofeos")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Desbloqueado", substring = true))
            save("estilo-${style.name.lowercase()}-logros")
            compose.onNodeWithContentDescription("Volver").performClick()
        }
    }

    /** Trofeos de varias colecciones y categorías, en sus tres estados, en cada estilo. */
    @Test fun trofeos() {
        var style by mutableStateOf(AppStyle.POP)
        val byId = AchievementCatalog.all.associateBy { it.id }
        compose.setContent {
            LevelUpTheme(Preferences(style = style), systemReducedMotion = false) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        listOf("first_step", "streak_7", "streak_30", "steps_250") to AchievementState.UNLOCKED,
                        listOf("affinity_fisico", "mastery_creatividad", "explore_9", "mastery_all") to AchievementState.UNLOCKED,
                        listOf("affinity_finanzas", "level_10", "week_12", "level_30") to AchievementState.AVAILABLE,
                        listOf("affinity_hogar", "mastery_digital", "mission_15", "secret_year") to AchievementState.LOCKED,
                    ).forEach { (ids, state) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ids.forEach { TrophyBadge(byId.getValue(it), state, 72.dp) }
                        }
                    }
                }
            }
        }
        AppStyle.entries.forEach {
            style = it
            save("estilo-trofeos-${it.name.lowercase()}")
        }
    }

    @Test fun logros() {
        launch()
        tab("Progreso")
        compose.onNodeWithText("Logros y trofeos").performClick()
        waitFor(" trofeos")
        // Las misiones van primero; para la tienda, los trofeos ganados.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Desbloqueado", substring = true))
        save("4-logros")
    }

    /**
     * La bienvenida en varios instantes de su animación (para revisar que cada fase entra en orden) y, al final,
     * la captura de la ficha.
     */
    @Test fun bienvenida() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LevelUpTheme(Preferences(), systemReducedMotion = false) { WelcomeScreen(onStart = {}) } }
        var at = 0
        listOf(0, 250, 500, 800, 1100).forEach { ms ->
            compose.mainClock.advanceTimeBy((ms - at).toLong())
            at = ms
            drawNow("animacion/bienvenida-$ms-ms")
        }
        compose.mainClock.advanceTimeBy(WELCOME_ANIMATION_MS.toLong())
        drawNow("0-bienvenida")
    }

    /** Dibuja la vista tal como está: captureToImage espera un redibujado que con el reloj parado no llega. */
    private fun drawNow(name: String) {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        write(name, bitmap)
    }

    /** Icono de la ficha: 512 × 512 a sangre (Play le pone sus propias esquinas). El mismo dibujo que el logo de la app. */
    @Test @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun icono() {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Brand.Teal)) { LevelUpLogo(Modifier.fillMaxSize()) }
        }
        save("icono-512")
    }

    /** Gráfico destacado: 1024 × 500, lo importante lejos de los bordes (Play lo recorta en algunos sitios). */
    @Test @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun grafico() {
        compose.setContent {
            Row(Modifier.fillMaxSize().background(Brand.Teal).padding(horizontal = 120.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelUpLogo(Modifier.size(220.dp))
                Spacer(Modifier.width(48.dp))
                Column {
                    Text("LevelUp", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                    Text("Retos pequeños para crear hábitos.\nSube de nivel a tu ritmo.", color = Color.White, fontSize = 30.sp, lineHeight = 40.sp)
                }
            }
        }
        save("grafico-1024x500")
    }

    @Test fun metas() {
        launch()
        tab("Perfil")
        compose.onNodeWithText("Mis metas").performClick()
        waitFor("Correr 5 km")
        save("5-metas")
    }
}
