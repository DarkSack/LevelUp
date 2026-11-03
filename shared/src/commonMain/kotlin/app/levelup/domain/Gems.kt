package app.levelup.domain

/**
 * Gemas para comprar mascotas y accesorios. Derivadas, no guardadas: se ganan con los retos completados (una por reto,
 * con tope al día para que se junten poco a poco), con cada logro según su nivel y con cada revisión semanal terminada.
 */
object Gems {
    const val DAILY_CAP = 3
    const val PER_REVIEW = 5

    fun forTrophy(tier: TrophyTier): Int = when (tier) {
        TrophyTier.BRONZE -> 5
        TrophyTier.SILVER -> 10
        TrophyTier.GOLD -> 20
        TrophyTier.PLATINUM -> 40
    }

    fun earned(completed: List<ChallengeInstance>, trophies: Collection<TrophyTier> = emptyList(), reviews: Int = 0): Int =
        completed.filter { it.isDone }.groupingBy { it.date }.eachCount().values.sumOf { minOf(it, DAILY_CAP) } +
            trophies.sumOf(::forTrophy) + reviews * PER_REVIEW

    /** Precio de la mascota número [bought] + 1: 100, 175, 275, 400, 550, 725, 925… (con 3 al día, la primera tarda más de un mes). */
    fun price(bought: Int): Int = 100 + 75 * bought + 25 * bought * (bought - 1) / 2

    fun spent(bought: Int): Int = (0 until bought).sumOf { price(it) }
}
