package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;


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
    public WiFiDirectBroadcastReceiver() {
    }

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
            if(manager == null){
                Log.d(WiFiDirectActivity.TAG, "manager is null");
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //设置isConnected给DeviceListFragment,防止整个页面被重置
            this.setConnected(networkInfo.isConnected());
            if(networkInfo.isConnected()){
                //设备已连接;查找组主IP
                DeviceDetailFragment fragment = (DeviceDetailFragment) activity.
                        getFragmentManager().findFragmentById(R.id.frag_detail);
                //请求连接信息
                manager.requestConnectionInfo(channel, fragment);
                //请求group信息
                manager.requestGroupInfo(channel, fragment);
            }else{
                activity.resetData();
                DeviceDetailFragment detailFragment = (DeviceDetailFragment) activity.getFragmentManager().findFragmentById(R.id.frag_detail);
                detailFragment.closeConnections();
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
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }


}
