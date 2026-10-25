package app.levelup.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** `name = null` crea la base de datos en memoria (pruebas). */
fun buildDatabase(context: Context, name: String? = "levelup.db"): AppDatabase {
    val builder = if (name == null) {
        Room.inMemoryDatabaseBuilder<AppDatabase>(context)
    } else {
        Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath(name).absolutePath)
    }
    return builder
        .setDriver(AndroidSQLiteDriver())
        .addCallback(SyncTriggers)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
