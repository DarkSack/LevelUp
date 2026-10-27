package app.levelup.wear

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.wear.compose.material3.MaterialTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Capturas del reloj para la ficha de Play (Wear OS pide cuadradas, de 384 × 384 como mínimo): 454 × 454.
 * No corre con las pruebas normales:
 * `./gradlew :wearApp:testDebugUnitTest --tests "app.levelup.wear.WatchScreenshots" -Pcapturas=E:/Dev/levelup/docs/play/capturas --rerun`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = "es-w227dp-h227dp-round-xhdpi")
class WatchScreenshots {
    @get:Rule val compose = createComposeRule()

    private val out = System.getProperty("levelup.capturas").orEmpty()

    private val today = Today(
        date = LocalDate.now().toString(), mainId = "a", mainTitle = "Camina 10 minutos", done = 1, total = 3,
        items = listOf(
            Today.Item("a", "Camina 10 minutos", done = true),
            Today.Item("b", "Lee 10 páginas", done = false),
            Today.Item("c", "Ordena tu escritorio", done = false),
        ),
        goals = listOf("Correr 5 km sin parar"),
    )

    private fun save(name: String) {
        assumeTrue("Solo con -Pcapturas=<carpeta>", out.isNotEmpty())
        compose.waitForIdle()
        val file = File(out, "$name.png").also { it.parentFile.mkdirs() }
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun hoy() {
        compose.setContent { MaterialTheme { WatchScreen(today, null) {} } }
        compose.onNodeWithText("1 de 3 hechos").assertExists()
        save("reloj-1-hoy")
    }

    @Test fun sinTelefono() {
        compose.setContent { MaterialTheme { WatchScreen(null, R.string.no_connection) {} } }
        compose.onNodeWithText("Abre LevelUp en el teléfono para ver tus retos de hoy.").assertExists()
        save("reloj-2-sin-datos")
    }
}
