package com.wzj.util;

import java.util.TimerTask;

import static com.wzj.wifi_direct.WiFiDirectActivity.dataSize;

/**
 * Created by WZJ on 2017/11/19.
 */

public class ComputeBandwidth extends TimerTask {
    long preDataSize = 0;
    double bandwidth = 0;

    @Override
    public void run() {
        synchronized (dataSize){
            bandwidth = (dataSize - preDataSize)/1000000.0;
            //Log.d("带宽", "" + dataSize/1000000.0+" "+preDataSize/1000000.0+" "+bandwidth+"Mbps" );
            preDataSize = dataSize;
        }
    }
    public double getBandwidth() {
        return bandwidth;
    }
}
