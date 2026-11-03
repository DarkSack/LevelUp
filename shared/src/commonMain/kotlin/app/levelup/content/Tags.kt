package app.levelup.content

import app.levelup.pets.PetStage
import app.levelup.pets.PetMood
import kotlinx.datetime.LocalDate
import app.levelup.domain.Preferences
import app.levelup.pets.Accessory
import app.levelup.pets.FlameTier
import app.levelup.pets.Slot
import app.levelup.pets.Species

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
            "En los retos físicos, ver tus pasos y minutos de ejercicio de hoy y marcar como verificados los que cubra tu ejercicio. Todo se queda en el teléfono: nunca se sube.",
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

/** Mascotas de racha (módulo :pets): nombres, cuáles tiene la persona y cuánto cuesta cada accesorio. */
object Pets {
    private val names = mapOf(
        Species.GATO to "Gatito", Species.PERRO to "Perrito", Species.TLACUACHE to "Tlacuachito", Species.AJOLOTE to "Ajolotito",
        Species.XOLO to "Xolito", Species.CONEJO to "Conejito", Species.PANDA to "Pandita", Species.RANA to "Ranita",
        Species.ZORRO to "Zorrito", Species.ALEBRIJE to "Alebrije",
    )

    fun name(species: Species): String = names.getValue(species)

    /** Lo que se ve de la mascota fuera de la app (widget y reloj). */
    data class Snapshot(val species: Species, val mood: PetMood, val accessories: Set<Accessory>, val stage: PetStage, val name: String)

    /** Para elegir la primera, gratis, al empezar. */
    val STARTERS = listOf(Species.GATO, Species.PERRO)

    private fun bought(prefs: Preferences): List<Species> = prefs.ownedPets.split(',').mapNotNull { Species.byKey(it.trim()) }

    /** Betatester con el huevo aún sin abrir: en Inicio se ve el huevo. */
    fun eggPending(prefs: Preferences): Boolean = prefs.betaTester && !prefs.eggHatched

    fun boughtCount(prefs: Preferences): Int = bought(prefs).size

    fun owned(prefs: Preferences): Set<Species> =
        setOfNotNull(Species.byKey(prefs.starterPet)?.takeIf { it in STARTERS }) + bought(prefs) +
            (if (prefs.betaTester && prefs.eggHatched) setOf(Species.ALEBRIJE) else emptySet())

    fun withBought(prefs: Preferences, species: Species): String = (bought(prefs) + species).distinct().joinToString(",") { it.key }

    private fun customNames(prefs: Preferences): Map<Species, String> = prefs.petNames.split('|').mapNotNull { entry ->
        val parts = entry.split('=', limit = 2)
        if (parts.size == 2) Species.byKey(parts[0])?.let { it to parts[1] } else null
    }.toMap()

    /** El nombre que le puso la persona, o null si no le puso ninguno. */
    fun customName(prefs: Preferences, species: Species): String? = customNames(prefs)[species]

    fun withName(prefs: Preferences, species: Species, name: String): String {
        val clean = name.replace('|', ' ').replace('=', ' ').filterNot { it.isISOControl() }.trim().take(20)
        val all = customNames(prefs).toMutableMap()
        if (clean.isEmpty()) all.remove(species) else all[species] = clean
        return all.entries.joinToString("|") { "${it.key.key}=${it.value}" }
    }

    /** Temporada del año, de [from] a [to] (mes, día) inclusive; puede cruzar el año nuevo. */
    enum class Season(val label: String, val from: Pair<Int, Int>, val to: Pair<Int, Int>) {
        SAN_VALENTIN("San Valentín", 2 to 1, 2 to 15),
        HALLOWEEN("Halloween", 10 to 15, 10 to 31),
        MUERTOS("Día de Muertos", 10 to 25, 11 to 5),
        NAVIDAD("Navidad", 12 to 1, 1 to 6);

        fun has(date: LocalDate): Boolean {
            val d = date.month.ordinal + 1 to date.day
            fun le(a: Pair<Int, Int>, b: Pair<Int, Int>) = a.first < b.first || (a.first == b.first && a.second <= b.second)
            return if (le(from, to)) le(from, d) && le(d, to) else le(from, d) || le(d, to)
        }
    }

    /**
     * [price] en gemas (0 = viene incluido). [unlockedBy]: logro que hace falta antes de poder comprarlo.
     * [season]: solo se compra en esa temporada; lo comprado se queda para siempre.
     */
    class Extra(val accessory: Accessory, val name: String, val price: Int, val unlockedBy: String? = null, val season: Season? = null)

