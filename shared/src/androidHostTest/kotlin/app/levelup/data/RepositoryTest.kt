package app.levelup.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.levelup.content.Catalog
import app.levelup.content.Tags
import app.levelup.domain.CustomChallengeDraft
import app.levelup.domain.CustomStatus
import app.levelup.domain.Days
import app.levelup.domain.HitKind
import app.levelup.domain.InstanceStatus
import app.levelup.domain.NOTE_MAX
import app.levelup.domain.XpRules
import app.levelup.domain.XpSource
import app.levelup.domain.MissionStatus
import app.levelup.domain.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import org.junit.After
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class TestClock(var instant: Instant = Instant.parse("2026-10-19T15:00:00Z")) : Clock {
    override fun now(): Instant = instant
    fun advanceDays(n: Int) { instant += n.days }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = TestClock() // lunes 19 de octubre de 2026
    private lateinit var db: AppDatabase
    private lateinit var repo: LevelUpRepository

    @Before fun setUp() {
        db = buildDatabase(context, null)
        repo = LevelUpRepository(db, clock) { TimeZone.UTC }
    }

    @After fun tearDown() = db.close()

    private suspend fun onboarded(change: (Preferences) -> Preferences = { it }) {
        repo.updatePreferences {
            change(it.copy(onboardingDone = true, categories = listOf("fisico", "mental", "hogar"), minutesPerDay = 30))
        }
    }

    @Test fun noChallengesUntilOnboardingIsDone() = runTest {
        assertTrue(repo.ensureToday().isEmpty())
    }

    @Test fun dailyChallengesAreGeneratedOnlyOnce() = runTest {
        onboarded()
        val first = repo.ensureToday()
        assertEquals(3, first.size)
        assertEquals(first, repo.ensureToday())
        assertEquals(0, first.first().slot)
    }

    @Test fun completingTwiceGivesXpOnce() = runTest {
        onboarded()
        val inst = repo.ensureToday().first()
        val result = assertNotNull(repo.complete(inst.id))
        assertEquals(listOf("first_step"), result.achievements.map { it.id })
        assertEquals(inst.xp + 10, result.xpGained)
        assertNull(repo.complete(inst.id))
        assertEquals(inst.xp + 10, repo.totalXp.first())
        assertEquals(InstanceStatus.COMPLETED, repo.instance(inst.id).first()?.status)
    }

    @Test fun undoRemovesTheChallengeXpButKeepsEarnedAchievements() = runTest {
        onboarded()
        val inst = repo.ensureToday().first()
        repo.complete(inst.id)
        repo.undoComplete(inst.id)
        assertEquals(10, repo.totalXp.first())
        assertEquals(InstanceStatus.PENDING, repo.instance(inst.id).first()?.status)
        val again = assertNotNull(repo.complete(inst.id))
        assertTrue(again.achievements.isEmpty())
        assertEquals(inst.xp + 10, repo.totalXp.first())
    }

    @Test fun simplifyWalksDownTheChainAndIsCelebrated() = runTest {
        onboarded()
        val walk = assertNotNull(repo.addToToday("fis.caminar-2km"))
        val one = repo.simplify(walk.id).getOrThrow()
        assertEquals("fis.caminar-1km", one.challengeId)
        assertEquals("fis.caminar-2km", one.originalChallengeId)
        assertEquals(15, one.xp)
        val ten = repo.simplify(walk.id).getOrThrow()
        assertEquals("fis.caminar-10", ten.challengeId)
        assertEquals("fis.caminar-2km", ten.originalChallengeId)
        assertTrue(repo.simplify(walk.id).isFailure)
        val done = assertNotNull(repo.complete(walk.id))
        assertTrue(done.achievements.any { it.id == "simplify_1" })
    }

    @Test fun replaceClosesTheOldOneWithoutXp() = runTest {
        onboarded()
        val old = assertNotNull(repo.addToToday("fis.caminar-1km"))
        val new = assertNotNull(repo.replace(old.id))
        assertNotEquals(old.challengeId, new.challengeId)
        assertEquals("fisico", new.category)
        assertTrue(new.difficulty <= old.difficulty)
        assertEquals(InstanceStatus.REPLACED, repo.instance(old.id).first()?.status)
        assertNull(repo.complete(old.id))
        assertEquals(0, repo.totalXp.first())
    }

    @Test fun addingTheSameChallengeTwiceKeepsOneInstance() = runTest {
        onboarded()
        val a = assertNotNull(repo.addToToday("hog.cama"))
        repo.skip(a.id)
        val b = assertNotNull(repo.addToToday("hog.cama"))
        assertEquals(a.id, b.id)
        assertEquals(InstanceStatus.PENDING, b.status)
    }

    @Test fun dataSurvivesClosingAndReopeningTheApp() = runTest {
        val name = "restart-test.db"
        context.deleteDatabase(name)
        val first = buildDatabase(context, name)
        val repo1 = LevelUpRepository(first, clock) { TimeZone.UTC }
        repo1.updatePreferences { it.copy(onboardingDone = true, displayName = "Ana") }
        val today = repo1.ensureToday()
        repo1.complete(today.first().id)
        val xp = repo1.totalXp.first()
        first.close()

        val second = buildDatabase(context, name)
        val repo2 = LevelUpRepository(second, clock) { TimeZone.UTC }
        assertEquals("Ana", repo2.prefs().displayName)
        assertEquals(today.map { it.id }, repo2.ensureToday().map { it.id })
        assertEquals(xp, repo2.totalXp.first())
        second.close()
        context.deleteDatabase(name)
    }

    @Test fun exportWipeImportRestoresEverything() = runTest {
        onboarded { it.copy(displayName = "Ana") }
        val inst = repo.ensureToday().first()
        repo.complete(inst.id)
        repo.saveCustom(CustomChallengeDraft(title = "Regar el huerto", category = "hogar", goal = "Regar todas las macetas"))
        val xp = repo.totalXp.first()
        val text = repo.exportBackup()

        repo.wipe()
        assertFalse(repo.prefs().onboardingDone)
        assertEquals(0, repo.totalXp.first())

        assertTrue(repo.importBackup(text).isSuccess)
        assertEquals("Ana", repo.prefs().displayName)
        assertEquals(xp, repo.totalXp.first())
        assertEquals(InstanceStatus.COMPLETED, repo.instance(inst.id).first()?.status)
        assertEquals(listOf("Regar el huerto"), repo.customChallenges.first().map { it.title })
    }

    @Test fun invalidImportChangesNothing() = runTest {
        onboarded()
        repo.complete(repo.ensureToday().first().id)
        val xp = repo.totalXp.first()
        assertTrue(repo.importBackup("{\"format\":\"levelup-backup\",\"exportedAt\":\"x\",\"xp\":[1]}").isFailure)
        assertTrue(repo.importBackup("no es json").isFailure)
        assertEquals(xp, repo.totalXp.first())
        assertTrue(repo.prefs().onboardingDone)
    }

    @Test fun customHabitScheduledTodayShowsUpImmediately() = runTest {
        onboarded()
        repo.ensureToday()
        val result = repo.saveCustom(
            CustomChallengeDraft(title = "Leer antes de dormir", category = "mental", goal = "Leer 10 páginas",
                habitDays = Days.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
        )
        assertTrue(result.errors.isEmpty())
        val today = repo.ensureToday()
        assertTrue(today.any { it.title == "Leer antes de dormir" && it.isHabit })
        assertTrue(result.unlocked.any { it.id == "custom_1" })
    }

    @Test fun invalidCustomChallengeIsNotSaved() = runTest {
        val result = repo.saveCustom(CustomChallengeDraft(title = "x", category = "nada"))
        assertTrue(result.errors.isNotEmpty())
        assertTrue(repo.customChallenges.first().isEmpty())
    }

    @Test fun missionCompletesInsideTheSameTransactionAsTheChallenge() = runTest {
        onboarded()
        repo.startMission("mis.exploracion").getOrThrow()
        assertTrue(repo.startMission("mis.ritmo").isFailure)
        val ids = listOf("fis.estirar", "men.idea", "hog.cama").map { assertNotNull(repo.addToToday(it)).id }
        assertTrue(repo.complete(ids[0])!!.missions.isEmpty())
        assertTrue(repo.complete(ids[1])!!.missions.isEmpty())
        val third = assertNotNull(repo.complete(ids[2]))
        assertEquals(listOf("mis.exploracion"), third.missions.map { it.id })
        assertTrue(third.achievements.any { it.id == "mission_1" })
        assertEquals(MissionStatus.COMPLETED, repo.missionRuns.first().single().status)
    }

    @Test fun missionsExpireWithoutPenalty() = runTest {
        onboarded()
        repo.startMission("mis.hogar").getOrThrow()
        clock.advanceDays(8)
        repo.ensureToday()
        assertEquals(MissionStatus.EXPIRED, repo.missionRuns.first().single().status)
        assertTrue(repo.startMission("mis.hogar").isSuccess)
    }

    @Test fun pauseStopsDailyChallengesAndDoesNotPunish() = runTest {
        onboarded()
        repo.ensureToday()
        val achievements = repo.startPause()
        assertTrue(achievements.any { it.id == "rest_1" })
        clock.advanceDays(1)
        assertTrue(repo.ensureToday().isEmpty())
        assertNotNull(repo.addExtraToday())
        repo.endPause()
        clock.advanceDays(1)
        assertEquals(3, repo.ensureToday().size)
    }

    @Test fun restDayGeneratesNothingButAllowsAnExtra() = runTest {
        onboarded { it.copy(restDays = Days.of(DayOfWeek.MONDAY)) }
        assertTrue(repo.ensureToday().isEmpty())
        assertNotNull(repo.addExtraToday())
        assertEquals(1, repo.ensureToday().size)
    }

    @Test fun reminderListsPendingAndSendsTheComebackNoticeOnlyOnce() = runTest {
        onboarded { it.copy(remindersEnabled = true) }
        assertEquals("3 retos para hoy", repo.reminderNow()?.title)

        repo.complete(repo.ensureToday().first().id)
        clock.advanceDays(4)
        assertEquals(true, repo.reminderNow()?.isRecovery)
        assertEquals(repo.today(), repo.prefs().lastRecoveryNotice)
        assertEquals(false, repo.reminderNow()?.isRecovery)
    }

    @Test fun noReminderOnDaysTheUserLeftOut() = runTest {
        onboarded { it.copy(remindersEnabled = true, reminderDays = Days.of(DayOfWeek.TUESDAY)) }
        assertNull(repo.reminderNow())
        clock.advanceDays(1)
        assertNotNull(repo.reminderNow())
    }

    @Test fun comingBackIsGentleAfterAWeekAndRecognizedAfterTwo() = runTest {
        onboarded()
        repo.complete(repo.ensureToday().first().id)
        clock.advanceDays(8)
        assertTrue(repo.isReturning())
        val week = repo.ensureToday()
        assertTrue(week.size in 1..2, "regreso suave: ${week.size} retos")
        assertTrue(week.sumOf { it.minutes } <= 10)
        assertFalse(repo.complete(week.first().id)!!.comeback, "tras una semana: retos suaves, sin XP extra")

        clock.advanceDays(XpRules.COMEBACK_XP_GAP_DAYS)
        val today = repo.ensureToday()
        suspend fun comebackXp() = repo.xpEntries.first().filter { it.sourceType == XpSource.COMEBACK }.sumOf { it.amount }
        val first = repo.complete(today.first().id)!!
        assertTrue(first.comeback)
        assertEquals(XpRules.COMEBACK_XP, comebackXp())
        today.getOrNull(1)?.let { assertFalse(repo.complete(it.id)!!.comeback, "solo el primero del día") }
        assertEquals(XpRules.COMEBACK_XP, comebackXp())

        today.forEach { repo.undoComplete(it.id) }
        assertEquals(0, comebackXp(), "deshacer quita también la XP de regreso")
    }

    @Test fun minimalVersionGivesTheEasiestXpAndUndoRestoresIt() = runTest {
        onboarded()
        val inst = repo.ensureToday().first()
        repo.complete(inst.id, minimal = true)
        val done = db.dao().instance(inst.id)!!
        assertTrue(done.minimal)
        assertEquals(XpRules.xpFor(1), done.xp)
        repo.undoComplete(inst.id)
        val undone = db.dao().instance(inst.id)!!
        assertFalse(undone.minimal)
        assertEquals(XpRules.xpFor(inst.difficulty), undone.xp)
    }

    @Test fun notesAreLimitedAndSearchable() = runTest {
        onboarded()
        val inst = repo.ensureToday().first()
        assertTrue(repo.setNote(inst.id, "x".repeat(NOTE_MAX + 1)).isFailure)
        assertTrue(repo.setNote(inst.id, "  Me sentí con energía  ").isSuccess)
        assertEquals("Me sentí con energía", db.dao().instance(inst.id)!!.note)
        assertEquals(listOf(inst.id), repo.search("ENERGIA").filter { it.kind == HitKind.NOTE }.map { it.id })
        assertTrue(repo.setNote(inst.id, " ").isSuccess)
        assertNull(db.dao().instance(inst.id)!!.note)
    }

    @Test fun extraForAPlaceOnlyPicksChallengesForThatPlace() = runTest {
        onboarded()
        repeat(4) {
            val inst = assertNotNull(repo.addExtraToday(Tags.TRABAJO))
            assertTrue(Tags.TRABAJO in Catalog.byId.getValue(inst.challengeId).tags)
        }
    }

    @Test fun surpriseIsSomethingNeverDoneAndNotSaved() = runTest {
        onboarded()
        val todayIds = repo.ensureToday().map { it.challengeId }.toSet()
        val s = assertNotNull(repo.surprise())
        assertFalse(s.id in todayIds)
        assertFalse(s.isHabit)
        assertEquals(todayIds, repo.ensureToday().map { it.challengeId }.toSet())
    }

    @Test fun favoritesAndGoalsSurviveABackup() = runTest {
        onboarded()
        assertTrue(repo.toggleFavorite("fis.estirar"))
        val goal = repo.saveGoal(null, "Terminar el curso de inglés", "Para viajar", null).getOrThrow()
        assertTrue(repo.saveGoal(null, "x", "", null).isFailure)
        val text = repo.exportBackup()

        assertFalse(repo.toggleFavorite("fis.estirar"))
        repo.deleteGoal(goal.id)
        assertTrue(repo.importBackup(text).isSuccess)
        assertEquals(listOf("fis.estirar"), repo.favorites.first().map { it.challengeId })
        assertEquals(listOf(goal), repo.goals.first())
    }

    @Test fun afterAPauseOnlyTheChosenHabitsComeBack() = runTest {
        onboarded()
        repo.saveCustom(CustomChallengeDraft(title = "Leer un rato", category = "mental", goal = "Leer 10 páginas", habitDays = Days.ALL))
        repo.saveCustom(CustomChallengeDraft(title = "Regar las plantas", category = "hogar", goal = "Regar todas", habitDays = Days.ALL))
        val habits = repo.customChallenges.first().associateBy { it.title }
        repo.startPause()
        clock.advanceDays(3)
        repo.endPause()
        val pause = repo.pauses.first().single()

        val keep = habits.getValue("Leer un rato").id
        repo.resumeAfterPause(pause.id, setOf(keep))
        val after = repo.customChallenges.first().associateBy { it.title }
        assertEquals(CustomStatus.ACTIVE, after.getValue("Leer un rato").status)
        assertEquals(CustomStatus.PAUSED, after.getValue("Regar las plantas").status)
        assertEquals(pause.id, repo.prefs().lastPauseReviewed)
        val open = repo.ensureToday().filter { it.isHabit && it.isOpen }.map { it.challengeId }
        assertEquals(listOf(keep), open)
    }
}
