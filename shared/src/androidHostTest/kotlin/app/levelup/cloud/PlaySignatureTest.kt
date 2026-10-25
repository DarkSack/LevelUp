package app.levelup.cloud

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaySignatureTest {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKey = Base64.getEncoder().encodeToString(keys.public.encoded)
    private val purchase = """{"productId":"levelup_plus","purchaseState":0}"""

    private fun sign(data: String) = Signature.getInstance("SHA1withRSA").run {
        initSign(keys.private)
        update(data.toByteArray())
        Base64.getEncoder().encodeToString(sign())
    }

    @Test fun acceptsGenuineSignature() = assertTrue(playSignatureValid(publicKey, purchase, sign(purchase)))

    @Test fun rejectsTamperedPurchase() =
        assertFalse(playSignatureValid(publicKey, purchase.replace("0", "1"), sign(purchase)))

    @Test fun rejectsGarbage() = assertFalse(playSignatureValid(publicKey, purchase, "no-es-una-firma"))

    @Test fun withoutKeyNothingIsChecked() = assertTrue(playSignatureValid("", purchase, ""))
}
