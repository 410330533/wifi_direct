package com.wzj.wifi_direct;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
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

import com.wzj.bean.Member;
import com.wzj.bean.Network;
import com.wzj.handover.MemberParametersCollection;
import com.wzj.handover.UpdateServicesThread;
import com.wzj.service.SocketService;
import com.wzj.wifi_direct.DeviceListFragment.DeviceActionListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends AppCompatActivity implements ChannelListener, DeviceActionListener,
            WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.DnsSdTxtRecordListener{

    public static final String TAG = "WifiDirectDemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private boolean isGroupOwner = false;
    private boolean isConnected = false;
    private boolean groupOwnerFind = false;
    private String groupOwnerMac = "";
    private String ssid = "";
    private int groupSize;
    private final IntentFilter intentFilter  = new IntentFilter();
    private final IntentFilter batteryIntentFilter  = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver = null;
    private BatteryReceiver batteryReceiver = null;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    public static ThreadPoolExecutor threadPoolExecutor;
    private Map<String, Network> candidateNetworks = new ConcurrentHashMap<>();
    private static MemberParametersCollection memberParametersCollection;
    public static Long dataSize = Long.valueOf(0);
    private Map<String, Member> lastMembers = new HashMap<>();
    private SocketService socketService;
    private ServiceConnection serviceConnection;

    public boolean getGroupOwnerFind() {
        return groupOwnerFind;
    }

    public void setGroupOwnerFind(boolean groupOwnerFind) {
        this.groupOwnerFind = groupOwnerFind;
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled){
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    public MemberParametersCollection getMemberParametersCollection() {
        return memberParametersCollection;
    }

    public void setMemberParametersCollection(MemberParametersCollection memberParametersCollection) {
        this.memberParametersCollection = memberParametersCollection;
    }
    public Map<String, Network> getCandidateNetworks() {
        return candidateNetworks;
    }

    public WiFiDirectBroadcastReceiver getReceiver() {
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
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        batteryIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

        manager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        if(null == threadPoolExecutor || threadPoolExecutor.isTerminated()){
            threadPoolExecutor = new ThreadPoolExecutor(CPU_COUNT + 1, CPU_COUNT*2 + 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));
        }
        if(!isGroupOwner){
            memberParametersCollection = new MemberParametersCollection(this, manager, channel);
            threadPoolExecutor.execute(memberParametersCollection);
        }
        //发布服务
        /*manager.setDnsSdResponseListeners(channel, this, this);
        String instanceName = "HandoverParameters";
        String serviceType = "_handover._tcp";
        UpdateServicesThread updateServicesThread = new UpdateServicesThread(this, manager, channel, instanceName, serviceType);
        threadPoolExecutor.execute(updateServicesThread);*/

    }
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        batteryReceiver = new BatteryReceiver(this);
        registerReceiver(receiver, intentFilter);
        registerReceiver(batteryReceiver, batteryIntentFilter);
        //resume时扫描可用设备
        if(manager != null){
            manager.requestPeers(channel, (WifiP2pManager.PeerListListener)this.
                    getFragmentManager().findFragmentById(R.id.frag_list));
            Log.d(WiFiDirectActivity.TAG, "requestPeers while resume");
        }

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
        //this.removeServices();
        Log.d("WiFiP2pService", "----------------------！");

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(batteryReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "清理当前所有service");
        this.removeServices();
        Log.d(TAG, "终止线程池");
        threadPoolExecutor.shutdownNow();
        //this.disconnect();
        super.onDestroy();

    }

    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(socketService == null){
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    socketService = ((SocketService.MBinder)service).getService();
                    DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
                    socketService.setTcpConnections(deviceDetailFragment.getTcpConnections());
                    System.out.println("service连接上了！！！！！！！！！");
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    socketService = null;
                }
            };
        }
        Intent serviceIntent = new Intent(this, SocketService.class);
        this.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
        Gson gson = new Gson();
        outState.putString("memberMap", gson.toJson(deviceDetailFragment.getMemberMap()));
    }*/

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
        setIsGroupOwner(false);
        setIsConnected(false);
        setGroupOwnerMac("");
        setGroupOwnerFind(false);
        setSsid("");
        candidateNetworks.clear();
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
        WifiP2pConfig wifiP2pConfig = fragment.connect();
        if(wifiP2pConfig != null){
            candidateNetworks.clear();
            manager.connect(channel, wifiP2pConfig, new ActionListener() {
                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
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
    public void connect(WifiP2pConfig wifiP2pConfig) {

        if(wifiP2pConfig != null){
            candidateNetworks.clear();
            manager.connect(channel, wifiP2pConfig, new ActionListener() {
                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
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

        /*double mParameters[][] = new double[][]{{-100, -60, -30}, {0, 0.5, 1}, {0, 50, 100}, {0, 0.5, 1}};
        double weights[] = new double[]{0.22, 0.1, 0.47, 0.21};
        double t = 0;
        Network currentNetwork = new Network(null, getRSSI("^DIRECT-[a-zA-Z 0-9]+-[a-zA-Z _0-9]+"), getLoadBalance(100), getPower());
        FNQDAlgorithmSimple fnqdAlgorithm = new FNQDAlgorithmSimple(currentNetwork, candidateNetworks, mParameters, weights, t);
        Network optimalNetwork = fnqdAlgorithm.fnqdProcess(currentNetwork, candidateNetworks, mParameters, weights, t);
        WifiP2pConfig config = new WifiP2pConfig();
        //所需连接设备的MAC地址
        //通过showDetail()方法初始化device,为列表中所选中的device
        if(optimalNetwork != null){
            config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
            this.connect(config);
        }*/
        //currentTime = System.currentTimeMillis();
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

    public double getPower(){
        return batteryReceiver.getPower();
    }

    public double getLoadBalance(int maxSize){
        return Float.valueOf(groupSize)/Float.valueOf(maxSize);

    }
    public double getBandwidth(float maxBandwidth){
        double availableBandwidth = 0;
        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
        Double bandwidth = deviceDetailFragment.getComputeBandwidth().getBandwidth();
        availableBandwidth = (maxBandwidth - bandwidth)/maxBandwidth;
        return availableBandwidth;
    }

    public int getRSSI(String regrex){
        int rssi = -100;
        if(!("").equals(regrex)){
            WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Pattern pattern = Pattern.compile(regrex);
            //Log.d(WiFiDirectActivity.TAG,""+scanResults.size());
            if(null != pattern){
                for(ScanResult scanResult : scanResults){
                    Matcher matcher = pattern.matcher(scanResult.SSID);
                    if(matcher.matches()){
                /*Log.d(WiFiDirectActivity.TAG, scanResult.SSID);
                Log.d(WiFiDirectActivity.TAG, scanResult.BSSID);
                Log.d(WiFiDirectActivity.TAG, ""+scanResult.level);*/
                        rssi = scanResult.level;

                    }
                }
            }
        }

        return rssi;
    }
    public void publishServices(){

        manager.setDnsSdResponseListeners(channel, this, this);
        String instanceName = "HandoverParameters";
        String serviceType = "_handover._tcp";
        UpdateServicesThread updateServicesThread = new UpdateServicesThread(this, manager, channel, instanceName, serviceType);
        threadPoolExecutor.execute(updateServicesThread);

        /*WifiP2pDnsSdServiceRequest wifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(instanceName, serviceType);
        manager.addServiceRequest(channel, wifiP2pDnsSdServiceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("WiFiP2pService", "添加service discovery request成功！");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("WiFiP2pService", "添加service discovery request失败！" + reason);
            }
        });*/
        //设置Listener


    }

    public void removeServices(){
        manager.clearLocalServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("WiFiP2pService", "清理service成功");
                manager.clearServiceRequests(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("WiFiP2pService", "清理service request成功！");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d("WiFiP2pService", "清理service request失败！" + reason);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d("WiFiP2pService", "清理service失败 " + reason);
            }
        });
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        Log.d("DnsSdServiceAvailable", instanceName + " " + registrationType + " " + srcDevice.deviceName);
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
        Log.d("DnsSdTxtRecordAvailable", fullDomainName + " " + txtRecordMap.get("power") + " " + srcDevice.deviceName);
        UpdateServicesThread.time = System.currentTimeMillis();
        String ssid = null;
        if(groupOwnerMac.equals(srcDevice.deviceAddress)){
            ssid = txtRecordMap.get("ssid");
            Log.d("candidateNetworks", "找到组主 "+ssid+ " / " +txtRecordMap.get("bandwidth"));
            int rssi = this.getRSSI(ssid);
            Network network = new Network(srcDevice, Double.valueOf(rssi), Double.valueOf(txtRecordMap.get("loadbalance")), Double.valueOf(txtRecordMap.get("bandwidth")), Double.valueOf(txtRecordMap.get("power")), true);
            candidateNetworks.put(srcDevice.deviceAddress, network);
            this.setGroupOwnerFind(true);
        }else if (!candidateNetworks.containsKey(srcDevice.deviceAddress)) {
            ssid = txtRecordMap.get("ssid");
            Log.d("candidateNetworks", "候选网络增加 " + ssid+ " / " +txtRecordMap.get("bandwidth"));
            int rssi = this.getRSSI(ssid);
            Network network = new Network(srcDevice, Double.valueOf(rssi), Double.valueOf(txtRecordMap.get("loadbalance")), Double.valueOf(txtRecordMap.get("bandwidth")), Double.valueOf(txtRecordMap.get("power")), false);
            candidateNetworks.put(srcDevice.deviceAddress, network);
        }
        for (Entry<String, Network> entry : candidateNetworks.entrySet()) {
            Log.d("candidateNetworks", entry.getKey() + " " +entry.getValue().getWifiP2pDevice().deviceName+" "+entry.getValue().getRssi());
        }


    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public boolean getIsGroupOwner() {
        return isGroupOwner;
    }

    public void setIsGroupOwner(boolean groupOwner) {
        isGroupOwner = groupOwner;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean connected) {
        isConnected = connected;
    }

    public String getGroupOwnerMac() {
        return groupOwnerMac;
    }

    public void setGroupOwnerMac(String groupOwnerMac) {
        this.groupOwnerMac = groupOwnerMac;
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public Channel getChannel() {
        return channel;
    }

    public Map<String, Member> getLastMembers() {
        DeviceDetailFragment detailFragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
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
        return lastMembers;
    }

    public void setLastMembers(Map<String, Member> lastMembers) {
        this.lastMembers = lastMembers;
    }


}
