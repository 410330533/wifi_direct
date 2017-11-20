package com.wzj.handover;

import android.util.Log;

import com.wzj.wifi_direct.WiFiDirectActivity;

/**
 * Created by WZJ on 2017/11/13.
 */

public class GroupOwnerParametersCollection implements Runnable {
    WiFiDirectActivity wiFiDirectActivity;
    long period = 1000*10;
    @Override
    public void run() {
        while (true){
            try {
                int rssi = wiFiDirectActivity.getRSSI(wiFiDirectActivity.getSsid());
                if(rssi < -70){
                    if(rssi > -80 && rssi < -70){
                        //选择性切换


                    }else {
                        //强制性切换
                    }
                }
                Thread.sleep(period);
            } catch (InterruptedException e) {
                Log.d("GOCollection", "线程终止！");
                break;
            }
        }

    }
}
