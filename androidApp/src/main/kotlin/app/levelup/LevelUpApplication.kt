package app.levelup

import kotlinx.coroutines.flow.combine
import android.app.Application
import android.content.res.Configuration
import android.os.Build
import app.levelup.cloud.AccountState
import app.levelup.cloud.CloudConfig
import app.levelup.cloud.CloudService
import app.levelup.cloud.androidCloudService
import app.levelup.data.LevelUpRepository
import app.levelup.data.buildDatabase
import app.levelup.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Dueño de lo que vive tanto como el proceso: base de datos, plataforma y nube (sin DI: son pocos objetos). */
class LevelUpApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db by lazy { buildDatabase(this) }
    val repo by lazy { LevelUpRepository(db) }
    val platform by lazy { AndroidPlatform(this) }

    /** null si esta compilación no tiene Supabase (sin supabase.properties): la app funciona igual, sin cuenta. */
    val cloud: CloudService? by lazy {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_KEY.isBlank()) return@lazy null
        androidCloudService(this, db, repo, CloudConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY, "LevelUp ${BuildConfig.VERSION_NAME} (${Build.MODEL}; Android ${Build.VERSION.RELEASE})"), scope) {
            SyncWork.soon(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        I18n.language = resources.configuration.locales[0].language
        ReminderWork.createChannel(this)
        SyncWork.createChannel(this)
        AchievementNotice.createChannel(this)
        scope.launch { ReminderWork.scheduleHabits(this@LevelUpApplication) }
        scope.launch {
            // Las preferencias también: cambiar de mascota o de accesorio se ve en el widget y el reloj.
            repo.todaySummary.combine(repo.preferences) { summary, _ -> summary }.collect {
                val pet = repo.petSnapshot()
                TodayWidget.render(this@LevelUpApplication, it, pet)
                WearSync.publish(this@LevelUpApplication, it, pet)
            }
        }
        val service = cloud ?: return
        scope.launch {
            service.account.filter { it !is AccountState.Loading }
                .map { it is AccountState.SignedIn }
                .distinctUntilChanged()
                .collect { SyncWork.periodic(this@LevelUpApplication, it) }
        }
    }

    // También llega cuando se cambia el idioma solo de la app (Android 13+).
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        I18n.language = newConfig.locales[0].language
    }
}
