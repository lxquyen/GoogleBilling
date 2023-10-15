package com.lxquyen.iap.service

import android.app.Activity
import com.lxquyen.iap.listener.PurchaseServiceListener
import com.lxquyen.iap.listener.SubscriptionServiceListener

internal interface IapService {
    fun setPurchaseServiceListener(listener: PurchaseServiceListener)
    fun setSubscriptionServiceListener(listener: SubscriptionServiceListener)
    fun buy(activity: Activity, productId: String)
    fun subscribe(activity: Activity, productId: String)
    fun unsubscribe(activity: Activity, productId: String)
}
