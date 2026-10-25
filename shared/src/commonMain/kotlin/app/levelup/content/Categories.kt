package app.levelup.content

import app.levelup.domain.CustomCategory

data class CategoryInfo(val key: String, val name: String, val description: String)

object Categories {
    const val FISICO = "fisico"
    const val SOCIAL = "social"
    const val MENTAL = "mental"
    const val PRODUCTIVIDAD = "productividad"
    const val BIENESTAR = "bienestar"
    const val CREATIVIDAD = "creatividad"
    const val FINANZAS = "finanzas"
    const val DIGITAL = "digital"
    const val HOGAR = "hogar"

    val builtIn = listOf(
        CategoryInfo(FISICO, "Físico", "Moverte a tu manera"),
        CategoryInfo(SOCIAL, "Social", "Cuidar tus relaciones"),
        CategoryInfo(MENTAL, "Mente y aprendizaje", "Aprender algo nuevo"),
        CategoryInfo(PRODUCTIVIDAD, "Productividad", "Organizarte y avanzar"),
        CategoryInfo(BIENESTAR, "Bienestar", "Descansar y cuidarte"),
        CategoryInfo(CREATIVIDAD, "Creatividad", "Crear por gusto"),
        CategoryInfo(FINANZAS, "Finanzas personales", "Ordenar tu dinero"),
        CategoryInfo(DIGITAL, "Hábitos digitales", "Usar mejor las pantallas"),
        CategoryInfo(HOGAR, "Hogar y entorno", "Un espacio más agradable"),
    )

    private val byKey = builtIn.associateBy { it.key }

    fun isBuiltIn(key: String): Boolean = key in byKey

    fun name(key: String, custom: List<CustomCategory> = emptyList()): String =
        byKey[key]?.name ?: custom.firstOrNull { it.id == key }?.name ?: "Personal"
}
