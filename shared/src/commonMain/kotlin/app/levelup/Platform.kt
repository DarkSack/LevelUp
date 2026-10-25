package app.levelup

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
}
