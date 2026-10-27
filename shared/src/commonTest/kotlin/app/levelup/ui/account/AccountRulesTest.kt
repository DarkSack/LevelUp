package app.levelup.ui.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Las mismas reglas que pide el servidor (supabase/config.toml), para avisar antes de enviar. */
class AccountRulesTest {
    @Test fun passwordNeedsEightCharsWithLettersAndDigits() {
        assertEquals("Al menos 8 caracteres.", passwordProblem("abc123"))
        assertEquals("Usa letras y números.", passwordProblem("abcdefgh"))
        assertEquals("Usa letras y números.", passwordProblem("12345678"))
        assertNull(passwordProblem("nivel2026"))
    }

    @Test fun deviceNameComesFromTheUserAgent() {
        fun device(agent: String) = app.levelup.cloud.DeviceSession("s", agent, "", "", false).device
        assertEquals("Pixel 7 · Android 14", device("LevelUp 0.1.0 (Pixel 7; Android 14)"))
        assertNull(device("okhttp/4.12.0"))
        assertNull(device(""))
    }

    @Test fun loginIsAnEmailOrAUsername() {
        val login = app.levelup.cloud.Login
        assertEquals("levelup-ana_1@sackito.online", login.email(" Ana_1 "))
        assertEquals("ana@correo.mx", login.email("Ana@Correo.mx"))
        assertEquals("ana_1", login.shown("levelup-ana_1@sackito.online"))
        assertEquals("ana@correo.mx", login.shown("ana@correo.mx"))
        assertNull(loginProblem("Ana_1"))
        assertNull(loginProblem("ana@correo.mx"))
        listOf("an", "año", "ana luz", "levelup-ana@sackito.online", "ana@correo").forEach { assertTrue(loginProblem(it) != null, it) }
    }

    @Test fun emailShape() {
        assertTrue(emailLooksValid(" ana@correo.mx "))
        assertFalse(emailLooksValid("ana@correo"))
        assertFalse(emailLooksValid("@correo.mx"))
        assertFalse(emailLooksValid("ana @correo.mx"))
        assertFalse(emailLooksValid("ana@correo."))
    }
}
