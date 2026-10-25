package app.levelup.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.levelup.domain.CustomChallengeDraft
import app.levelup.domain.Days
import app.levelup.domain.InstanceStatus
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
}
