package com.lxquyen.iap.listener

import com.lxquyen.iap.DataWrappers

interface PurchaseServiceListener {

    /**
     * Callback will be triggered upon obtaining information about product prices
     */
    fun onPricesUpdated(products: List<DataWrappers.ProductDetails>)

    /**
     * Callback will be triggered when a product purchased successfully
     *
     * @param purchaseInfo - specifier of owned product
     */
    fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo)

}