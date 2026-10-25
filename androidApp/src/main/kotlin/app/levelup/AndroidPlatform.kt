package app.levelup

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vive en la Application, no en la actividad: el ViewModel la guarda y sobrevive a los giros de pantalla.
 * Los selectores de archivos y el permiso necesitan una actividad, que se engancha en [attach].
 */
class AndroidPlatform(private val app: LevelUpApplication) : Platform {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    class Launchers(
        val activity: Activity,
        val export: ActivityResultLauncher<String>,
        val exportCsv: ActivityResultLauncher<String>,
        val import: ActivityResultLauncher<Array<String>>,
        val permission: ActivityResultLauncher<String>,
        val health: ActivityResultLauncher<Set<String>>,
    )

    private var launchers: Launchers? = null
    private var pendingExport: CompletableDeferred<Uri?>? = null
    private var pendingImport: CompletableDeferred<Uri?>? = null
    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private var pendingHealth: CompletableDeferred<Set<String>>? = null
    private var secure = false

    fun attach(value: Launchers) {
        launchers = value
        applySecure(value.activity)
    }
    fun detach(value: Launchers) { if (launchers === value) launchers = null }

    // Los resultados llegan a la actividad que esté viva, aunque no sea la que lanzó el selector.
    fun onExportPicked(uri: Uri?) { pendingExport?.complete(uri) }
    fun onImportPicked(uri: Uri?) { pendingImport?.complete(uri) }
    fun onPermissionResult(granted: Boolean) { pendingPermission?.complete(granted) }
    fun onHealthResult(granted: Set<String>) { pendingHealth?.complete(granted) }

    override fun setSecure(secure: Boolean) {
        this.secure = secure
        launchers?.activity?.let(::applySecure)
    }

    private fun applySecure(activity: Activity) = activity.runOnUiThread {
        if (secure) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun shareAchievement(card: ShareCard) {
        val activity = launchers?.activity ?: return
        scope.launch {
            val uri = withContext(Dispatchers.IO) { runCatching { ShareImage.write(app, card) }.getOrNull() } ?: return@launch
            val send = Intent(Intent.ACTION_SEND).setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, "Conseguí «${card.title}» en LevelUp.")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = ClipData.newRawUri(null, uri) // así el permiso de lectura llega a través del selector
            withContext(Dispatchers.Main) { activity.startActivity(Intent.createChooser(send, "Compartir logro")) }
        }
    }

    private val links = MutableStateFlow<String?>(null)
    override val deepLinks: StateFlow<String?> = links
    override fun consumeDeepLink() { links.value = null }

    /** levelup://hoy, levelup://reto/<id>… y las pantallas de permisos de Health Connect (abren la privacidad). */
    fun openIntent(intent: Intent?) {
        val link = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data?.takeIf { it.scheme == "levelup" }
                ?.let { uri -> listOfNotNull(uri.host, uri.pathSegments.firstOrNull()).joinToString("/") }
            HEALTH_RATIONALE, Intent.ACTION_VIEW_PERMISSION_USAGE -> "privacidad"
            else -> null
        }
        if (link != null && link.substringBefore('/') in LINKS) links.value = link
    }

    override val health: HealthSource? by lazy {
        HealthConnectSource(app) { permissions ->
            val l = launchers ?: return@HealthConnectSource emptySet()
            val granted = CompletableDeferred<Set<String>>().also { pendingHealth = it }
            withContext(Dispatchers.Main) { l.health.launch(permissions) }
            granted.await()
        }
    }

    private val billing = PlusBilling(app, scope, BuildConfig.PLAY_LICENSE_KEY)
    override val plus = billing.state
    override fun buyPlus() { launchers?.activity?.let(billing::buy) }
    override fun refreshPlus() = billing.refresh()

    override val appVersion: String =
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: ""

    override val systemReducedMotion: Boolean
        get() = Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    override suspend fun exportFile(fileName: String, content: String, mimeType: String): Boolean {
        val l = launchers ?: return false
        val picked = CompletableDeferred<Uri?>().also { pendingExport = it }
        withContext(Dispatchers.Main) { (if (mimeType == "text/csv") l.exportCsv else l.export).launch(fileName) }
        val uri = picked.await() ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(content.toByteArray()) }
            }.isSuccess
        }
    }

    override suspend fun importFile(): String? {
        val l = launchers ?: return null
        val picked = CompletableDeferred<Uri?>().also { pendingImport = it }
        withContext(Dispatchers.Main) { l.import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        val uri = picked.await() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { app.contentResolver.openInputStream(uri)!!.use(::readLimited) }.getOrNull()
        }
    }

    /** Un respaldo real ocupa pocos KB: algo enorme no es un respaldo y podría agotar la memoria. */
    private fun readLimited(input: java.io.InputStream): String? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return out.toString(Charsets.UTF_8.name())
            if (out.size() + n > MAX_IMPORT_BYTES) return null
            out.write(buffer, 0, n)
        }
    }

    override fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(app).areNotificationsEnabled()

    override suspend fun requestNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return notificationsAllowed()
        val l = launchers ?: return false
        val granted = CompletableDeferred<Boolean>().also { pendingPermission = it }
        withContext(Dispatchers.Main) { l.permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        return granted.await()
    }

    override fun remindersChanged() {
        scope.launch { ReminderWork.schedule(app, app.repo.prefs()) }
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 20 * 1024 * 1024
        const val HEALTH_RATIONALE = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
        val LINKS = setOf("hoy", "enfoque", "buscar", "metas", "privacidad", "reto")
    }
}
