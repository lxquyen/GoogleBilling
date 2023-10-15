package com.lxquyen.iap

import android.app.Activity
import android.content.Context
import com.lxquyen.iap.listener.PurchaseServiceListener
import com.lxquyen.iap.listener.SubscriptionServiceListener
import com.lxquyen.iap.service.IapService
import com.lxquyen.iap.service.IapServiceImpl

class IapConnector constructor(
    context: Context,
    consumableKeys: List<String> = listOf(),
    subscriptionKeys: List<String> = listOf(),
    enableLogging: Boolean = false,
) {

    private var iapService: IapService = IapServiceImpl(
        context, consumableKeys, subscriptionKeys, enableLogging
    )

    fun setPurchaseServiceListener(listener: PurchaseServiceListener) {
        iapService.setPurchaseServiceListener(listener)
    }

    fun setSubscriptionServiceListener(listener: SubscriptionServiceListener) {
        iapService.setSubscriptionServiceListener(listener)
    }

    fun buy(activity: Activity, productId: String) {
        iapService.buy(activity, productId)
    }

    fun subscribe(activity: Activity, productId: String) {
        iapService.subscribe(activity, productId)
    }

    fun unsubscribe(activity: Activity, productId: String) {
        iapService.unsubscribe(activity, productId)
    }

}