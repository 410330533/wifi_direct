package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by WZJ on 2017/11/8.
 */

public class BatteryReceiver extends BroadcastReceiver {
    public static final String TAG = "BatteryReceiver";
    private WiFiDirectActivity wiFiDirectActivity;
    public static float power ;

    public BatteryReceiver(WiFiDirectActivity wiFiDirectActivity) {
        this.wiFiDirectActivity = wiFiDirectActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " " + wiFiDirectActivity.getIsGroupOwner());
        power = intent.getExtras().getInt("level")/100.0f;
        if (wiFiDirectActivity.getIsGroupOwner()){
            //power = 0.14f;
            Log.d(TAG, ""+ power +" " + wiFiDirectActivity.getIsGroupOwner());
            if(power <= 0.2){
                if(power <= 0.2 && power > 0.1){
                    if(wiFiDirectActivity.getGroupSize() == 0){
                        Log.d(TAG, ""+"组主选择性切换");
                        //组主选择性切换
                    }else {
                        //组内切换
                        Log.d(TAG, ""+"组内切换");
                        WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver = wiFiDirectActivity.getReceiver();
                        wiFiDirectBroadcastReceiver.setLowPower(true);
                        wiFiDirectActivity.disconnect();
                    }

                }else {
                    if(wiFiDirectActivity.getGroupSize() == 0){
                        //组主强制性切换
                        Log.d(TAG, ""+"组主强制性切换");
                    }else {
                        //组内切换
                        Log.d(TAG, ""+"组内切换");
                    }

                }
            }
        }

        //Log.d(TAG, ""+power);
    }

    public float getPower() {
        return power;
    }
}
