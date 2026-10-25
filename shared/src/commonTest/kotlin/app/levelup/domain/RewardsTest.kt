package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.content.AchievementCatalog
import app.levelup.content.Categories
import app.levelup.content.Cosmetics
import app.levelup.content.MissionCatalog
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RewardsTest {
    private val all = AchievementCatalog.all

    private fun ids(list: List<Achievement>) = list.map { it.id }.toSet()

    @Test fun firstCompletionUnlocksFirstStepOnly() {
        val new = AchievementRules.newlyUnlocked(all, Stats(totalCompleted = 1, byCategory = mapOf("fisico" to 1)), 15, emptySet())
        assertEquals(setOf("first_step"), ids(new))
    }

    @Test fun alreadyUnlockedIsNeverReturnedAgain() {
        val new = AchievementRules.newlyUnlocked(all, Stats(totalCompleted = 1), 15, setOf("first_step"))
        assertTrue(new.isEmpty())
    }

    @Test fun aSeriesCanAdvanceSeveralStepsInOnePass() {
        val new = AchievementRules.newlyUnlocked(all, Stats(totalCompleted = 10), 200, emptySet())
        assertTrue(ids(new).containsAll(setOf("first_step", "steps_10")))
        assertFalse("steps_50" in ids(new))
    }

    @Test fun achievementXpCanRaiseTheLevelAndUnlockTheLevelAchievement() {
        // 990 XP = nivel 4. "Primer paso" da 10 → 1000 = nivel 5 → "Nivel 5" en la misma pasada.
        val new = AchievementRules.newlyUnlocked(all, Stats(totalCompleted = 1, level = 4), 990, emptySet())
        assertTrue("level_5" in ids(new), ids(new).toString())
    }

    @Test fun states() {
        val steps10 = AchievementCatalog.byId.getValue("steps_10")
        assertEquals(AchievementState.LOCKED, AchievementRules.state(steps10, emptySet()))
        assertEquals(AchievementState.AVAILABLE, AchievementRules.state(steps10, setOf("first_step")))
        assertEquals(AchievementState.UNLOCKED, AchievementRules.state(steps10, setOf("first_step", "steps_10")))
        assertEquals(4, steps10.progress(Stats(totalCompleted = 4)))
        assertEquals(10, steps10.progress(Stats(totalCompleted = 40)))
    }

    @Test fun catalogIsConsistent() {
        assertEquals(all.size, AchievementCatalog.byId.size, "IDs de logros repetidos")
        all.forEach { a ->
            a.requires?.let { assertTrue(it in AchievementCatalog.byId, "${a.id} requiere $it, que no existe") }
            assertTrue(a.target > 0 && a.name.isNotBlank() && a.description.isNotBlank(), a.id)
            assertTrue(a.xpReward in 0..100, a.id)
        }
        Cosmetics.frames.forEach { f ->
            f.unlockedBy?.let {
                assertTrue(it == Cosmetics.PLUS || it in AchievementCatalog.byId || it in MissionCatalog.byId, "${f.id}: $it no existe")
            }
        }
        assertEquals(Cosmetics.frames.size, Cosmetics.byId.size)
        MissionCatalog.all.forEach { m ->
            val g = m.goal
            if (g is MissionGoal.InCategory) assertTrue(Categories.isBuiltIn(g.category), m.id)
            assertTrue(m.days in 3..30 && m.goal.target > 0, m.id)
        }
    }

    @Test fun unlockedCosmetics() {
        val basic = Cosmetics.unlocked(emptySet(), emptySet())
        assertEquals(listOf(Cosmetics.DEFAULT), basic.map { it.id })
        val more = Cosmetics.unlocked(setOf("first_step"), setOf("mis.ritmo")).map { it.id }
        assertTrue(more.containsAll(listOf("frame.bronze", "frame.ember")))
        assertEquals("frame.bronze", Cosmetics.rewardOf("first_step")?.id)
        val withPlus = Cosmetics.unlocked(emptySet(), emptySet(), plus = true).map { it.id }
        assertEquals(5, withPlus.size, "el básico y los 4 marcos de Plus")
    }

    @Test fun missionProgressCountsOnlyInsideItsWindow() {
        val mission = MissionCatalog.byId.getValue("mis.exploracion")
        val run = MissionRun("r", mission.id, MONDAY, MissionRules.endsOn(mission, MONDAY), MissionStatus.ACTIVE)
        assertEquals(MONDAY.plus(6, DateTimeUnit.DAY), run.endsOn)
        val before = inst("a", MONDAY.plus(-1, DateTimeUnit.DAY), category = "social")
        val inside = listOf(inst("b", MONDAY, category = "fisico"), inst("c", MONDAY.plus(2, DateTimeUnit.DAY), category = "fisico"))
        assertEquals(1, MissionRules.progress(mission, run, inside + before))
        val third = inst("d", MONDAY.plus(6, DateTimeUnit.DAY), category = "mental")
        val fourth = inst("e", MONDAY.plus(6, DateTimeUnit.DAY), category = "hogar")
        assertTrue(MissionRules.isComplete(mission, run, inside + third + fourth))
        assertEquals(3, MissionRules.progress(mission, run, inside + third + fourth))
    }

    @Test fun missionsExpireAfterTheirLastDay() {
        val run = MissionRun("r", "mis.hogar", MONDAY, MONDAY.plus(6, DateTimeUnit.DAY), MissionStatus.ACTIVE)
        assertFalse(MissionRules.isExpired(run, MONDAY.plus(6, DateTimeUnit.DAY)))
        assertTrue(MissionRules.isExpired(run, MONDAY.plus(7, DateTimeUnit.DAY)))
        assertFalse(MissionRules.isExpired(run.copy(status = MissionStatus.COMPLETED), MONDAY.plus(30, DateTimeUnit.DAY)))
    }

    @Test fun statsFromHistory() {
        val done = listOf(
            inst("a", MONDAY, category = "fisico"),
            inst("b", MONDAY, category = "mental", simplified = true),
            inst("c", MONDAY.plus(10, DateTimeUnit.DAY), category = "fisico"),
            inst("h", MONDAY, isHabit = true),
            inst("h", MONDAY.plus(7, DateTimeUnit.DAY), isHabit = true),
            inst("skip", MONDAY, status = InstanceStatus.SKIPPED),
        )
        val s = Stats.compute(MONDAY.plus(10, DateTimeUnit.DAY), done, 500, Preferences(), emptyList(), 2, 1)
        assertEquals(5, s.totalCompleted)
        assertEquals(1, s.simplifiedCompleted)
        assertEquals(2, s.distinctCategories)
        assertEquals(1, s.comebacks)
        assertEquals(2, s.habitBestWeeks)
        assertEquals(3, s.level)
        assertEquals(1, s.currentStreak)
        assertEquals(2, s.customCreated)
        assertEquals(1, s.missionsCompleted)
    }
}
