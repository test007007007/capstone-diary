package com.yourorg.sample;

import android.content.Context;
import android.net.wifi.WifiManager;


public final class WifiCheck {

    public static boolean enableWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().
                getSystemService(Context.WIFI_SERVICE);
        return wifiManager.setWifiEnabled(true);
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().
                getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }
}
