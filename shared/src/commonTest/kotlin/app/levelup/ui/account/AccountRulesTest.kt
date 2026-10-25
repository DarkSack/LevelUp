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

    @Test fun emailShape() {
        assertTrue(emailLooksValid(" ana@correo.mx "))
        assertFalse(emailLooksValid("ana@correo"))
        assertFalse(emailLooksValid("@correo.mx"))
        assertFalse(emailLooksValid("ana @correo.mx"))
        assertFalse(emailLooksValid("ana@correo."))
    }
}
