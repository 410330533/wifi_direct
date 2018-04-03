package com.wzj.util;

import android.util.Log;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by WZJ on 2018/1/25.
 */

public class AddressObtain {
    private static String mMACofWiFi;
    private static String mMACofP2P;
    public static String getWlanIPAddress(){
        String address = "";
        return address;
    }

    public static String getP2pIPAddress(){
        String address = "";
        return address;
    }

    public static String getWlanMACAddress(){
        if(mMACofWiFi == null){
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                String regrex = "^wlan";
                Pattern pattern = Pattern.compile(regrex);
                while(networkInterfaces.hasMoreElements()){
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Matcher matcher = pattern.matcher(networkInterface.getName());
                    if(matcher.find()){
                       mMACofWiFi = getMacFromBytes(networkInterface.getHardwareAddress());
                        Log.d("NetworkInterface", "匹配/"+networkInterface.getName()+"/"+ mMACofWiFi);
                        break;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            return mMACofWiFi;
        }else {
            return mMACofWiFi;
        }


    }

    public static String getP2pMACAddress(){
        if(mMACofP2P == null){
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                String regrex = "^p2p";
                Pattern pattern = Pattern.compile(regrex);
                while(networkInterfaces.hasMoreElements()){
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Matcher matcher = pattern.matcher(networkInterface.getName());
                    if(matcher.find()){
                        mMACofP2P = getMacFromBytes(networkInterface.getHardwareAddress());
                        Log.d("NetworkInterface", "匹配/"+networkInterface.getName()+"/"+ mMACofP2P);
                        break;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            return mMACofP2P;
        }else {
            return mMACofP2P;
        }

    }

    private static String getMacFromBytes(byte[] macBytes) {
        String mac = "";
        for(int i = 0;i < macBytes.length; i++){
            String sTemp = Integer.toHexString(0xFF &  macBytes[i]);
            if(sTemp.length() == 1){
                mac += "0" + sTemp + ":";
            }else {
                mac += sTemp + ":";
            }

        }
        mac = mac.substring(0,mac.lastIndexOf(":"));
        return mac;
    }
}
