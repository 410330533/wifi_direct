package com.wzj.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wzj.wifi_direct.DeviceListFragment.DeviceActionListener;


/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends AppCompatActivity implements ChannelListener, DeviceActionListener{

    public static final String TAG = "WifiDirectDemo";
    private WifiP2pManager manager;
    private WifiManager wifiManager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter  = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled){
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public BroadcastReceiver getReceiver() {
        return receiver;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        //wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        //WifiMulticastLock.allowMulticast(wifiManager);
        channel = manager.initialize(this, getMainLooper(), null);


    }
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        /*Class[] method = WifiP2pManager.class.getClasses();
        for(Class m : method){
            System.out.println(m.getName());
        }*/

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        //resume时扫描可用设备
        if(manager != null){
            manager.requestPeers(channel, (WifiP2pManager.PeerListListener)this.
                    getFragmentManager().findFragmentById(R.id.frag_list));
            Log.d(WiFiDirectActivity.TAG, "requestPeers while resume");
        }
       /* try {
            Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();
            while (networkInterface.hasMoreElements()){
                NetworkInterface n =networkInterface.nextElement();
                System.out.println(n.toString());
            }
            System.out.println(NetworkInterface.getByInetAddress(InetAddress.getByName("192.168.49.207")).getName());

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        System.out.println("销毁了！！！！！！！");
        //this.disconnect();
        super.onDestroy();
        //WifiMulticastLock.stopMulticast();

    }


    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData(){
        DeviceListFragment fragmentList = (DeviceListFragment)getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment)getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if(fragmentList !=null){
            fragmentList.clearPeers();
        }
        if(fragmentDetails != null){
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.atn_direct_enable:
                if(manager != null && channel !=null){
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    // 转到设置页面
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }else {
                    Log.e(TAG, "channel or manager is null!");
                }
                return true;
            case R.id.atn_direct_discover:
                if(!isWifiP2pEnabled){
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return  true;
                }
                final DeviceListFragment fragment = (DeviceListFragment)getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                //An initiated discovery request from an application stays active until the device starts connecting to a peer ,
                //forms a p2p group or there is an explicit stopPeerDiscovery(WifiP2pManager.Channel, WifiP2pManager.ActionListener).
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new ActionListener(){
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed"+reason,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void showDetails(WifiP2pDevice device, View view) {
        DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device, view);
    }

    @Override
    public void connect() {
        DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        final WifiP2pConfig wifiP2pConfig = fragment.connect();
        if(wifiP2pConfig != null){
            manager.connect(channel, wifiP2pConfig, new ActionListener() {
                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                    System.out.println("成功了！！！！ "+ wifiP2pConfig.groupOwnerIntent);
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry."+reason,
                            Toast.LENGTH_SHORT).show();
                    /*manager.cancelConnect(channel, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(WiFiDirectActivity.this, "Connect abort request failed, Reason Code: "
                                    +reason, Toast.LENGTH_SHORT).show();
                        }
                    });*/
                }
            });
        }

    }

    public void removeGroup() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                if(fragment.getView() != null){
                    fragment.getView().setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Disconnect failed. Reason:"+reason);
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if(manager != null && !retryChannel){
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_SHORT).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        }else {
            Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    public  void cancel(){
        manager.cancelConnect(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect abort request failed, Reason Code: "
                        +reason, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void disconnect() {
         /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        DeviceDetailFragment detailFragment = (DeviceDetailFragment)getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        detailFragment.disconnect();
        if(manager != null){
            final DeviceListFragment fragment = (DeviceListFragment)getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            //已经连接上，建立了组 removeGroup()
            if(fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED){
                removeGroup();
                //未建立组，处于协商阶段 cancelConnect()
            }else if (fragment.getDevice().status == WifiP2pDevice.INVITED){
                manager.cancelConnect(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WiFiDirectActivity.this, "Connect abort request failed, Reason Code: "
                                +reason, Toast.LENGTH_SHORT).show();
                    }
                });
            }else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE){
                Toast.makeText(WiFiDirectActivity.this, "no connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void createGroup() {
        manager.createGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Create group successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Failed to create group!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void stopDiscovery() {
        manager.stopPeerDiscovery(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Stop discovery!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Stop discovery failed! "+ reason, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
