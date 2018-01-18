package com.wzj.handover;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

import com.wzj.bean.Network;
import com.wzj.handover.algorithm.FNQDAlgorithmSimple;
import com.wzj.wifi_direct.WiFiDirectActivity;

/**
 * Created by WZJ on 2017/11/13.
 */

public class MemberParametersCollection implements Runnable {
    WiFiDirectActivity wiFiDirectActivity;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private boolean flag = true;

    public MemberParametersCollection(WiFiDirectActivity wiFiDirectActivity, WifiP2pManager wifiP2pManager, Channel channel) {
        this.wiFiDirectActivity = wiFiDirectActivity;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    long period = 1000*10;
    @Override
    public void run() {
        while (flag){
            try {
                int rssi = wiFiDirectActivity.getRSSI(wiFiDirectActivity.getSsid());
                rssi = -90;
                double t = 0;
                double ct = 0;
                int rt = 0;
                //boolean flag = false;
                //自动成组
                /*if(!wiFiDirectActivity.getIsConnected() && wiFiDirectActivity.getCandidateNetworks().size() > 0){
                    //当设备未加入任何组时，进行强制性切换，此时只需要在周围存在的网络中选择出较好的网络加入（不涉及到当前网络相关计算）
                    flag = true;
                    rssi = -90;
                }*/
                Log.d("MPCollection", ""+ rssi + " " +wiFiDirectActivity.getSsid());
                if((wiFiDirectActivity.getIsConnected() && wiFiDirectActivity.getGroupOwnerFind()) && wiFiDirectActivity.getCandidateNetworks().size() > 1 ){//||falg
                    Log.d("MPCollection", "切换判决");
                    if(rssi <= -45){
                        //UpdateServicesThread.period = 1000*10;
                        if(rssi > -55){
                            //组员选择性切换
                            Log.d("MPCollection", "组员选择性切换");
                            rt = 20;
                            ct = 0.2;
                            t = 0.2;
                        }else {
                            //组员强制性切换
                            Log.d("MPCollection", "组员强制性切换");
                        }

                        //隶属函数中的M1、M2、M3
                        //double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 50, 100}, {0, 0.5, 1}};
                        double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
                        //权重向量
                            /*double weights[] = new double[]{0.22, 0.1, 0.47, 0.21};
                            double weightsR[] = new double[]{0.22, -0.1, 0.47, 0.21};*/
                        double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
                        double weightsC[] = new double[]{0.38, -0.17, 0.34, 0.11};
                        //PEV阈值

                        //Network currentNetwork = new Network(null, wiFiDirectActivity.getRSSI("^DIRECT-[a-zA-Z 0-9]+-[a-zA-Z _0-9]+"), wiFiDirectActivity.getLoadBalance(100), wiFiDirectActivity.getPower());
                        /*RSSIBasedAlgorithm rssiBasedAlgorithm = new RSSIBasedAlgorithm(wiFiDirectActivity.getCandidateNetworks(), wiFiDirectActivity.getRSSI(wiFiDirectActivity.getSsid()), rt);
                        rssiBasedAlgorithm.process();
                        CostFunctionBasedAlgorithm costFunctionBasedAlgorithm = new CostFunctionBasedAlgorithm(wiFiDirectActivity.getCandidateNetworks(), ct, weightsC);
                        costFunctionBasedAlgorithm.process();*/
                        FNQDAlgorithmSimple fnqdAlgorithm = new FNQDAlgorithmSimple(wiFiDirectActivity.getCandidateNetworks(), mParameters, weights, t);
                        final Network optimalNetwork = fnqdAlgorithm.fnqdProcess();
                        //建立连接
                        final WifiP2pConfig config = new WifiP2pConfig();
                        if(optimalNetwork != null){
                            //UpdateServicesThread.period = 1000*60;
                            if(wiFiDirectActivity.getIsConnected()){
                                wiFiDirectActivity.disconnect();
                            }
                            config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
                            config.wps.setup = WpsInfo.PBC;
                            wiFiDirectActivity.setWifiP2pConfig(config);
                            wiFiDirectActivity.setAutoConnect(true);
                            /*Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
                                    config.wps.setup = WpsInfo.PBC;
                                    config.groupOwnerIntent = 0;
                                    wiFiDirectActivity.connect(config);
                                }
                            },5000);*/
                            /*wifiP2pManager.discoverPeers(channel, new ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Timer timer = new Timer();
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            config.deviceAddress = optimalNetwork.getWifiP2pDevice().deviceAddress;
                                            config.wps.setup = WpsInfo.PBC;
                                            config.groupOwnerIntent = 0;
                                            wiFiDirectActivity.connect(config);
                                        }
                                    },5000);
                                    //Thread.sleep(3000);

                                }

                                @Override
                                public void onFailure(int reason) {

                                }
                            });*/
                        }
                    }
                }

                Thread.sleep(period);
            } catch (InterruptedException e) {
                Log.d("MPCollection", "线程终止！");
                break;
            }
        }

    }
}
