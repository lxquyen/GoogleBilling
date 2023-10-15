package com.lxquyen.iap.extension

import com.android.billingclient.api.Purchase
import com.lxquyen.iap.DataWrappers

internal val Purchase.dataWrapper: DataWrappers.PurchaseInfo
    get() {
        val productId = this.products[0]
        return DataWrappers.PurchaseInfo(
            productId,
            this.originalJson,
            this.isAcknowledged,
            this.purchaseTime
        )
    }