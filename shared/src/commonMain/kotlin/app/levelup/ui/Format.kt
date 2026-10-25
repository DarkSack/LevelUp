package app.levelup.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// shortcut: textos solo en español dentro del código; al añadir otro idioma, pasarlos a composeResources (values-xx/strings.xml).
object Fmt {
    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun day(date: LocalDate): String = "${date.day} de ${months[date.month.ordinal]}"

    fun dayWithYear(date: LocalDate): String = "${day(date)} de ${date.year}"

    fun monthName(month: Month): String = months[month.ordinal]

    fun weekday(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    }

    fun weekdayShort(day: DayOfWeek): String = weekday(day).take(2).replaceFirstChar { it.uppercase() }

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
            0 -> "hoy"
            1 -> "ayer"
            in 2..6 -> "hace $days días"
            else -> "el ${day(date)}"
        }
    }

    fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

    fun difficulty(d: Int): String = when (d) {
        1 -> "Muy fácil"
        2 -> "Fácil"
        3 -> "Media"
        4 -> "Exigente"
        else -> "Reto mayor"
    }
}
