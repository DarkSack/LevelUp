package app.levelup.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

// shortcut: textos solo en español dentro del código; al añadir otro idioma, pasarlos a recursos (docs/LOCALIZACION.md).
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

    fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

    fun difficulty(d: Int): String = when (d) {
        1 -> "Muy fácil"
        2 -> "Fácil"
        3 -> "Media"
        4 -> "Exigente"
        else -> "Reto mayor"
    }
}
