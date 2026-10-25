package app.levelup.content

/** Etiquetas de contexto de los retos: dónde se pueden hacer y si hace falta algo más que lo que llevas encima. */
object Tags {
    const val CASA = "casa"
    const val EXTERIOR = "exterior"
    const val TRABAJO = "trabajo"
    const val LIBRE = "libre"

    /** Sin dinero, equipo ni instalaciones: basta el cuerpo, papel o el teléfono. */
    const val SIN_MATERIALES = "sin_materiales"

    val places = listOf(CASA to "Casa", EXTERIOR to "Exterior", TRABAJO to "Trabajo o estudio", LIBRE to "Tiempo libre")

    val all: Set<String> = places.map { it.first }.toSet() + SIN_MATERIALES

    fun name(tag: String): String = places.firstOrNull { it.first == tag }?.second ?: if (tag == SIN_MATERIALES) "Sin materiales" else tag
}

/** Funciones experimentales: se activan a mano en Perfil → Laboratorio y apagarlas no borra nada. */
object Lab {
    const val SURPRISE = "surprise"
    const val HEALTH = "health_connect"

    data class Experiment(val id: String, val name: String, val description: String)

    val all = listOf(
        Experiment(SURPRISE, "Retos sorpresa", "En Inicio, un botón para descubrir un reto que nunca has hecho. No cambia tu plan del día."),
        Experiment(
            HEALTH, "Actividad de Health Connect",
            "En los retos físicos, ver tus pasos y minutos de ejercicio de hoy. Solo se leen en el teléfono y nunca se suben.",
        ),
    )
}

/** Avatares predefinidos (emoji): no hay fotos que moderar ni que guardar. */
object Avatars {
    val all = listOf(
        "zorro" to "🦊", "buho" to "🦉", "gato" to "🐱", "perro" to "🐶", "tortuga" to "🐢", "panda" to "🐼",
        "pulpo" to "🐙", "abeja" to "🐝", "planta" to "🌱", "flor" to "🌻", "arbol" to "🌳", "montana" to "⛰️",
        "ola" to "🌊", "sol" to "☀️", "luna" to "🌙", "estrella" to "⭐", "cohete" to "🚀", "libro" to "📚",
        "pincel" to "🎨", "musica" to "🎵", "balon" to "⚽", "bici" to "🚲", "taza" to "☕", "corazon" to "💚",
    )
    private val byKey = all.toMap()

    fun emoji(key: String): String? = byKey[key]
}
