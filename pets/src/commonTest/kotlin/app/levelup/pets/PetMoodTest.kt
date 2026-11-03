package app.levelup.pets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PetMoodTest {
    @Test fun moodFollowsTheStreak() {
        assertEquals(PetMood.SLEEPY, PetMood.of(streak = 0, doneToday = false))
        assertEquals(PetMood.SAD, PetMood.of(streak = 0, doneToday = false, hadStreak = true), "la racha se apagó")
        assertEquals(PetMood.CALM, PetMood.of(streak = 3, doneToday = false, hadStreak = true))
        assertEquals(PetMood.HAPPY, PetMood.of(streak = 1, doneToday = true, hadStreak = true))
        assertEquals(PetMood.HAPPY, PetMood.of(streak = 6, doneToday = true))
        assertEquals(PetMood.PARTY, PetMood.of(streak = 7, doneToday = true))
    }

    @Test fun flameGrowsWithTheStreak() {
        assertNull(FlameTier.of(0))
        assertEquals(FlameTier.NARANJA, FlameTier.of(2))
        assertEquals(FlameTier.ROJA, FlameTier.of(3))
        assertEquals(FlameTier.AZUL, FlameTier.of(29))
        assertEquals(FlameTier.VIOLETA, FlameTier.of(30))
        assertEquals(FlameTier.ARCOIRIS, FlameTier.of(299))
        assertEquals(FlameTier.DORADA, FlameTier.of(300))
        assertEquals(FlameTier.COSMICA, FlameTier.of(999))
        assertEquals(FlameTier.LEGENDARIA, FlameTier.of(1000))
        assertEquals(FlameTier.AZUL, FlameTier.ROJA.next)
        assertNull(FlameTier.LEGENDARIA.next)
    }

    @Test fun oneAccessoryPerSlot() {
        val worn = emptySet<Accessory>().toggle(Accessory.MONO).toggle(Accessory.LENTES).toggle(Accessory.CORONA)
        assertEquals(setOf(Accessory.CORONA, Accessory.LENTES), worn, "la corona reemplaza al moño")
        assertEquals(setOf(Accessory.CORONA), worn.toggle(Accessory.LENTES), "tocarlo de nuevo lo quita")
        assertEquals(worn, Accessory.parse(Accessory.format(worn)))
        assertEquals(setOf(Accessory.MONO), Accessory.parse("mono,dragón,"))
    }

    @Test fun speciesByKey() {
        assertEquals(Species.TLACUACHE, Species.byKey("tlacuache"))
        assertNull(Species.byKey("dragón"))
    }

    @Test fun stillFrameHasEyesOpenAndNoHop() {
        val pose = Pose(STILL, PetMood.HAPPY)
        assertEquals(1f, pose.eyeOpen, 0.001f)
        assertEquals(false, pose.happyEyes)
        assertEquals(0f, pose.hop, 0.001f)
    }
}
