package app.levelup

import android.app.Activity
import android.content.Context
import app.levelup.cloud.playSignatureValid
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LevelUp Plus: compra única ([PRODUCT_ID] en Play Console). Google Play la recuerda: se recupera al abrir la
 * app, también en otro teléfono con la misma cuenta de Google. Lo último que dijo Play se guarda para no
 * parpadear al abrir sin conexión.
 */
class PlusBilling(context: Context, private val scope: CoroutineScope, private val licenseKey: String) : PurchasesUpdatedListener {
    private val cache = context.getSharedPreferences("levelup_plus", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(PlusState(owned = cache.getBoolean(KEY_OWNED, false)))
    val state: StateFlow<PlusState> = _state

    private var product: ProductDetails? = null

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Al abrir la app y al pulsar «restaurar»: un pago pendiente puede haberse completado. */
    fun refresh() {
        if (client.isReady) return load()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) load()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun buy(activity: Activity) {
        val details = product ?: return refresh()
        client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
                .build(),
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && purchases != null) {
            scope.launch { if (confirm(purchases)) setOwned(true) }
        }
    }

    private fun load() {
        scope.launch {
            val owned = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
            // Es la lista completa de Play: también apaga Plus tras un reembolso. Si Play falla, no se toca nada.
            if (owned.billingResult.responseCode == BillingResponseCode.OK) setOwned(confirm(owned.purchasesList))
            val query = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(ProductType.INAPP).build()
            product = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(query)).build())
                .productDetailsList?.firstOrNull()
            _state.update { it.copy(price = product?.oneTimePurchaseOfferDetails?.formattedPrice) }
        }
    }

    private fun setOwned(owned: Boolean) {
        cache.edit().putBoolean(KEY_OWNED, owned).apply()
        _state.update { it.copy(owned = owned) }
    }

    /** Si hay una compra pagada y firmada por Play. Sin confirmar en 3 días, Google la reembolsa: se confirma aquí. */
    private suspend fun confirm(purchases: List<Purchase>): Boolean {
        val owned = purchases.filter {
            PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                playSignatureValid(licenseKey, it.originalJson, it.signature)
        }
        owned.filterNot { it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        return owned.isNotEmpty()
    }

    private companion object {
        /** Hay que crearlo con este id en Play Console (producto integrado, no suscripción). */
        const val PRODUCT_ID = "levelup_plus"
        const val KEY_OWNED = "owned"
    }
}
