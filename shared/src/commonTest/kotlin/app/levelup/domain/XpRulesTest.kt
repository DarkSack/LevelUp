package app.levelup.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class XpRulesTest {
    @Test fun xpGrowsWithDifficultyAndIsClamped() {
        assertEquals(listOf(10, 15, 25, 40, 60), (1..5).map { XpRules.xpFor(it) })
        assertEquals(10, XpRules.xpFor(0))
        assertEquals(60, XpRules.xpFor(9))
    }

    @Test fun levelCurveMatchesTheDocumentedFormula() {
        assertEquals(listOf(0, 100, 300, 600, 1000), (1..5).map { XpRules.totalForLevel(it) })
        assertEquals(1, XpRules.levelFor(0))
        assertEquals(1, XpRules.levelFor(99))
        assertEquals(2, XpRules.levelFor(100))
        assertEquals(2, XpRules.levelFor(299))
        assertEquals(3, XpRules.levelFor(300))
        assertEquals(5, XpRules.levelFor(1000))
        assertEquals(1, XpRules.levelFor(-50))
    }

    @Test fun levelForIsExactAtEveryBorderUpToLevel200() {
        for (level in 1..200) {
            val start = XpRules.totalForLevel(level)
            assertEquals(level, XpRules.levelFor(start), "inicio del nivel $level")
            if (level > 1) assertEquals(level - 1, XpRules.levelFor(start - 1), "justo antes del nivel $level")
        }
    }

    @Test fun progressInsideLevel() {
        val p = XpRules.progress(150)
        assertEquals(2, p.level)
        assertEquals(50, p.xpInLevel)
        assertEquals(200, p.xpForNext)
        assertEquals(0.25f, p.fraction)
    }

    @Test fun categoryCurveUsesItsOwnStep() {
        assertEquals(2, XpRules.levelFor(50, XpRules.CATEGORY_STEP))
        assertEquals(1, XpRules.levelFor(49, XpRules.CATEGORY_STEP))
    }

    @Test fun titlesFollowLevel() {
        assertEquals("Primeros pasos", XpRules.titleFor(1))
        assertEquals("En marcha", XpRules.titleFor(4))
        assertEquals("Leyenda tranquila", XpRules.titleFor(99))
        assertEquals(3 to "En marcha", XpRules.nextTitle(1))
        assertEquals(null, XpRules.nextTitle(30))
    }
}
