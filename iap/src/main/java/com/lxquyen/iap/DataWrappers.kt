package com.lxquyen.iap

class DataWrappers {

    data class ProductDetails(
        val title: String?,
        val productId: String,
        val productType: String?,
        val formattedPrice: String?,
        val priceAmountMicros: Long
    ) {
        override fun toString(): String {
            return "title: $title\n" +
                    "productId: $productId\n" +
                    "productType: $productType\n" +
                    "formattedPrice: $formattedPrice\n" +
                    "priceAmountMicros: $priceAmountMicros"
        }
    }

    data class PurchaseInfo(
        val productId: String,
        val originalJson: String,
        val isAcknowledged: Boolean,
        val purchaseTime: Long,
    ) {
        override fun toString(): String {
            return "productId: $productId\n" +
                    "originalJson: $originalJson\n" +
                    "isAcknowledged: $isAcknowledged\n" +
                    "purchaseTime: $purchaseTime"
        }
    }
}