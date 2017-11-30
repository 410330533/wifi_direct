package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wzj.handover.GroupOwnerParametersCollection;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by WZJ on 2017/11/8.
 */

public class BatteryReceiver extends BroadcastReceiver {
    public static final String TAG = "BatteryReceiver";
    private WiFiDirectActivity wiFiDirectActivity;
    public static float power = 1;
    public BatteryReceiver(WiFiDirectActivity wiFiDirectActivity) {
        this.wiFiDirectActivity = wiFiDirectActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        power = intent.getExtras().getInt("level")/100.0f;
        power = (float)(Math.round(power*100))/100;
        if(power < 0.25){
            if(wiFiDirectActivity.getIsGroupOwner() && wiFiDirectActivity.getGroupOwnerParametersCollection() == null){
                Log.d(TAG, "启动GroupOwnerParametersCollection");
                final GroupOwnerParametersCollection groupOwnerParametersCollection = new GroupOwnerParametersCollection(wiFiDirectActivity, this);
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        WiFiDirectActivity.threadPoolExecutor.execute(groupOwnerParametersCollection);
                    }
                }, 1000*5);
            }

        }
        //Log.d(TAG, ""+power);
    }

    public float getPower() {
        return power;
    }
}
