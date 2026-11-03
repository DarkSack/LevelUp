package app.levelup.store

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.levelup.pets.Accessory
import app.levelup.pets.FlameTier
import app.levelup.pets.PetFrame
import app.levelup.pets.PetMood
import app.levelup.pets.STILL
import app.levelup.pets.Species
import app.levelup.pets.StreakFlame
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Hoja con todas las mascotas, ánimos, accesorios y llamas, para revisarlas a ojo. Solo con -Pcapturas:
 * `./gradlew :shared:testAndroidHostTest --tests "app.levelup.store.PetSheet" -Pcapturas=<carpeta> --rerun`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = "w880dp-h1600dp-mdpi")
class PetSheet {
    @get:Rule val compose = createComposeRule()

    @Test fun sheet() {
        val out = System.getProperty("levelup.capturas").orEmpty()
        assumeTrue("Solo con -Pcapturas=<carpeta>", out.isNotEmpty())
        val cell = Modifier.size(96.dp).padding(3.dp)
        compose.setContent {
            Column(Modifier.background(Color(0xFFFFF8EC)).padding(8.dp)) {
                // Cada ánimo en reposo, y un fotograma saltando y uno tocándola.
                Species.entries.forEach { s ->
                    Row { PetMood.entries.forEach { m -> PetFrame(s, m, STILL, cell) }; PetFrame(s, PetMood.PARTY, 0.06f, cell); PetFrame(s, PetMood.CALM, 0.4f, cell, poke = 0.5f) }
                }
                Accessory.entries.chunked(8).forEach { row ->
                    Row { row.forEach { a -> PetFrame(Species.TLACUACHE, PetMood.HAPPY, STILL, cell, setOf(a)) } }
                }
                val combos = listOf(
                    setOf(Accessory.MAGO, Accessory.CORAZONES, Accessory.COLLAR), setOf(Accessory.FLOR, Accessory.ANTIFAZ, Accessory.PALIACATE),
                    setOf(Accessory.CORONA_FUEGO, Accessory.BIGOTE, Accessory.CADENA), setOf(Accessory.VIKINGO, Accessory.REDONDOS, Accessory.CORBATIN),
                    setOf(Accessory.HALO, Accessory.MONOCULO, Accessory.MARIPOSA), setOf(Accessory.AUDIFONOS, Accessory.LENTES, Accessory.GLOBO),
                    setOf(Accessory.NAVIDAD, Accessory.CASCABEL, Accessory.LUCIERNAGAS), setOf(Accessory.COPA, Accessory.BIGOTE, Accessory.MEDALLA),
                )
                Row { Species.entries.drop(1).zip(combos).forEach { (s, c) -> PetFrame(s, PetMood.CALM, STILL, cell, c) } }
                Row { StreakFlame(null, cell, animated = false); FlameTier.entries.forEach { t -> StreakFlame(t, cell, animated = false) } }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(out, "mascotas.png").also { it.parentFile.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
