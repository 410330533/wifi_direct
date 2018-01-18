package com.wzj.handover;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;

import com.wzj.bean.Member;
import com.wzj.bean.Network;
import com.wzj.handover.algorithm.FNQDAlgorithmSimple;
import com.wzj.wifi_direct.BatteryReceiver;
import com.wzj.wifi_direct.DeviceDetailFragment;
import com.wzj.wifi_direct.R;
import com.wzj.wifi_direct.WiFiDirectActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by WZJ on 2017/11/13.
 */

public class GroupOwnerParametersCollection implements Runnable {
    public static final String TAG = "GOParametersCollection";
    private WiFiDirectActivity wiFiDirectActivity;
    private BatteryReceiver batteryReceiver;
    private boolean flag = true;
    long period = 1000*30;

    public GroupOwnerParametersCollection(WiFiDirectActivity wiFiDirectActivity, BatteryReceiver batteryReceiver) {
        this.wiFiDirectActivity = wiFiDirectActivity;
        this.batteryReceiver = batteryReceiver;
    }

    @Override
    public void run() {
        while (flag){
            try {
                float power = (float)(Math.round(batteryReceiver.getPower()*100))/100;
                Log.d(TAG, "电量/"+power +" "+wiFiDirectActivity.getIsGroupOwner());
                if (wiFiDirectActivity.getIsGroupOwner()){
                    //power = 0.14f;
                    Log.d(TAG, ""+ power +" " + wiFiDirectActivity.getIsGroupOwner());
                    if(power < 0){//0.21
                        if(power > (float)(Math.round(0.1*100))/100){
                            if(wiFiDirectActivity.getGroupSize() == 0){
                                Log.d(TAG, "组主选择性切换");
                                //组主选择性切换
                                double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
                                double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
                                double t = 0.1;
                                FNQDAlgorithmSimple fnqdAlgorithm = new FNQDAlgorithmSimple(wiFiDirectActivity.getCandidateNetworks(), mParameters, weights, t);
                                Network optimalNetwork = fnqdAlgorithm.fnqdProcess();
                                //建立连接
                                WifiP2pConfig config = new WifiP2pConfig();
                                if(optimalNetwork != null) {
                                    //UpdateServicesThread.period = 1000*60;
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                    config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
                                    config.wps.setup = WpsInfo.PBC;
                                    wiFiDirectActivity.setWifiP2pConfig(config);
                                    wiFiDirectActivity.setAutoConnect(true);
                                }else {
                                    //启动服务发现，收集周围网络信息
                                    wiFiDirectActivity.setMemberServiceDiscovery(false);
                                    Log.d(TAG, "无候选网络，启动服务发现，收集周围网络信息");
                                }
                            }else {
                                //组内切换
                                Log.d(TAG, ""+"选择性组内切换");
                                //WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver = wiFiDirectActivity.getReceiver();
                                //wiFiDirectBroadcastReceiver.setLowPower(true);
                                DeviceDetailFragment detailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                                float maxPower = 0;
                                String maxMac = "";
                                Map<String, Member> lastMembers = new HashMap<>();
                                for(Map.Entry<String, Member> map : detailFragment.getMemberMap().entrySet()){
                                    lastMembers.put(map.getKey(), map.getValue());
                                    if(map.getValue().getPower() > maxPower){
                                        maxPower = map.getValue().getPower();
                                        maxMac = map.getKey();
                                    }

                                }
                                Log.d(TAG, maxMac + "/" +maxPower);
                                final String deviceAddress = maxMac;
                                //如果当前设备电量最高
                                if(detailFragment.getMyDevice().deviceAddress.equals(maxMac)){
                                    Log.d(TAG, ""+"选择性组内切换，当前设备电量最高，继续监听");
                                    //继续监听
                                }else {//若电量最高的设备不是当前设备
                                    Log.d(TAG, ""+"选择性组内切换，当前设备非电量最高");
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                    final WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                                    wifiP2pConfig.deviceAddress = deviceAddress;
                                    wiFiDirectActivity.getManager().discoverPeers(wiFiDirectActivity.getChannel(), new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            wiFiDirectActivity.setWifiP2pConfig(wifiP2pConfig);
                                            wiFiDirectActivity.setAutoConnect(true);

                                        }
                                        @Override
                                        public void onFailure(int reason) {

                                        }
                                    });
                                }
                            }

                        }else {
                            if(wiFiDirectActivity.getGroupSize() == 0){
                                //组主强制性切换
                                Log.d(TAG, ""+"组主强制性切换");
                                wiFiDirectActivity.setIsGroupOwner(false);
                                double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
                                double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
                                double t = 0;
                                FNQDAlgorithmSimple fnqdAlgorithm = new FNQDAlgorithmSimple(wiFiDirectActivity.getCandidateNetworks(), mParameters, weights, t);
                                Network optimalNetwork = fnqdAlgorithm.fnqdProcess();
                                //建立连接
                                WifiP2pConfig config = new WifiP2pConfig();
                                if(optimalNetwork != null) {
                                    //UpdateServicesThread.period = 1000*60;
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                    config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
                                    config.wps.setup = WpsInfo.PBC;
                                    wiFiDirectActivity.setWifiP2pConfig(config);
                                    wiFiDirectActivity.setAutoConnect(true);
                                }else {
                                    Log.d(TAG, ""+"组主强制性切换无最优网络");
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                }
                            }else {
                                //组内切换
                                Log.d(TAG, ""+"强制性组内切换");
                                //WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver = wiFiDirectActivity.getReceiver();
                                //wiFiDirectBroadcastReceiver.setLowPower(true);
                                DeviceDetailFragment detailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                                float maxPower = 0;
                                String maxMac = "";
                                Map<String, Member> lastMembers = new HashMap<>();
                                for(Map.Entry<String, Member> map : detailFragment.getMemberMap().entrySet()){
                                    lastMembers.put(map.getKey(), map.getValue());
                                    if(map.getValue().getPower() > maxPower){
                                        maxPower = map.getValue().getPower();
                                        maxMac = map.getKey();
                                    }
                                }
                                Log.d(TAG, maxMac + "/" +maxPower);
                                final String deviceAddress = maxMac;
                                //如果当前设备电量最高、断开连接
                                if(detailFragment.getMyDevice().deviceAddress.equals(maxMac)){
                                    Log.d(TAG, ""+"强制性组内切换，且组主电量最高");
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                    //继续监听
                                }else {//若电量最高的设备不是当前设备
                                    Log.d(TAG, ""+"强制性组内切换，当前设备非电量最高");
                                    if (wiFiDirectActivity.getIsConnected()) {
                                        wiFiDirectActivity.disconnect();
                                    }
                                    final WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                                    wifiP2pConfig.deviceAddress = deviceAddress;
                                    wiFiDirectActivity.getManager().discoverPeers(wiFiDirectActivity.getChannel(), new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            wiFiDirectActivity.setWifiP2pConfig(wifiP2pConfig);
                                            wiFiDirectActivity.setAutoConnect(true);

                                        }
                                        @Override
                                        public void onFailure(int reason) {

                                        }
                                    });
                                }
                            }

                        }
                    }
                }
                Thread.sleep(period);
            } catch (InterruptedException e) {
                Log.d(TAG, "线程终止");
            }
        }
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
