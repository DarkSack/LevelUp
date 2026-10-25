package app.levelup

import android.app.Application
import app.levelup.data.LevelUpRepository
import app.levelup.data.buildDatabase

/** Dueño de lo que vive tanto como el proceso: la base de datos y la plataforma (sin DI: son dos objetos). */
class LevelUpApplication : Application() {
    val repo by lazy { LevelUpRepository(buildDatabase(this)) }
    val platform by lazy { AndroidPlatform(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderWork.createChannel(this)
    }
}
