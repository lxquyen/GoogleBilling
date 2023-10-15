package com.lxquyen.iap.extension

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.AlternativeChoiceDetails.Product
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal val BillingResult.isOk: Boolean
    get() = this.responseCode == BillingClient.BillingResponseCode.OK

internal suspend fun BillingClient.queryProductDetailsExt(ids: List<String>, type: String): List<ProductDetails> {
    if (ids.isEmpty()) {
        return emptyList()
    }

    val productList = ids.map {
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId(it)
            .setProductType(type)
            .build()
    }

    val params = QueryProductDetailsParams.newBuilder()
        .setProductList(productList)
        .build()

    return suspendCancellableCoroutine {
        queryProductDetailsAsync(params) { _, productDetailsList ->
            it.resume(productDetailsList)
        }
    }
}

internal suspend fun BillingClient.acknowledgePurchaseExt(purchaseToken: String): BillingResult {
    return suspendCancellableCoroutine { conn ->
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        val callback = AcknowledgePurchaseResponseListener(conn::resume)
        acknowledgePurchase(acknowledgePurchaseParams, callback)
    }
}

internal suspend fun BillingClient.consumeAsyncExt(purchaseToken: String): BillingResult {
    return suspendCancellableCoroutine { conn ->
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        val callback = ConsumeResponseListener { billingResult, _ ->
            conn.resume(billingResult)
        }
        this.consumeAsync(params, callback)
    }
}

internal suspend fun BillingClient.queryProductDetailsAsyncExt(productId: String, productType: String): ProductDetails {
    return suspendCancellableCoroutine { conn ->
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val callback = ProductDetailsResponseListener { _, productDetails ->
            val product = productDetails.first { it.productId == productId }
            conn.resume(product)
        }
        this.queryProductDetailsAsync(params, callback)
    }
}
