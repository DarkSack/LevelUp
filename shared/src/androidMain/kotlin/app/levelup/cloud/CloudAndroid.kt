package app.levelup.cloud

import android.content.Context
import app.levelup.data.AppDatabase
import app.levelup.data.LevelUpRepository
import app.levelup.data.SyncEngine
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * La sesión (tokens) en un archivo de preferencias propio, privado de la app. Las reglas de copia de Android
 * lo excluyen: restaurar una sesión en otro teléfono la reutilizaría sin volver a entrar.
 */
class PrefsSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY, json.encodeToString(session)).apply()
    }

    // La librería espera una excepción si no hay sesión (su loadSessionOrNull la convierte en null).
    override suspend fun loadSession(): UserSession =
        json.decodeFromString(prefs.getString(KEY, null) ?: error("Sin sesión guardada"))

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        /** Debe coincidir con res/xml/backup_rules.xml y data_extraction_rules.xml. */
        const val FILE = "levelup_session"
        private const val KEY = "session"
    }
}

/** Arma la nube para Android. La app no conoce Supabase: solo recibe el servicio. */
fun androidCloudService(
    context: Context,
    db: AppDatabase,
    repo: LevelUpRepository,
    config: CloudConfig,
    scope: CoroutineScope,
    scheduleSync: () -> Unit,
): CloudService {
    val cloud = Cloud(config, PrefsSessionManager(context.applicationContext))
    return CloudService(cloud, SyncEngine(db, cloud), repo, scope, scheduleSync)
}

/**
 * Firma de Google Play sobre el JSON de una compra (RSA con SHA1, como la genera Play), comprobada con la clave
 * de licencia de Play Console. Sin clave no se comprueba.
 *
 * shortcut: la comprobación vive en el teléfono y una app modificada puede saltarla; si el pirateo llega a
 * importar, verificar el token con la Google Play Developer API desde el servidor.
 */
fun playSignatureValid(base64PublicKey: String, signedData: String, signature: String): Boolean {
    if (base64PublicKey.isBlank()) return true
    return runCatching {
        val decoder = Base64.getMimeDecoder()
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decoder.decode(base64PublicKey)))
        Signature.getInstance("SHA1withRSA").run {
            initVerify(key)
            update(signedData.toByteArray())
            verify(decoder.decode(signature))
        }
    }.getOrDefault(false)
}
