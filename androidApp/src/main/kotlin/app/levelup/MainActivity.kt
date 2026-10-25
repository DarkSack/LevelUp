package app.levelup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val app get() = application as LevelUpApplication

    // Registrados como propiedades: así existen antes de STARTED, como exige la API.
    private val launchers = AndroidPlatform.Launchers(
        activity = this,
        export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { app.platform.onExportPicked(it) },
        exportCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { app.platform.onExportPicked(it) },
        import = registerForActivityResult(ActivityResultContracts.OpenDocument()) { app.platform.onImportPicked(it) },
        permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { app.platform.onPermissionResult(it) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        app.platform.attach(launchers)
        setContent { App(app.repo, app.platform, app.cloud) }
    }

    override fun onDestroy() {
        app.platform.detach(launchers)
        super.onDestroy()
    }
}
