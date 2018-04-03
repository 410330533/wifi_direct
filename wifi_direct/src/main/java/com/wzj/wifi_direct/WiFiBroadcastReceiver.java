package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.wzj.communication.MulticastThread;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by WZJ on 2018/1/20.
 */

public class WiFiBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "WiFiBroadcastReceiver";
    private WifiManager wifiManager;
    private WiFiDirectActivity wiFiDirectActivity;
    private boolean isWFDConnected = false;

    public WiFiBroadcastReceiver(WifiManager wifiManager, WiFiDirectActivity wiFiDirectActivity) {
        this.wifiManager = wifiManager;
        this.wiFiDirectActivity = wiFiDirectActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)){
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(null != networkInfo && networkInfo.isConnected()){
                Log.d(TAG, "WiFi网络已连接！");
                WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                String ssid = wifiInfo.getSSID();
                int intIPAddress = wifiInfo.getIpAddress();
                String ipAddress = String.format("%d.%d.%d.%d", (intIPAddress & 0xff), (intIPAddress >> 8 & 0xff), (intIPAddress >> 16 & 0xff),
                        (intIPAddress >> 24 & 0xff));
                Log.d(TAG, ssid +"/"+ ipAddress);
                String ssidRegrex = "^\"DIRECT-[a-zA-Z 0-9]+-[a-zA-Z _0-9]+\"";
                String ipRegrex = "^192.168.49.[0-9]+";
                Pattern ssidPattern = Pattern.compile(ssidRegrex);
                Matcher ssidMatcher = ssidPattern.matcher(ssid);
                Pattern ipPattern = Pattern.compile(ipRegrex);
                Matcher ipMatcher = ipPattern.matcher(ipAddress);
                if(ssidMatcher.matches() && ipMatcher.matches()){
                    Log.d(TAG, "以legacy连接到WiFi Direct网络！");
                    DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                    deviceDetailFragment.setMulticastWlanRead();
                    deviceDetailFragment.setPeriodMulticastWlanWrite();
                    isWFDConnected = true;
                }else {
                    isWFDConnected = false;
                }

            }else {
                Log.d(TAG, "WiFi网络连接状态改变！");
                DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                MulticastThread multicastWlanRead = deviceDetailFragment.getMulticastWlanRead();
                if(multicastWlanRead != null){
                    multicastWlanRead.close();
                    deviceDetailFragment.setMulticastWlanRead(null);
                }
                isWFDConnected = false;
            }
        }
    }

    public boolean isWFDConnected() {
        return isWFDConnected;
    }
}
