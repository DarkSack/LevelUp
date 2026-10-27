package app.levelup.i18n

/**
 * Traducciones. El español escrito en el código es la clave; el inglés y el portugués (de Brasil) están en
 * TextosUi.kt y TextosCatalogo.kt. Lo que no tenga traducción sale en español.
 *
 * Dónde se traduce:
 * - Todo lo que se pinta con `Text` (el de ui/components, no el de Material): textos fijos y también los del
 *   catálogo, los logros o los guardados en la base, que siguen en español para valer en cualquier idioma.
 * - Con [tr], lo que no pasa por `Text`: avisos, descripciones para TalkBack, notificaciones, widget y reloj, y
 *   los textos con datos dentro («{0} de {1} hechos»).
 */
object I18n {
    /** "es", "en" o "pt". La plataforma lo fija al arrancar con el idioma del teléfono (o el elegido para la app). */
    var language: String = "es"
        set(value) {
            field = if (value == "en" || value == "pt") value else "es"
        }

    private val texts: Map<String, Array<String>> by lazy {
        HashMap<String, Array<String>>(4096).apply {
            ui()
            catalog()
        }
    }

    /** [es] en el idioma actual; el mismo texto si no hay traducción (o si no es una clave, como lo que escribe la persona). */
    fun get(es: String): String = when (language) {
        "en" -> texts[es]?.get(0)
        "pt" -> texts[es]?.get(1)
        else -> null
    } ?: es

    /** Para las pruebas: qué textos tienen traducción. */
    internal val keys: Set<String> get() = texts.keys
}

/**
 * [es] traducido, con {0}, {1}… cambiados por [args]. Los argumentos de texto también se traducen si son claves
 * (títulos de retos, nombres de categorías o de logros); lo escrito por la persona queda igual.
 */
fun tr(es: String, vararg args: Any?): String {
    var s = I18n.get(es)
    args.forEachIndexed { i, a -> s = s.replace("{$i}", if (a is String) I18n.get(a) else a.toString()) }
    return s
}

internal fun MutableMap<String, Array<String>>.t(es: String, en: String, pt: String) {
    put(es, arrayOf(en, pt))
}
