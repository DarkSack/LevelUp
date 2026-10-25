package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.content.Catalog
import app.levelup.content.Categories
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChallengeEngineTest {
    private val prefs = Preferences(
        categories = listOf(Categories.FISICO, Categories.MENTAL, Categories.BIENESTAR),
        priorityCategories = listOf(Categories.MENTAL),
        difficulty = 2,
        minutesPerDay = 30,
        dailyCount = 3,
        onboardingDone = true,
    )

    private fun input(
        p: Preferences = prefs,
        history: List<ChallengeInstance> = emptyList(),
        completedEver: Set<String> = emptySet(),
        challenges: List<Challenge> = Catalog.challenges,
    ) = ChallengeEngine.Input(MONDAY, p, challenges, history, completedEver)

    @Test fun picksRespectCountCategoriesTimeAndDifficulty() {
        repeat(50) { seed ->
            val picks = ChallengeEngine.dailyPicks(input(), Random(seed))
            assertEquals(3, picks.size, "semilla $seed")
            assertEquals(3, picks.map { it.id }.distinct().size)
            assertTrue(picks.all { it.category in prefs.categories })
            assertTrue(picks.sumOf { it.minutes } <= prefs.minutesPerDay, "tiempo: ${picks.map { it.minutes }}")
            assertTrue(picks.all { it.difficulty <= prefs.difficulty + 1 })
        }
    }

    @Test fun mainChallengeComesFromAPriorityCategoryWhenPossible() {
        val picks = (0 until 30).map { ChallengeEngine.dailyPicks(input(), Random(it)) }
        val withPriority = picks.filter { day -> day.any { it.category == Categories.MENTAL } }
        assertTrue(withPriority.isNotEmpty())
        assertTrue(withPriority.all { it.first().category == Categories.MENTAL })
    }

    @Test fun sameSeedSameSelection() {
        assertEquals(
            ChallengeEngine.dailyPicks(input(), Random(7)).map { it.id },
            ChallengeEngine.dailyPicks(input(), Random(7)).map { it.id },
        )
    }

    @Test fun avoidsWhatWasShownInTheLastThreeDays() {
        val first = ChallengeEngine.dailyPicks(input(), Random(1))
        val history = first.map { inst(it.id, MONDAY.minus(1, DateTimeUnit.DAY), InstanceStatus.PENDING, it.category) }
        repeat(20) { seed ->
            val next = ChallengeEngine.dailyPicks(input(history = history), Random(seed))
            assertTrue(next.none { c -> first.any { it.id == c.id } }, "repitió: ${next.map { it.id }}")
        }
    }

    @Test fun challengesSkippedTwiceRecentlyAreLeftOut() {
        val skipped = Catalog.byId.getValue("men.aprender-10")
        val history = listOf(
            inst(skipped.id, MONDAY.minus(5, DateTimeUnit.DAY), InstanceStatus.SKIPPED, skipped.category),
            inst(skipped.id, MONDAY.minus(9, DateTimeUnit.DAY), InstanceStatus.REPLACED, skipped.category),
        )
        repeat(40) { seed ->
            assertTrue(ChallengeEngine.dailyPicks(input(history = history), Random(seed)).none { it.id == skipped.id })
        }
    }

    @Test fun frequencyAndPrerequisitesAreRespected() {
        val once = Challenge("x.once", "Una vez", "", Categories.FISICO, "g", "d", 2, 5, Frequency.ONCE)
        val weekly = Challenge("x.weekly", "Semanal", "", Categories.FISICO, "g", "d", 2, 5, Frequency.WEEKLY)
        val locked = Challenge("x.locked", "Con requisito", "", Categories.FISICO, "g", "d", 2, 5, requires = "x.base")
        val p = prefs.copy(categories = listOf(Categories.FISICO), priorityCategories = emptyList(), dailyCount = 3)
        val pool = listOf(once, weekly, locked)
        val doneThisWeek = listOf(inst("x.weekly", MONDAY))

        val picks = ChallengeEngine.dailyPicks(
            input(p, history = doneThisWeek, completedEver = setOf("x.once", "x.weekly"), challenges = pool), Random(1),
        )
        assertTrue(picks.isEmpty(), "no debería ofrecer ninguno: ${picks.map { it.id }}")

        val unlocked = ChallengeEngine.dailyPicks(input(p, completedEver = setOf("x.base"), challenges = pool), Random(1))
        assertEquals(setOf("x.once", "x.weekly", "x.locked"), unlocked.map { it.id }.toSet())
    }

    @Test fun tinyTimeBudgetStillGivesTheShortestChallenge() {
        val picks = ChallengeEngine.dailyPicks(input(prefs.copy(minutesPerDay = 0, categories = emptyList())), Random(3))
        assertEquals(1, picks.size)
        assertEquals(Catalog.challenges.filter { it.difficulty <= 3 }.minOf { it.minutes }, picks.single().minutes)
    }

    @Test fun neverJumpsMoreThanOneStepAboveTarget() {
        val p = prefs.copy(difficulty = 1, categoryDifficulty = mapOf(Categories.FISICO to 3))
        repeat(40) { seed ->
            ChallengeEngine.dailyPicks(input(p), Random(seed)).forEach {
                assertTrue(it.difficulty <= p.difficultyFor(it.category) + 1, "${it.id} d=${it.difficulty}")
            }
        }
    }

    @Test fun dayKinds() {
        val p = prefs.copy(restDays = Days.of(DayOfWeek.SUNDAY), challengeDays = Days.ALL and Days.bit(DayOfWeek.SATURDAY).inv())
        val sunday = MONDAY.plus(6, DateTimeUnit.DAY)
        val saturday = MONDAY.plus(5, DateTimeUnit.DAY)
        assertEquals(DayKind.ACTIVE, ChallengeEngine.dayKind(MONDAY, p, emptyList()))
        assertEquals(DayKind.REST, ChallengeEngine.dayKind(sunday, p, emptyList()))
        assertEquals(DayKind.OFF, ChallengeEngine.dayKind(saturday, p, emptyList()))
        assertEquals(DayKind.PAUSED, ChallengeEngine.dayKind(MONDAY, p, listOf(Pause("p", MONDAY.minus(2, DateTimeUnit.DAY)))))
    }

    @Test fun habitsOnlyOnTheirDays() {
        val habit = Challenge("h", "Leer", "", Categories.MENTAL, "g", "d", 1, 10, source = ChallengeSource.CUSTOM,
            habitDays = Days.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(listOf(habit), ChallengeEngine.habitsFor(MONDAY, listOf(habit)))
        assertTrue(ChallengeEngine.habitsFor(MONDAY.plus(1, DateTimeUnit.DAY), listOf(habit)).isEmpty())
        // Un hábito nunca entra en la selección aleatoria: tiene sus propios días.
        val picks = ChallengeEngine.dailyPicks(input(prefs.copy(categories = listOf(Categories.MENTAL)), challenges = listOf(habit)), Random(1))
        assertTrue(picks.isEmpty())
    }

    @Test fun replacementIsSameCategoryNotHarderAndNew() {
        val current = inst("fis.caminar-1km", MONDAY, InstanceStatus.PENDING, Categories.FISICO, difficulty = 2)
        val today = setOf("fis.caminar-1km", "men.leer-10")
        repeat(30) { seed ->
            val r = assertNotNull(ChallengeEngine.replacementFor(input(), current, today, Random(seed)))
            assertEquals(Categories.FISICO, r.category)
            assertTrue(r.difficulty <= 2)
            assertTrue(r.id !in today)
        }
    }
}