    val accessories = listOf(
        Extra(Accessory.MONO, "Moño", 0),
        Extra(Accessory.FLOR, "Cempasúchil", 30, season = Season.MUERTOS),
        Extra(Accessory.CATRINA, "Sombrero de Catrina", 120, season = Season.MUERTOS),
        Extra(Accessory.CALABAZA, "Calabaza", 60, season = Season.HALLOWEEN),
        Extra(Accessory.RENO, "Cuernos de reno", 60, season = Season.NAVIDAD),
        Extra(Accessory.BOINA, "Boina", 30),
        Extra(Accessory.GORRA, "Gorra", 30),
        Extra(Accessory.AUDIFONOS, "Audífonos", 60),
        Extra(Accessory.NAVIDAD, "Gorro navideño", 60, season = Season.NAVIDAD),
        Extra(Accessory.GORRO, "Gorro de fiesta", 120),
        Extra(Accessory.MAGO, "Sombrero de mago", 120),
        Extra(Accessory.VIKINGO, "Casco vikingo", 120),
        Extra(Accessory.COPA, "Sombrero de copa", 120),
        Extra(Accessory.SOMBRERO, "Sombrero", 150, "streak_30"),
        Extra(Accessory.CORONA, "Corona", 250, "streak_100"),
        Extra(Accessory.HALO, "Aureola", 400, "streak_300"),
        Extra(Accessory.CORONA_FUEGO, "Corona de fuego", 800, "streak_1000"),
        Extra(Accessory.BIGOTE, "Bigote", 30),
        Extra(Accessory.REDONDOS, "Lentes redondos", 30),
        Extra(Accessory.LENTES, "Lentes de sol", 60),
        Extra(Accessory.CORAZONES, "Lentes de corazón", 60, season = Season.SAN_VALENTIN),
        Extra(Accessory.MONOCULO, "Monóculo", 60),
        Extra(Accessory.ANTIFAZ, "Máscara de luchador", 120),
        Extra(Accessory.PALIACATE, "Paliacate", 0),
        Extra(Accessory.CASCABEL, "Cascabel", 30),
        Extra(Accessory.CORBATIN, "Corbatín", 30),
        Extra(Accessory.BUFANDA, "Bufanda", 60),
        Extra(Accessory.COLLAR, "Collar de flores", 60),
        Extra(Accessory.CADENA, "Cadena de oro", 120),
        Extra(Accessory.MEDALLA, "Medalla", 120),
        Extra(Accessory.GLOBO, "Globo", 60),
        Extra(Accessory.MARIPOSA, "Mariposa", 120),
        Extra(Accessory.LUCIERNAGAS, "Luciérnagas", 500, "streak_500"),
    )

    fun extra(a: Accessory): Extra = accessories.first { it.accessory == a }

    /** Los incluidos y los comprados. */
    fun ownedAccessories(prefs: Preferences): Set<Accessory> =
        accessories.filter { it.price == 0 }.map { it.accessory }.toSet() + Accessory.parse(prefs.ownedAccessories)

    fun accessoriesSpent(prefs: Preferences): Int = Accessory.parse(prefs.ownedAccessories).sumOf { extra(it).price }

    fun withAccessory(prefs: Preferences, a: Accessory): String = Accessory.format(Accessory.parse(prefs.ownedAccessories) + a)

    /** Los que ya se pueden comprar: sin logro pendiente y, si son de temporada, en su temporada. */
    fun gatesOpen(achievements: Set<String>, today: LocalDate): Set<Accessory> = accessories.filter {
        (it.unlockedBy == null || it.unlockedBy in achievements) && (it.season == null || it.season.has(today))
    }.map { it.accessory }.toSet()

    fun slotName(slot: Slot): String = when (slot) {
        Slot.HEAD -> "Cabeza"
        Slot.FACE -> "Cara"
        Slot.NECK -> "Cuello"
        Slot.SIDE -> "Al lado"
    }

    fun flameName(tier: FlameTier): String = when (tier) {
        FlameTier.NARANJA -> "Llama naranja"
        FlameTier.ROJA -> "Llama roja"
        FlameTier.AZUL -> "Llama azul"
        FlameTier.VIOLETA -> "Llama violeta"
        FlameTier.ARCOIRIS -> "Llama arcoíris"
        FlameTier.DORADA -> "Llama dorada"
        FlameTier.COSMICA -> "Llama cósmica"
        FlameTier.LEGENDARIA -> "Llama legendaria"
    }
}
