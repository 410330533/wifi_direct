package com.wzj.util;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

/**
 * Created by wzj on 2017/6/1.
 */

public class WifiMulticastLock {
    private static MulticastLock multicastLock;
    public static void allowMulticast(WifiManager wifiManager){
        multicastLock = wifiManager.createMulticastLock("multicastLock");
        multicastLock.acquire();
        System.out.println("WiFi组播开启！！！"+wifiManager.getConnectionInfo().getSSID()+" "+wifiManager.getConnectionInfo().getLinkSpeed());
    }
    public static void stopMulticast(){
        multicastLock.release();
        System.out.println("WiFi组播关闭！！！");
    }
}
