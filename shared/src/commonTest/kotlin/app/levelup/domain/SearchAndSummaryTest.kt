package app.levelup.domain

import app.levelup.MONDAY
import app.levelup.content.AchievementCatalog
import app.levelup.content.Catalog
import app.levelup.data.CsvExport
import app.levelup.inst
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchAndSummaryTest {
    private fun goal(id: String, title: String, status: GoalStatus = GoalStatus.ACTIVE, createdAt: Long = 1) =
        PersonalGoal(id, title, "porque sí", null, status, createdAt, createdAt)

    @Test fun searchIgnoresAccentsAndCaseAndCoversEverything() {
        val noted = inst("a", MONDAY).copy(note = "Me costó la RESPIRACIÓN")
        val hits = Search.run("respiracion", Catalog.challenges, AchievementCatalog.all, emptyList(), listOf(noted))
        assertTrue(hits.any { it.kind == HitKind.NOTE && it.id == noted.id && it.date == MONDAY })
        val moves = Search.run("MUEVETE", Catalog.challenges, AchievementCatalog.all, emptyList(), emptyList())
        assertTrue(moves.any { it.kind == HitKind.CHALLENGE && it.id == "fis.levantarse" })

        val goals = Search.run("INGLÉS", emptyList(), emptyList(), listOf(goal("g", "Terminar el curso de inglés")), emptyList())
        assertEquals(listOf("g"), goals.map { it.id })
        assertTrue(Search.run("a", Catalog.challenges, AchievementCatalog.all, emptyList(), emptyList()).isEmpty())
    }

    @Test fun searchCapsEachKind() {
        val notes = (0 until 30).map { inst("n$it", MONDAY.minus(it, DateTimeUnit.DAY)).copy(note = "nota") }
        val hits = Search.run("nota", emptyList(), emptyList(), emptyList(), notes)
        assertEquals(Search.LIMIT_PER_KIND, hits.size)
        assertEquals(MONDAY, hits.first().date, "las más recientes primero")
    }

    @Test fun todaySummaryPicksTheFirstOpenAndActiveGoals() {
        val list = listOf(
            inst("a", MONDAY).copy(slot = 0),
            inst("b", MONDAY, InstanceStatus.PENDING).copy(slot = 1),
            inst("c", MONDAY, InstanceStatus.REPLACED).copy(slot = 2),
            inst("d", MONDAY.minus(1, DateTimeUnit.DAY), InstanceStatus.PENDING),
        )
        val goals = listOf(goal("g2", "Dos", createdAt = 2), goal("g1", "Uno"), goal("g3", "Hecha", GoalStatus.ACHIEVED))
        val s = TodaySummary.from(MONDAY, list, goals)
        assertEquals("b@$MONDAY", s.mainId)
        assertEquals(1, s.done)
        assertEquals(2, s.total)
        assertEquals(listOf("Uno", "Dos"), s.goals)
        assertNull(TodaySummary.from(MONDAY, list.take(1)).mainId)
    }

    @Test fun pendingReturnOnlyForRecentUnreviewedPauses() {
        val ended = Pause("p1", MONDAY.minus(10, DateTimeUnit.DAY), MONDAY.minus(1, DateTimeUnit.DAY))
        val open = Pause("p2", MONDAY, null)
        assertEquals(ended, PauseRules.pendingReturn(MONDAY, listOf(ended), null))
        assertNull(PauseRules.pendingReturn(MONDAY, listOf(ended), "p1"))
        assertNull(PauseRules.pendingReturn(MONDAY, listOf(open), null))
        assertNull(PauseRules.pendingReturn(MONDAY.plus(40, DateTimeUnit.DAY), listOf(ended), null))
    }

    @Test fun csvQuotesAndNeutralizesFormulas() {
        assertEquals("\"a,\"\"b\"\"\"", CsvExport.field("a,\"b\""))
        assertEquals("\"'=HYPERLINK(1)\"", CsvExport.field("=HYPERLINK(1)"))
        assertEquals("\"'+1\"", CsvExport.field("+1"))
        assertEquals("\"'@x\"", CsvExport.field("@x"))
        assertEquals("\"\"", CsvExport.field(""))

        val csv = CsvExport.instances(
            listOf(inst("a", MONDAY).copy(note = "-nota", minimal = true), inst("b", MONDAY, InstanceStatus.REPLACED)),
        ) { "Físico" }
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("\"$MONDAY\""))
        assertTrue("\"hecho (versión mínima)\"" in lines[1])
        assertTrue(lines[1].endsWith("\"'-nota\""))
    }
}
