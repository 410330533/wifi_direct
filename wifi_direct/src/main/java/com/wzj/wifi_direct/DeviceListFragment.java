package com.wzj.wifi_direct;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Timer;

/**
 * Created by wzj on 2017/1/17.
 */
/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    private Timer autoConnectTimer;

    public Timer getAutoConnectTimer() {
        return autoConnectTimer;
    }

    public void setAutoConnectTimer(Timer autoConnectTimer) {
        this.autoConnectTimer = autoConnectTimer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        mContentView.findViewById(R.id.create_group).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceDetailFragment.preGroupSize = 0;
                ((DeviceActionListener)getActivity()).createGroup();
            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DeviceActionListener)getActivity()).disconnect();
            }
        });
        mContentView.findViewById(R.id.btn_service).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WiFiDirectActivity)getActivity()).publishServices();
            }
        });
        return mContentView;
    }

    public WifiP2pDevice getDevice(){
        return device;
    }
    private static String getDeviceStatus(int deviceStatus){
        String status = "";
        switch(deviceStatus){
            case WifiP2pDevice.AVAILABLE:
                status = "Available";
                break;
            case WifiP2pDevice.INVITED:
                status = "Invited";
                break;
            case WifiP2pDevice.CONNECTED:
                status = "Connected";
                break;
            case WifiP2pDevice.FAILED:
                status = "Failed";
                break;
            case WifiP2pDevice.UNAVAILABLE:
                status = "Unavailable";
                break;
            default:
                status = "Unknown";
                break;
        }
        //Log.d(WiFiDirectActivity.TAG, "Peer status :"+status);
        return status;
    }
    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device=(WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener)getActivity()).showDetails(device, v);
        ((DeviceActionListener)getActivity()).connect();

    }


    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {
        private List<WifiP2pDevice> items;

        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }


        @Nullable
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                //LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化；
                //而findViewById()是找xml布局文件下的具体widget控件(如Button、TextView等)。
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            final WifiP2pDevice device = items.get(position);
            if(device != null){
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if(top != null){
                    top.setText(device.deviceName);
                }
                if(bottom != null){
                    bottom.setText(getDeviceStatus(device.status));
                }
                ImageView imageView = (ImageView)v.findViewById(R.id.detailed_info);
                imageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = "address: " + device.deviceAddress + "\n";
                        message = message + device.toString();
                        showDetailedInfo(device.deviceName , message);
                    }
                });
            }
            return v;
        }
    }
    public void showDetailedInfo(String title, String info){
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(info);
        dialog.setNeutralButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    //更改本机设备信息
    public void updateThisDevice(WifiP2pDevice device){
        this.device = device;
        TextView view = (TextView)mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView)mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter)getListAdapter()).notifyDataSetChanged();
        Collection<WifiP2pDevice> p2pDevices = peerList.getDeviceList();
        if(((WiFiDirectActivity)getActivity()).isAutoConnect()){
            final WifiP2pConfig wifiP2pConfig = ((WiFiDirectActivity)getActivity()).getWifiP2pConfig();
            for (WifiP2pDevice wifiP2pDevice : p2pDevices){
                if(wifiP2pDevice.deviceAddress.equals(wifiP2pConfig.deviceAddress)){
                    Log.d("AutoConnect", "自动连接开始！");
                    ((WiFiDirectActivity)getActivity()).setAutoConnect(false);
                    Random random = new Random();
                    int ran = random.nextInt(6);
                    ((WiFiDirectActivity)getActivity()).connect(wifiP2pConfig);
                    /*autoConnectTimer = new Timer();
                    autoConnectTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(!((WiFiDirectActivity)getActivity()).getIsConnected()){
                                ((WiFiDirectActivity)getActivity()).connect(wifiP2pConfig);
                                Log.d("AutoConnect", "自动连接！");
                            }
                        }
                    },0,3000+1000*ran);*/

                }
            }
        }



        if((((WiFiDirectActivity)getActivity()).getReceiver()).isConnected()){
            for (WifiP2pDevice wifiP2pDevice : p2pDevices){
                if(wifiP2pDevice.status == 0){
                    ((WiFiDirectActivity)getActivity()).setGroupOwnerMac(wifiP2pDevice.deviceAddress);
                    break;
                }
            }
        }
        Log.d("onPeersAvailable ",""+peerList.getDeviceList().size());
        //notifyDataSetChanged方法通过一个外部的方法控制
        //如果适配器的内容改变时需要强制调用getView来刷新每个Item的内容。

        if((((WiFiDirectActivity)getActivity()).getReceiver()).isConnected()
                && peers.size() == 0){
            this.clearPeers();
        }else if(peers.size() == 0){
            //((WiFiDirectActivity)getActivity()).resetData();
            Log.d(WiFiDirectActivity.TAG, "No devices found");
            return;
        }
    }

    public void clearPeers(){
        peers.clear();
        ((WiFiPeerListAdapter)getListAdapter()).notifyDataSetChanged();
    }

    public void onInitiateDiscovery(){

        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers",
                true, true, new OnCancelListener() {
                    @Override
                    //是否真的取消发现操作？
                    public void onCancel(DialogInterface dialog) {
                        //((DeviceActionListener)getActivity()).stopDiscovery();
                    }
                });
    }
    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */

    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device, View view);

        //void cancelDisconnect();

        void connect();

        void disconnect();

        void createGroup();

        void cancel();

        void stopDiscovery();
    }

}
