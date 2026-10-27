package app.levelup.ui

import app.levelup.domain.DayBlock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import app.levelup.i18n.tr

/** Fechas y cifras ya traducidas al idioma de la app (ver i18n/I18n.kt). */
object Fmt {
    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun day(date: LocalDate): String = tr("{0} de {1}", date.day, months[date.month.ordinal])

    fun dayWithYear(date: LocalDate): String = tr("{0} de {1} de {2}", date.day, months[date.month.ordinal], date.year)

    fun monthName(month: Month): String = tr(months[month.ordinal])

    fun weekday(day: DayOfWeek): String = tr(when (day) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    })

    fun weekdayShort(day: DayOfWeek): String = tr(listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")[day.ordinal])

    fun minutes(min: Int): String = when {
        min < 60 -> "$min min"
        min % 60 == 0 -> "${min / 60} h"
        else -> "${min / 60} h ${min % 60} min"
    }

    fun time(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun dateTime(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
        return "${day(t.date)}, ${time(t.hour * 60 + t.minute)}"
    }

    /** "hoy", "ayer", "hace 3 días" o la fecha, a partir de una marca ISO del servidor. null si no se entiende. */
    fun lastSeen(iso: String, today: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): String? {
        val date = runCatching { Instant.parse(iso) }.getOrNull()?.toLocalDateTime(zone)?.date ?: return null
        return when (val days = date.daysUntil(today)) {
            0 -> tr("hoy")
            1 -> tr("ayer")
            in 2..6 -> tr("hace {0} días", days)
            else -> tr("el {0}", day(date))
        }
    }

    fun plural(n: Int, one: String, many: String): String = "$n ${tr(if (n == 1) one else many)}"

    fun difficulty(d: Int): String = tr(when (d) {
        1 -> "Muy fácil"
        2 -> "Fácil"
        3 -> "Media"
        4 -> "Exigente"
        else -> "Reto mayor"
    })

    fun block(b: DayBlock): String = tr(when (b) {
        DayBlock.MORNING -> "Mañana"
        DayBlock.AFTERNOON -> "Tarde"
        DayBlock.EVENING -> "Noche"
    })
}
