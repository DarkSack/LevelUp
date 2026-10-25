package app.levelup

import kotlinx.coroutines.flow.StateFlow

/** LevelUp Plus: compra única en la tienda. `price` llega de la tienda con su moneda; null si no responde. */
data class PlusState(val owned: Boolean = false, val price: String? = null)

/** Lo que depende del sistema operativo. Android lo implementa en androidApp; iOS lo hará en su proyecto. */
interface Platform {
    val appVersion: String

    /** El sistema pide quitar animaciones (en Android: "Quitar animaciones" en Accesibilidad). */
    val systemReducedMotion: Boolean

    /** Guarda un archivo donde elija el usuario. false si cancela o falla. */
    suspend fun exportFile(fileName: String, content: String): Boolean

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
}
