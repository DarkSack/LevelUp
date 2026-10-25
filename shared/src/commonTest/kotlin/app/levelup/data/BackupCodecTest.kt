package app.levelup.data

import app.levelup.MONDAY
import app.levelup.domain.Preferences
import app.levelup.domain.XpEntry
import app.levelup.domain.XpSource
import app.levelup.inst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupCodecTest {
    private val backup = Backup(
        exportedAt = "2026-10-24T12:00:00Z",
        preferences = Preferences(displayName = "Ana", categories = listOf("fisico"), categoryDifficulty = mapOf("fisico" to 3)),
        instances = listOf(inst("fis.estirar", MONDAY)),
        xp = listOf(XpEntry("x1", XpSource.CHALLENGE, "fis.estirar@$MONDAY", 15, "fisico", 1)),
    )

    @Test fun roundTrip() {
        val text = BackupCodec.encode(backup)
        assertTrue("\"format\": \"levelup-backup\"" in text, "debe ser JSON legible")
        assertEquals(backup, BackupCodec.decode(text).getOrThrow())
    }

    @Test fun rejectsWhatIsNotABackup() {
        assertTrue(BackupCodec.decode("hola").isFailure)
        assertTrue(BackupCodec.decode("{}").isFailure)
        assertTrue(BackupCodec.decode(BackupCodec.encode(backup.copy(format = "otra-app"))).isFailure)
    }

    @Test fun rejectsNewerSchemas() {
        val error = BackupCodec.decode(BackupCodec.encode(backup.copy(schemaVersion = Backup.SCHEMA_VERSION + 1))).exceptionOrNull()
        assertTrue(error?.message?.contains("versión más nueva") == true)
    }

    @Test fun rejectsDuplicatedXp() {
        val dup = backup.copy(xp = backup.xp + backup.xp.first().copy(id = "x2"))
        assertTrue(BackupCodec.decode(BackupCodec.encode(dup)).isFailure)
    }
}
