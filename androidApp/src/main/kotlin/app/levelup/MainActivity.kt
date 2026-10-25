package app.levelup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController

class MainActivity : ComponentActivity() {
    private val app get() = application as LevelUpApplication

    // Registrados como propiedades: así existen antes de STARTED, como exige la API.
    private val launchers = AndroidPlatform.Launchers(
        activity = this,
        export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { app.platform.onExportPicked(it) },
        exportCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { app.platform.onExportPicked(it) },
        import = registerForActivityResult(ActivityResultContracts.OpenDocument()) { app.platform.onImportPicked(it) },
        permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { app.platform.onPermissionResult(it) },
        health = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { app.platform.onHealthResult(it) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        app.platform.attach(launchers)
        if (savedInstanceState == null) app.platform.openIntent(intent)
        setContent { App(app.repo, app.platform, app.cloud) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        app.platform.openIntent(intent)
    }

    override fun onDestroy() {
        app.platform.detach(launchers)
        super.onDestroy()
    }
}
