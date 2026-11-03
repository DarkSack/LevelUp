package app.levelup.domain

/**
 * Gemas para comprar mascotas. Derivadas, no guardadas: se ganan con los retos completados (una por reto, con tope
 * al día para que se junten poco a poco) y se gastan según cuántas mascotas se compraron, cada una más cara.
 */
object Gems {
    const val DAILY_CAP = 3

    fun earned(completed: List<ChallengeInstance>): Int =
        completed.filter { it.isDone }.groupingBy { it.date }.eachCount().values.sumOf { minOf(it, DAILY_CAP) }

    /** Precio de la mascota número [bought] + 1: 100, 175, 275, 400, 550, 725, 925… (con 3 al día, la primera tarda más de un mes). */
    fun price(bought: Int): Int = 100 + 75 * bought + 25 * bought * (bought - 1) / 2

    fun spent(bought: Int): Int = (0 until bought).sumOf { price(it) }
}
