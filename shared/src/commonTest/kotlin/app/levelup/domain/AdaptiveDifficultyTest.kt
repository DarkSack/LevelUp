package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveDifficultyTest {
    private val prefs = Preferences(difficulty = 2, difficultyMode = DifficultyMode.SUGGEST)

    private fun days(n: Int, status: InstanceStatus, rating: Rating? = null, simplified: Boolean = false) =
        (1..n).map { inst("c$it-$status-$rating", MONDAY.minus(it, DateTimeUnit.DAY), status, "fisico", 2, rating, simplified) }

    private fun suggest(history: List<ChallengeInstance>, p: Preferences = prefs) =
        AdaptiveDifficulty.suggest("fisico", p, history, MONDAY)

    @Test fun manualModeNeverSuggests() {
        assertNull(suggest(days(8, InstanceStatus.COMPLETED, Rating.EASY), prefs.copy(difficultyMode = DifficultyMode.MANUAL)))
    }

    @Test fun easyAndConsistentSuggestsOneStepUp() {
        val s = suggest(days(6, InstanceStatus.COMPLETED, Rating.EASY))
        assertEquals(AdaptiveDifficulty.Suggestion("fisico", 2, 3), s)
        assertTrue(s!!.isUp)
    }

    @Test fun manyCompletionsWithoutRatingsAlsoSuggestUp() {
        assertEquals(3, suggest(days(10, InstanceStatus.COMPLETED))?.to)
        assertNull(suggest(days(6, InstanceStatus.COMPLETED)))
    }

    @Test fun aSingleSkipNeverChangesAnything() {
        assertNull(suggest(days(1, InstanceStatus.SKIPPED)))
        assertNull(suggest(days(4, InstanceStatus.COMPLETED) + days(1, InstanceStatus.SKIPPED)))
    }

    @Test fun inactivityIsNotFailure() {
        assertNull(suggest(days(12, InstanceStatus.PENDING)))
    }

    @Test fun repeatedHardRatingsSuggestOneStepDown() {
        assertEquals(1, suggest(days(5, InstanceStatus.COMPLETED, Rating.HARD))?.to)
    }

    @Test fun manyDropsSuggestDown() {
        val history = days(2, InstanceStatus.COMPLETED) + days(4, InstanceStatus.REPLACED)
        assertEquals(1, suggest(history)?.to)
    }

    @Test fun cooldownAfterAChange() {
        val recent = prefs.copy(lastDifficultyChange = MONDAY.minus(3, DateTimeUnit.DAY))
        assertNull(suggest(days(8, InstanceStatus.COMPLETED, Rating.EASY), recent))
    }

    @Test fun staysWithinOneToFive() {
        assertNull(suggest(days(8, InstanceStatus.COMPLETED, Rating.EASY), prefs.copy(difficulty = 5)))
        assertNull(suggest(days(8, InstanceStatus.COMPLETED, Rating.HARD), prefs.copy(difficulty = 1)))
    }
}
