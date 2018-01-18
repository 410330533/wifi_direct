package com.wzj.handover.algorithm;

import android.util.Log;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by WZJ on 2017/11/16.
 */

public class CostFunctionBasedAlgorithm {
    public static final String TAG = "CFBdAlgorithm";
    private Map<String, Network> candidateNetwork;
    private double t;
    private double[] weights;

    public CostFunctionBasedAlgorithm(Map<String, Network> candidateNetwork, double t, double[] weights) {
        this.candidateNetwork = candidateNetwork;
        this.t = t;
        this.weights = weights;
    }

    public Network process(){
        Log.d(TAG, "开始处理------------------------");
        Network optimalNetwork = null;
        Network currentNetwork = null;
        double cPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Network network = entry.getValue();
            if(network.isGroupOwner()){
                double[] factors = network.getFactors();
                factors[0] = (factors[0] + 100) / 100;
                for(int i = 0; i<factors.length; i++){
                    cPEV += weights[i] * factors[i];
                }
                Log.d(TAG, "计算组主PEV "+ cPEV);
                currentNetwork = network;
            }
        }
        double maxPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Network network = entry.getValue();
            if(!network.isGroupOwner()){
                double[] factors = network.getFactors();
                double pev = 0;
                factors[0] = (factors[0] + 100) / 100;
                for(int i = 0; i<factors.length; i++){
                    pev += weights[i] * factors[i];
                }
                Log.d(TAG, ""+pev);
                if(pev > maxPEV && pev - cPEV > t){
                    maxPEV = pev;
                    optimalNetwork = network;
                }
            }

        }
        if(optimalNetwork != null){
            Log.d(TAG, "最优切换网络："+optimalNetwork.getWifiP2pDevice().deviceName + " " +maxPEV);
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
            Log.d(TAG, "无最优候选网络 "+ maxPEV);
        }
        Log.d(TAG, "处理完成------------------------");
        return optimalNetwork;
    }
}
