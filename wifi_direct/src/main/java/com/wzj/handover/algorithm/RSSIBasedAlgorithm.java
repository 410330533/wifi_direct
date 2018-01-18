package com.wzj.handover.algorithm;

import android.util.Log;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by WZJ on 2017/11/16.
 */

public class RSSIBasedAlgorithm {
    public static final String TAG = "RSSIBasedAlgorithm";
    private Map<String, Network> candidateNetwork;
    private float rssi;
    private int t;

    public RSSIBasedAlgorithm(Map<String, Network> candidateNetwork, float rssi, int t) {
        this.candidateNetwork = candidateNetwork;
        this.rssi = rssi;
        this.t = t;
    }

    public Network process(){
        Log.d(TAG, "开始处理-----------------  " );
        Log.d(TAG, "当前网络RSSI：" +rssi);
        Network optimalNetwork = null;
        Network currentNetwork = null;
        double maxRssi = -100;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Network network = entry.getValue();
            if(!network.isGroupOwner()){
                if(network.getRssi() > maxRssi && network.getRssi() - rssi > t){
                    maxRssi = network.getRssi();
                    optimalNetwork = network;
                }
            }else {
                currentNetwork = network;
            }
        }
        if(optimalNetwork != null){
            Log.d(TAG, "最优切换网络："+optimalNetwork.getWifiP2pDevice().deviceName+" " + maxRssi);
            optimalNetwork.setGroupOwner(true);
            if(currentNetwork != null){
                currentNetwork.setGroupOwner(false);
            }
            for(Entry<String, Network> entry : candidateNetwork.entrySet()){
                if(!entry.getValue().isGroupOwner()){
                    //candidateNetwork.remove(entry.getKey());
                }
            }
        }else {
            Log.d(TAG, "无最优候选网络 "+ maxRssi);
        }
        Log.d(TAG, "处理完成-----------------");
        return optimalNetwork;

    }
}
