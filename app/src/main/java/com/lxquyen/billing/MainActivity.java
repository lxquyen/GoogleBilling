package com.lxquyen.billing;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lxquyen.iap.DataWrappers;
import com.lxquyen.iap.IapConnector;
import com.lxquyen.iap.listener.PurchaseServiceListener;
import com.lxquyen.iap.listener.SubscriptionServiceListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements PurchaseServiceListener, SubscriptionServiceListener {

    private IapConnector iapConnector;
    private TextView tvDebug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<String> consumableKeys = new ArrayList<>();
        consumableKeys.add("coin_2");
        List<String> subscriptionKeys = new ArrayList<>();
        subscriptionKeys.add("gold_monthly");

        iapConnector = new IapConnector(this,
                consumableKeys,
                subscriptionKeys,
                true
        );

        tvDebug = findViewById(R.id.tv_debug);
        iapConnector.setPurchaseServiceListener(this);
        iapConnector.setSubscriptionServiceListener(this);

    }

    public void doBuy(View view) {
        iapConnector.buy(this, "coin_2");
    }

    public void doSubscribe(View view) {
        iapConnector.subscribe(this, "gold_monthly");
    }

    @Override
    public void onPricesUpdated(@NonNull List<DataWrappers.ProductDetails> products) {
        tvDebug.append("\n\n***********onPricesUpdated***********\n");
        for (int i = 0; i < products.size(); i++) {
            DataWrappers.ProductDetails productDetails = products.get(i);
            tvDebug.append(productDetails.toString());
        }
    }

    @Override
    public void onProductPurchased(@NonNull DataWrappers.PurchaseInfo purchaseInfo) {
        tvDebug.append("\n\n***********onProductPurchased***********\n");
        tvDebug.append(purchaseInfo.toString());
    }

    @Override
    public void onSubscriptionPurchased(@NonNull DataWrappers.PurchaseInfo purchaseInfo) {
        tvDebug.append("\n\n***********onSubscriptionPurchased***********\n");
        tvDebug.append(purchaseInfo.toString());
    }

    @Override
    public void onSubscriptionRestored(@NonNull DataWrappers.PurchaseInfo purchaseInfo) {
        tvDebug.append("\n\n***********onSubscriptionRestored***********\n");
        tvDebug.append(purchaseInfo.toString());
    }

}