package app.levelup.content

import app.levelup.domain.Frequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Reglas de calidad del contenido: instrucciones claras, objetivos alcanzables y finalización comprensible. */
class CatalogTest {
    private val all = Catalog.challenges

    @Test fun idsAreUniqueAndPrefixedByCategory() {
        assertEquals(all.size, Catalog.byId.size, "IDs repetidos")
        val prefixes = mapOf(
            Categories.FISICO to "fis.", Categories.SOCIAL to "soc.", Categories.MENTAL to "men.",
            Categories.PRODUCTIVIDAD to "pro.", Categories.BIENESTAR to "bie.", Categories.CREATIVIDAD to "cre.",
            Categories.FINANZAS to "fin.", Categories.DIGITAL to "dig.", Categories.HOGAR to "hog.",
        )
        all.forEach { assertTrue(it.id.startsWith(prefixes.getValue(it.category)), it.id) }
    }

    @Test fun everyCategoryIsUsefulOfflineFromDayOne() {
        assertTrue(all.size >= 120, "catálogo pequeño: ${all.size}")
        Categories.builtIn.forEach { cat ->
            val inCat = all.filter { it.category == cat.key }
            assertTrue(inCat.size >= 12, "${cat.key}: solo ${inCat.size} retos")
            assertTrue(inCat.count { it.difficulty == 1 && it.minutes <= 5 } >= 2, "${cat.key}: faltan retos de 5 minutos")
            (1..3).forEach { d -> assertTrue(inCat.any { it.difficulty == d }, "${cat.key}: sin dificultad $d") }
        }
    }

    @Test fun textsAreClearAndShort() {
        all.forEach { c ->
            assertTrue(c.title.length in 5..40, "${c.id}: título de ${c.title.length} caracteres")
            assertTrue(c.goal.length in 15..140, "${c.id}: objetivo de ${c.goal.length}")
            assertTrue(c.doneWhen.length in 10..120, "${c.id}: criterio de ${c.doneWhen.length}")
            assertTrue(c.description.isNotBlank(), "${c.id}: sin descripción")
            assertTrue(c.goal.trimEnd().endsWith('.') && c.doneWhen.trimEnd().endsWith('.'), "${c.id}: frases sin punto")
            assertTrue(c.steps.size <= 6 && c.steps.all { it.length <= 80 }, "${c.id}: pasos largos")
        }
    }

    @Test fun goalsAreReachable() {
        all.forEach { c ->
            assertTrue(c.difficulty in 1..5, c.id)
            assertTrue(c.minutes in 1..180, "${c.id}: ${c.minutes} min")
            if (c.minutes > 60) assertEquals(Frequency.WEEKLY, c.frequency, "${c.id}: retos largos, como mucho semanales")
        }
    }

    @Test fun everyNonTrivialChallengeHasASmallerVersion() {
        all.filter { it.difficulty >= 2 }.forEach { c ->
            val simpler = Catalog.byId[c.simplerId] ?: fail("${c.id}: sin versión sencilla")
            assertEquals(c.category, simpler.category, "${c.id} → ${simpler.id}")
            assertTrue(simpler.difficulty < c.difficulty, "${c.id} → ${simpler.id} no es más fácil")
        }
        all.filter { it.difficulty == 1 }.forEach { assertEquals(null, it.simplerId, it.id) }
    }

    @Test fun prerequisitesExistAndAreEasier() {
        all.mapNotNull { c -> c.requires?.let { c to it } }.forEach { (c, req) ->
            val r = Catalog.byId[req] ?: fail("${c.id} requiere $req, que no existe")
            assertTrue(r.difficulty < c.difficulty, c.id)
        }
    }

    @Test fun physicalChallengesCarrySafetyNotes() {
        all.filter { it.category == Categories.FISICO && it.difficulty >= 2 }.forEach {
            assertTrue(!it.safety.isNullOrBlank(), "${it.id}: sin indicación de seguridad")
        }
    }
}
