package app.levelup

import app.levelup.domain.ChallengeInstance
import app.levelup.domain.ChallengeSource
import app.levelup.domain.InstanceStatus
import app.levelup.domain.Rating
import kotlinx.datetime.LocalDate

val MONDAY = LocalDate(2026, 10, 19)

fun inst(
    challengeId: String,
    date: LocalDate,
    status: InstanceStatus = InstanceStatus.COMPLETED,
    category: String = "fisico",
    difficulty: Int = 2,
    rating: Rating? = null,
    simplified: Boolean = false,
    isHabit: Boolean = false,
) = ChallengeInstance(
    id = "$challengeId@$date",
    challengeId = challengeId,
    source = ChallengeSource.CATALOG,
    title = challengeId,
    category = category,
    difficulty = difficulty,
    minutes = 5,
    xp = 15,
    date = date,
    slot = 0,
    isHabit = isHabit,
    status = status,
    originalChallengeId = if (simplified) "original" else null,
    rating = rating,
    createdAt = 0,
)
