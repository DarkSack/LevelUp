package app.levelup

import android.app.Application
import android.os.Build
import app.levelup.cloud.AccountState
import app.levelup.cloud.CloudConfig
import app.levelup.cloud.CloudService
import app.levelup.cloud.androidCloudService
import app.levelup.data.LevelUpRepository
import app.levelup.data.buildDatabase
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
        ReminderWork.createChannel(this)
        SyncWork.createChannel(this)
        AchievementNotice.createChannel(this)
        scope.launch { ReminderWork.scheduleHabits(this@LevelUpApplication) }
        scope.launch {
            repo.todaySummary.collect {
                TodayWidget.render(this@LevelUpApplication, it)
                WearSync.publish(this@LevelUpApplication, it)
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
}
