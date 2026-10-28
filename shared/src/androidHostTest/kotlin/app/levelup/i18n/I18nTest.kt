package app.levelup.i18n

import app.levelup.content.AchievementCatalog
import app.levelup.content.Avatars
import app.levelup.content.Catalog
import app.levelup.content.Categories
import app.levelup.content.Cosmetics
import app.levelup.content.Lab
import app.levelup.content.MissionCatalog
import app.levelup.content.Tags
import app.levelup.domain.XpRules
import app.levelup.ui.Fmt
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class I18nTest {
    @After fun spanish() { I18n.language = "es" }

    private fun inBoth(es: String) = listOf("en", "pt").map { lang -> I18n.language = lang; I18n.get(es) }.also { I18n.language = "es" }

    @Test fun everyEntryKeepsItsPlaceholders() {
        val holes = Regex("""\{\d}""")
        val bad = I18n.keys.filter { es ->
            val want = holes.findAll(es).map { it.value }.sorted().toList()
            inBoth(es).any { it.isBlank() || holes.findAll(it).map { m -> m.value }.sorted().toList() != want }
        }
        assertEquals(emptyList(), bad)
    }

    @Test fun theWholeCatalogIsTranslated() {
        val texts = Catalog.challenges.flatMap { listOfNotNull(it.title, it.description, it.goal, it.doneWhen, it.safety) + it.steps } +
            AchievementCatalog.all.flatMap { listOf(it.name, it.description, it.collection) } +
            MissionCatalog.all.flatMap { listOf(it.title, it.description) } +
            Categories.builtIn.flatMap { listOf(it.name, it.description) } +
            Cosmetics.frames.map { it.name } + XpRules.TITLES.map { it.second } + Tags.places.map { it.second } +
            Lab.all.flatMap { listOf(it.name, it.description) } + Avatars.all.map { it.first }
        assertEquals(emptyList(), texts.filter { it.isNotBlank() && it !in I18n.keys }.distinct())
    }

    /** Busca en el código los textos para personas ("a" + "b" cuenta como uno) y exige su traducción. */
    @Test fun everyTextInTheCodeIsTranslated() {
        val notText = setOf("KotlinNoActualForExpect", "POP") // POP: valor por defecto de la columna style
        val dataFiles = setOf("CsvExport.kt", "Sync.kt") // SQL y columnas del CSV: no son interfaz
        val tagWords = setOf("casa", "exterior", "trabajo", "libre", "sin") // etiquetas del catálogo, no se muestran
        val skipLine = Regex("""^\s*(import|package|//|\*|/\*)|@SerialName|Regex\(|@Query|println\(|error\("|require\(|check(NotNull)?\(""")
        val literal = Regex(""""((?:[^"\\\n]|\\.)*)"""")
        val roots = listOf(File("src/commonMain/kotlin"), File("../androidApp/src/main/kotlin"))
        val missing = roots.flatMap { it.walk().filter { f -> f.extension == "kt" && "i18n" !in f.path && f.name !in dataFiles } }.flatMap { f ->
            val src = f.readText().replace(Regex("\"\"\"[\\s\\S]*?\"\"\""), "\"\"").replace(Regex("\"\\s*\\+\\s*\""), "")
            src.lines().filterNot { skipLine.containsMatchIn(it) }.flatMap { line -> literal.findAll(line).map { it.groupValues[1] } }
                .filter { s -> '$' !in s && human(s) && s !in notText && !s.split(' ').all { it in tagWords } }
                .map { it.replace("\\n", "\n").replace("\\\"", "\"") }
                .filter { it !in I18n.keys }
                .map { "${f.name}: $it" }
        }
        assertEquals(emptyList(), missing)
    }

    private fun human(s: String): Boolean {
        val t = s.trim()
        if (t.none { it.isLetter() } || Regex("[a-z0-9_.\\-/:]+").matches(t)) return false
        return Regex("[áéíóúñÁÉÍÓÚÑ¿¡«»…·→]").containsMatchIn(t) || ' ' in t || t.first().isUpperCase() || t.first() in "¿¡"
    }

    @Test fun datesFollowTheLanguage() {
        val d = LocalDate(2026, 3, 9)
        assertEquals("9 de marzo", Fmt.day(d))
        I18n.language = "en"
        assertEquals("March 9", Fmt.day(d))
        assertEquals("March 9, 2026", Fmt.dayWithYear(d))
        assertEquals("Mo", Fmt.weekdayShort(d.dayOfWeek))
        assertEquals("3 days", Fmt.plural(3, "día", "días"))
        I18n.language = "pt"
        assertEquals("9 de março de 2026", Fmt.dayWithYear(d))
        assertEquals("Nível 3 · Em andamento", tr("Nivel {0} · {1}", 3, "En marcha"))
    }

    @Test fun anUnknownLanguageFallsBackToSpanish() {
        I18n.language = "fr"
        assertEquals("es", I18n.language)
        assertEquals("Nivel 3 · En marcha", tr("Nivel {0} · {1}", 3, "En marcha"))
    }
}
