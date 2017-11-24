package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;

import com.wzj.bean.Member;
import com.wzj.handover.MemberParametersCollection;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by wzj on 2017/1/16.
 */

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectActivity activity;
    private boolean isConnected = false;
    private Map<String, Member> lastMembers = new HashMap<>();
    private boolean lowPower = false;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WiFiDirectActivity activity){
        super();
        this.manager=manager;
        this.channel=channel;
        this.activity=activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            //广播当设备的WiFi Direct模式开启或关闭
            //重新注册广播就会产生该action（resume）
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                //WiFi直连模式开启
                activity.setIsWifiP2pEnabled(true);
            }else{
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();
            }
            Log.d(WiFiDirectActivity.TAG, "P2P state changed : "+state);
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //请求可用的连接设备；异步调用；通过PeerListener.onPeersAvailable()回调
            //只有通过discovery操作后才会产生该action
            if(manager != null){
               manager.requestPeers(channel, (WifiP2pManager.PeerListListener)activity.
                       getFragmentManager().findFragmentById(R.id.frag_list));

            }else{
                activity.resetData();
                Log.d(WiFiDirectActivity.TAG, "manager is null");
            }
            Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            //广播当设备的连接状态发生改变
            //重新注册广播就会产生该action（resume）
            /*if(activity.getMemberParametersCollection() == null){
                MemberParametersCollection memberParametersCollection = new MemberParametersCollection(activity, manager, channel);
                activity.setMemberParametersCollection(memberParametersCollection);
                activity.threadPoolExecutor.execute(memberParametersCollection);
            }*/
            if(manager == null){
                Log.d(WiFiDirectActivity.TAG, "manager is null");
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //设置isConnected给DeviceListFragment,防止整个页面被重置
            this.setConnected(networkInfo.isConnected());
            activity.setIsConnected(networkInfo.isConnected());
            System.out.println("这里这里："+networkInfo.isConnected());
            if(networkInfo.isConnected()){
                //设备已连接;查找组主IP
                activity.getCandidateNetworks().clear();
                DeviceDetailFragment fragment = (DeviceDetailFragment) activity.
                        getFragmentManager().findFragmentById(R.id.frag_detail);
                //请求连接信息
                manager.requestConnectionInfo(channel, fragment);
                //请求group信息
                manager.requestGroupInfo(channel, fragment);
            }else{
                //当电量低时执行组内切换
                DeviceDetailFragment detailFragment = (DeviceDetailFragment) activity.getFragmentManager().findFragmentById(R.id.frag_detail);
                /*if(lowPower && activity.getIsGroupOwner()){
                    //组主组内切换
                    this.handoverWithinGroup();
                }else if(!activity.getIsGroupOwner() && detailFragment.getMemberMap().size() != 0){
                    this.handoverWithinGroup();
                }*/
                activity.resetData();
                detailFragment.closeConnections();
                if(activity.getMemberParametersCollection() == null){
                    MemberParametersCollection memberParametersCollection = new MemberParametersCollection(activity, manager, channel);
                    activity.setMemberParametersCollection(memberParametersCollection);
                    activity.threadPoolExecutor.execute(memberParametersCollection);
                    Log.d(WiFiDirectActivity.TAG, "创建组员监听线程！！！");
                }
                //重置定时器
                if(detailFragment.getTimer()!=null){
                    detailFragment.getTimer().cancel();
                    detailFragment.setTimer(null);
                    detailFragment.setComputeBandwidth(null);
                }
                Log.d(WiFiDirectActivity.TAG, "disconnection");
            }
            Log.d(WiFiDirectActivity.TAG, "P2P connection changed");
        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //广播本机设备细节发生改变
            //重新注册广播就会产生该action（resume）
            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager().findFragmentById(R.id.frag_list);
            DeviceDetailFragment detailFragment = (DeviceDetailFragment) activity.getFragmentManager().findFragmentById(R.id.frag_detail);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
            detailFragment.setMyDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

            Log.d(WiFiDirectActivity.TAG, "P2P device's detail changed");
        }else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
            if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                Log.d("WFDBroadcastReceiver", "设备发现停止！");
            }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                Log.d("WFDBroadcastReceiver", "设备发现开始！");
            }else{
                Log.d("WFDBroadcastReceiver", "设备发现未知错误！");
            }
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void handoverWithinGroup(){
        DeviceDetailFragment detailFragment = (DeviceDetailFragment) activity.getFragmentManager().findFragmentById(R.id.frag_detail);
        float maxPower = 0;
        String maxMac = "";
        for(Map.Entry<String, Map<String, Member>> map : detailFragment.getMemberMap().entrySet()){
            for(Map.Entry<String, Member> mapE : map.getValue().entrySet()){
                lastMembers.put(map.getKey(), mapE.getValue());
                if(mapE.getValue().getPower() > maxPower){
                    maxPower = mapE.getValue().getPower();
                    maxMac = map.getKey();
                }
            }
        }
        Log.d("handoverWithinGroup", maxMac + "/" +maxPower);
        final String deviceAddress = maxMac;
        //如果当前设备电量最高，主动创建组
        if(detailFragment.getMyDevice().deviceAddress.equals(maxMac)){
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    activity.createGroup();
                }
            }, 2000);
        }else {//若电量最高的设备不是当前设备
            final WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
            wifiP2pConfig.deviceAddress = deviceAddress;
            manager.discoverPeers(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            activity.connect(wifiP2pConfig);
                        }
                    }, 3000);

                }
                @Override
                public void onFailure(int reason) {

                }
            });
            /*Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    final WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                    wifiP2pConfig.deviceAddress = deviceAddress;
                    manager.discoverPeers(channel, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            try {
                                Thread.sleep(3000);
                                activity.connect(wifiP2pConfig);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                        @Override
                        public void onFailure(int reason) {

                        }
                    });

                }
            }, 2000);*/
        }

    }

    public boolean isLowPower() {
        return lowPower;
    }

    public void setLowPower(boolean lowPower) {
        this.lowPower = lowPower;
    }
}
