package app.levelup.content

import app.levelup.pets.Accessory
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetsCatalogTest {
    @Test fun seasonsIncludeTheirEdgesAndCrossNewYear() {
        assertTrue(Pets.Season.MUERTOS.has(LocalDate(2026, 10, 25)))
        assertTrue(Pets.Season.MUERTOS.has(LocalDate(2026, 11, 5)))
        assertFalse(Pets.Season.MUERTOS.has(LocalDate(2026, 11, 6)))
        assertTrue(Pets.Season.NAVIDAD.has(LocalDate(2026, 12, 31)))
        assertTrue(Pets.Season.NAVIDAD.has(LocalDate(2027, 1, 6)))
        assertFalse(Pets.Season.NAVIDAD.has(LocalDate(2027, 1, 7)))
        assertFalse(Pets.Season.NAVIDAD.has(LocalDate(2027, 11, 30)))
    }

    @Test fun seasonalAccessoriesOnlyOpenInTheirSeason() {
        val muertos = Pets.gatesOpen(emptySet(), LocalDate(2026, 11, 2))
        val summer = Pets.gatesOpen(emptySet(), LocalDate(2026, 7, 2))
        assertTrue(Accessory.CATRINA in muertos)
        assertFalse(Accessory.CATRINA in summer)
        assertFalse(Accessory.RENO in muertos)
        assertTrue(Accessory.GORRA in summer)
    }

    @Test fun everyAccessoryIsInTheCatalogOnce() {
        assertEquals(Accessory.entries.toSet(), Pets.accessories.map { it.accessory }.toSet())
        assertEquals(Accessory.entries.size, Pets.accessories.size)
    }
}
