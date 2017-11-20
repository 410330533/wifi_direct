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
    private float power;

    public BatteryReceiver(WiFiDirectActivity wiFiDirectActivity) {
        this.wiFiDirectActivity = wiFiDirectActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " " + wiFiDirectActivity.getIsGroupOwner());
        power = intent.getExtras().getInt("level")/100.0f;
        if (wiFiDirectActivity.getIsGroupOwner()){
            Log.d(TAG, ""+ power +" " + wiFiDirectActivity.getIsGroupOwner());
            if(power < 0.2){
                if(power < 0.2 && power > 0.1){
                    if(wiFiDirectActivity.getGroupSize() == 0){
                        Log.d(TAG, ""+"组主选择性切换");
                        //组主选择性切换
                    }else {
                        //组内切换
                    }

                }else {
                    if(wiFiDirectActivity.getGroupSize() == 0){
                        //组主强制性切换
                    }else {
                        //组内切换
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
