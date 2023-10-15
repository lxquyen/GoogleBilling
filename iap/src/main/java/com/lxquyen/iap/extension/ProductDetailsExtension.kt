package com.lxquyen.iap.extension

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.lxquyen.iap.DataWrappers


internal val ProductDetails.formattedPrice: String?
    get() {
        if (this.productType == BillingClient.ProductType.INAPP) {
            return this.oneTimePurchaseOfferDetails?.formattedPrice
        }
        return this.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

internal val ProductDetails.priceAmountMicros: Long?
    get() {
        if (this.productType == BillingClient.ProductType.INAPP) {
            return this.oneTimePurchaseOfferDetails?.priceAmountMicros
        }
        return this.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.priceAmountMicros
    }

internal val ProductDetails.dataWrapper: DataWrappers.ProductDetails
    get() {
        return DataWrappers.ProductDetails(
            title = this.title,
            productId = this.productId,
            productType = this.productType,
            formattedPrice = this.formattedPrice,
            priceAmountMicros = this.priceAmountMicros ?: 0L,
        )
    }