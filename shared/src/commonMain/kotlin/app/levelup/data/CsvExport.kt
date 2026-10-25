package app.levelup.data

import app.levelup.domain.ChallengeInstance
import app.levelup.domain.DayBlock
import app.levelup.domain.InstanceStatus
import app.levelup.domain.Rating

/** Historial en CSV para abrirlo en una hoja de cálculo. El respaldo completo sigue siendo el JSON. */
object CsvExport {
    private val header = listOf("fecha", "reto", "categoria", "estado", "dificultad", "minutos", "xp", "valoracion", "momento", "nota")

    fun instances(list: List<ChallengeInstance>, categoryName: (String) -> String): String = buildString {
        appendLine(header.joinToString(","))
        list.filter { it.status != InstanceStatus.REPLACED }.sortedWith(compareBy({ it.date }, { it.slot })).forEach { i ->
            val row = listOf(
                i.date.toString(), i.title, categoryName(i.category), status(i), i.difficulty.toString(), i.minutes.toString(),
                if (i.isDone) i.xp.toString() else "0", rating(i.rating), block(i.block), i.note.orEmpty(),
            )
            appendLine(row.joinToString(",") { field(it) })
        }
    }

    /**
     * Entre comillas, con las comillas internas dobladas. Lo que empieza por = + - @ lleva un apóstrofo delante:
     * así una hoja de cálculo no lo ejecuta como fórmula (los títulos y notas los escribe el usuario).
     */
    fun field(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun status(i: ChallengeInstance) = when (i.status) {
        InstanceStatus.COMPLETED -> if (i.minimal) "hecho (versión mínima)" else "hecho"
        InstanceStatus.SKIPPED -> "para otro día"
        InstanceStatus.REPLACED -> "cambiado"
        else -> "sin hacer"
    }

    private fun rating(r: Rating?) = when (r) {
        Rating.EASY -> "fácil"
        Rating.RIGHT -> "justo"
        Rating.HARD -> "difícil"
        null -> ""
    }

    private fun block(b: DayBlock?) = when (b) {
        DayBlock.MORNING -> "mañana"
        DayBlock.AFTERNOON -> "tarde"
        DayBlock.EVENING -> "noche"
        null -> ""
    }
}
