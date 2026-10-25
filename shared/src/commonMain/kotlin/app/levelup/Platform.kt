package app.levelup

import app.levelup.domain.TrophyTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** LevelUp Plus: compra única en la tienda. `price` llega de la tienda con su moneda; null si no responde. */
data class PlusState(val owned: Boolean = false, val price: String? = null)

/** Tarjeta de un logro para compartir como imagen. Sin nombre ni datos de la persona. */
data class ShareCard(val title: String, val description: String, val tier: TrophyTier, val date: String)

/** Actividad física de hoy según el sistema (Health Connect en Android). */
data class HealthToday(val steps: Long, val exerciseMinutes: Long)

/** Lectura de actividad física. Solo lectura y solo de hoy; nada se guarda ni se sube. */
interface HealthSource {
    suspend fun available(): Boolean
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun today(): HealthToday?
}

private val noLinks = MutableStateFlow<String?>(null)

/** Lo que depende del sistema operativo. Android lo implementa en androidApp; iOS lo hará en su proyecto. */
interface Platform {
    val appVersion: String

    /** El sistema pide quitar animaciones (en Android: "Quitar animaciones" en Accesibilidad). */
    val systemReducedMotion: Boolean

    /** Guarda un archivo donde elija el usuario. false si cancela o falla. */
    suspend fun exportFile(fileName: String, content: String, mimeType: String = "application/json"): Boolean

    /** Abre un archivo elegido por el usuario. null si cancela o no se puede leer. */
    suspend fun importFile(): String?

    fun notificationsAllowed(): Boolean

    /** Pide el permiso de notificaciones (solo cuando el usuario activa los recordatorios). */
    suspend fun requestNotifications(): Boolean

    /** Las preferencias de recordatorios cambiaron: reprogramar. */
    fun remindersChanged()

    val plus: StateFlow<PlusState>

    /** Abre la compra de la tienda (Google Play). */
    fun buyPlus()

    /** Vuelve a pedir las compras a la tienda: restaura Plus en un teléfono nuevo o tras un pago pendiente. */
    fun refreshPlus()

    /** Abre la hoja de compartir del sistema con la imagen de un logro. */
    fun shareAchievement(card: ShareCard) {}

    /** Impide o vuelve a permitir capturas de la pantalla actual. */
    fun setSecure(secure: Boolean) {}

    /**
     * Enlaces que abren una pantalla concreta (atajos del lanzador, widget, automatizaciones):
     * "hoy", "enfoque", "buscar", "metas", "privacidad" o "reto/<id>". Solo abren pantallas; no devuelven datos.
     */
    val deepLinks: StateFlow<String?> get() = noLinks

    fun consumeDeepLink() {}

    /** null si el sistema no tiene de dónde leer actividad física. */
    val health: HealthSource? get() = null
}
