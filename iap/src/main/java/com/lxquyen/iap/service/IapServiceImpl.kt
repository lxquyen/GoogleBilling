package com.lxquyen.iap.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.lxquyen.iap.DataWrappers
import com.lxquyen.iap.extension.acknowledgePurchaseExt
import com.lxquyen.iap.extension.consumeAsyncExt
import com.lxquyen.iap.extension.dataWrapper
import com.lxquyen.iap.extension.isOk
import com.lxquyen.iap.extension.queryProductDetailsAsyncExt
import com.lxquyen.iap.extension.queryProductDetailsExt
import com.lxquyen.iap.listener.PurchaseServiceListener
import com.lxquyen.iap.listener.SubscriptionServiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class IapServiceImpl constructor(
    context: Context,
    private val consumableKeys: List<String> = listOf(),
    private val subscriptionKeys: List<String> = listOf(),
    private val enableLogging: Boolean = false,
) : IapService, BillingClientStateListener, CoroutineScope, PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    companion object {
        private const val TAG = "IapConnector"
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    private var _purchaseServiceListener: PurchaseServiceListener? = null
    private var _subscriptionServiceListener: SubscriptionServiceListener? = null

    init {
        billingClient.startConnection(this)
        log("startConnection")
    }

    //region#IapConnector
    override fun setPurchaseServiceListener(listener: PurchaseServiceListener) {
        this._purchaseServiceListener = listener
    }

    override fun setSubscriptionServiceListener(listener: SubscriptionServiceListener) {
        this._subscriptionServiceListener = listener
    }

    override fun buy(activity: Activity, productId: String) {
        launch {
            launchBillingFlow(activity, productId, BillingClient.ProductType.INAPP)
        }
    }

    override fun subscribe(activity: Activity, productId: String) {
        launch {
            launchBillingFlow(activity, productId, BillingClient.ProductType.SUBS)
        }
    }

    override fun unsubscribe(activity: Activity, productId: String) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val subscriptionUrl = ("http://play.google.com/store/account/subscriptions"
                    + "?package=" + activity.packageName
                    + "&sku=" + productId)
            intent.data = Uri.parse(subscriptionUrl)
            activity.startActivity(intent)
            activity.finish()
        } catch (e: Exception) {
            log("Unsubscribing failed.")
        }
    }
    //endregion

    //region#BillingClientStateListener
    override fun onBillingServiceDisconnected() {
        log("onBillingServiceDisconnected")
    }

    override fun onBillingSetupFinished(p0: BillingResult) {
        log("onBillingSetupFinishedOkay: billingResult: $p0")
        if (!p0.isOk) {
            return
        }

        launch {
            log("queryProductDetailsExt: consumableKeys: ${consumableKeys.size}")
            val consumableProducts = mutableListOf<DataWrappers.ProductDetails>()
            billingClient.queryProductDetailsExt(consumableKeys, BillingClient.ProductType.INAPP)
                .mapTo(consumableProducts) {
                    productDetailsMap[it.productId] = it
                    return@mapTo it.dataWrapper
                }
            _purchaseServiceListener?.onPricesUpdated(consumableProducts)
            log("onPricesConsumableProductsUpdated: products: ${consumableProducts.size}")


            log("queryProductDetailsExt: subscriptionKeys: ${consumableKeys.size}")
            val subscriptionProducts = mutableListOf<DataWrappers.ProductDetails>()
            billingClient.queryProductDetailsExt(subscriptionKeys, BillingClient.ProductType.SUBS)
                .mapTo(subscriptionProducts) {
                    productDetailsMap[it.productId] = it
                    return@mapTo it.dataWrapper
                }
            _subscriptionServiceListener?.onPricesUpdated(subscriptionProducts)
            log("onPricesSubscriptionProductsUpdated: products: ${subscriptionProducts.size}")

            queryPurchases()
        }
    }

    /**
     * Called by the Billing Library when new purchases are detected.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        log("onPurchasesUpdated: responseCode:$responseCode debugMessage: $debugMessage")

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                log("onPurchasesUpdated. purchase: $purchases")
                processPurchases(purchases ?: listOf(), false)
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                log("onPurchasesUpdated: User canceled the purchase")

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                log("onPurchasesUpdated: The user already owns this item")
                //item already owned? call queryPurchases to verify and process all such items
                launch {
                    queryPurchases()
                }
            }

            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                Log.e(
                    TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                            "does not recognize the configuration. If you are just getting started, " +
                            "make sure you have configured the application correctly in the " +
                            "Google Play Console. The SKU product ID must match and the APK you " +
                            "are using must be signed with release keys."
                )
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        log("onAcknowledgePurchaseResponse: billingResult: $billingResult")
        launch {
            queryPurchases()
        }
    }
    //endregion

    private suspend fun launchBillingFlow(activity: Activity, productId: String, type: String) {
        val product = getProductDetails(productId, type)
        val offsetToken = product.subscriptionOfferDetails?.first()?.offerToken ?: ""
        val params = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offsetToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(params)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Query Google Play Billing for existing purchases.
     * New purchases will be provided to the PurchasesUpdatedListener.
     */
    private suspend fun queryPurchases() {
        if (consumableKeys.size > 0) {
            val inAppResult: PurchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            processPurchases(inAppResult.purchasesList, isRestore = true)
        }

        if (subscriptionKeys.size > 0) {
            val subsResult: PurchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

            )
            processPurchases(subsResult.purchasesList, isRestore = true)
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>, isRestore: Boolean = false) {
        if (purchasesList.isEmpty()) {
            log("processPurchases: with no purchases")
            return
        }

        log("processPurchases: " + purchasesList.size + " purchase(s)")
        purchases@ for (purchase in purchasesList) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!isSignatureValid(purchase)) {
                    log("processPurchases. Signature is not valid for: $purchase")
                    continue@purchases
                }
                // Acknowledge the purchase if it hasn't already been acknowledged.
                if (!purchase.isAcknowledged) {
                    log("processPurchases. acknowledgePurchase")
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
                }
                // Grant entitlement to the user.
                val productDetails = productDetailsMap[purchase.products[0]]
                when (productDetails?.productType) {
                    BillingClient.ProductType.INAPP -> {
                        val params = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.consumeAsync(params) { billingResult, _ ->
                            log("processPurchases. consumeAsync: ${billingResult.responseCode}")
                            when (billingResult.responseCode) {
                                BillingClient.BillingResponseCode.OK -> {
                                    _purchaseServiceListener?.onProductPurchased(purchase.dataWrapper)
                                }

                                else -> {
                                    Log.d(
                                        TAG,
                                        "Handling consumables : Error during consumption attempt -> ${billingResult.debugMessage}"
                                    )
                                }
                            }
                        }
                    }

                    BillingClient.ProductType.SUBS -> {
                        if (isRestore) {
                            _subscriptionServiceListener?.onSubscriptionRestored(purchase.dataWrapper)
                        } else {
                            _subscriptionServiceListener?.onSubscriptionPurchased(purchase.dataWrapper)
                        }
                    }
                }
            } else {
                Log.e(
                    TAG, "processPurchases failed. purchase: $purchase "
                )
            }
        }
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return true
    }

    /**
     * Get Product details by productId and type.
     * This method has cache functionality.
     */
    private suspend fun getProductDetails(productId: String, type: String): ProductDetails {
        return productDetailsMap[productId] ?: run {
            return@run billingClient.queryProductDetailsAsyncExt(productId, type)
        }
    }

    private fun log(message: String) {
        if (enableLogging) {
            Log.d(TAG, message)
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main.immediate

}