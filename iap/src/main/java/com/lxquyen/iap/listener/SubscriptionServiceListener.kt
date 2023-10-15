package com.lxquyen.iap.listener

import com.lxquyen.iap.DataWrappers

interface SubscriptionServiceListener {

    /**
     * Callback will be triggered upon obtaining information about product prices
     */
    fun onPricesUpdated(products: List<DataWrappers.ProductDetails>)

    /**
     * Callback will be triggered when a subscription purchased successfully
     *
     * @param purchaseInfo - specifier of purchased subscription
     */
    fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo)

    /**
     * Callback will be triggered upon owned subscription restore
     *
     * @param purchaseInfo - specifier of owned subscription
     */
    fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo)
}