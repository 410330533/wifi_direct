package com.wzj.handover;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import com.wzj.wifi_direct.WiFiDirectActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by WZJ on 2017/11/9.
 */

public class UpdateServicesThread implements Runnable, WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.DnsSdTxtRecordListener{
    public static final String  TAG = "UpdateServicesThread";
    public static final long TIME_OUT = 1000*60*20;
    public static long period = 1000*10;
    public static long time = System.currentTimeMillis();
    private WiFiDirectActivity wiFiDirectActivity;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private String instanceName;
    private String serviceType;
    private Map<String, String> txtRecordMap = new HashMap<>();

    public UpdateServicesThread(WiFiDirectActivity wiFiDirectActivity, WifiP2pManager wifiP2pManager, Channel channel, String instanceName, String serviceType) {
        this.wiFiDirectActivity = wiFiDirectActivity;
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.instanceName = instanceName;
        this.serviceType = serviceType;
    }

    @Override
    public void run() {
        //周期性更新local service
        while (true){
            try {
                //在一定的时间间隔不能获取到service，执行关闭/开启Wi-Fi操作
                /*if(System.currentTimeMillis() - time > TIME_OUT){
                    WifiManager wifiManager = (WifiManager)wiFiDirectActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    Log.d("service获取超时：","关闭/开启Wi-Fi");
                    wifiManager.setWifiEnabled(false);
                    wifiManager.setWifiEnabled(true);
                    Thread.sleep(1000);
                    wiFiDirectActivity.getCandidateNetworks().clear();
                    time = System.currentTimeMillis();
                }*/
                if(wiFiDirectActivity.isMemberServiceDiscovery()){
                    wifiP2pManager.clearLocalServices(channel, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "service清理成功！");
                            txtRecordMap.put("ssid", "" + wiFiDirectActivity.getSsid());
                            txtRecordMap.put("loadbalance", "" + 1/15.0);
                            txtRecordMap.put("bandwidth", "" + 1);
                            Log.d("带宽这里",""+wiFiDirectActivity.getBandwidth(20));
                            txtRecordMap.put("power", "" + wiFiDirectActivity.getPower());
                            Log.d("电量",""+wiFiDirectActivity.getPower());
                            WifiP2pDnsSdServiceInfo wifiP2pDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, serviceType, txtRecordMap);
                            wifiP2pManager.addLocalService(channel, wifiP2pDnsSdServiceInfo, new ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "本地service添加成功！");
                                    /*wifiP2pManager.setDnsSdResponseListeners(channel, wiFiDirectActivity, wiFiDirectActivity);
                                    wifiP2pManager.clearServiceRequests(channel, new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "service request清理成功！");
                                            WifiP2pDnsSdServiceRequest wifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(instanceName, serviceType);
                                            wifiP2pManager.addServiceRequest(channel, wifiP2pDnsSdServiceRequest, new ActionListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "添加service discovery request成功！");
                                                    wifiP2pManager.discoverPeers(channel, new ActionListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Log.d(TAG, "发现peer成功！");
                                                            wifiP2pManager.discoverServices(channel, new ActionListener() {
                                                                @Override
                                                                public void onSuccess() {
                                                                    Log.d(TAG, "发现service成功！");
                                                                }

                                                                @Override
                                                                public void onFailure(int reason) {
                                                                    Log.d(TAG, "发现service失败！" + reason);
                                                                }
                                                            });
                                                        }
                                                        @Override
                                                        public void onFailure(int reason) {
                                                            Log.d(TAG, "发现peer失败！" + reason);
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onFailure(int reason) {
                                                    Log.d(TAG, "添加service discovery request失败！" + reason);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(int reason) {
                                            Log.d(TAG, "service request清理失败！"+ reason);
                                        }
                                    });*/
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.d(TAG, "本地service添加失败！" + reason);

                                }
                            });
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "service清理失败！"+ reason);
                        }
                        });
                    Thread.sleep(1000*10);
                }else {
                    wifiP2pManager.setDnsSdResponseListeners(channel, wiFiDirectActivity, wiFiDirectActivity);
                    wifiP2pManager.clearServiceRequests(channel, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "service request清理成功！");
                            WifiP2pDnsSdServiceRequest wifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(instanceName, serviceType);
                            wifiP2pManager.addServiceRequest(channel, wifiP2pDnsSdServiceRequest, new ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "添加service discovery request成功！");
                                    wifiP2pManager.discoverPeers(channel, new ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "发现peer成功！");
                                            wifiP2pManager.discoverServices(channel, new ActionListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "发现service成功！");
                                                }

                                                @Override
                                                public void onFailure(int reason) {
                                                    Log.d(TAG, "发现service失败！" + reason);
                                                }
                                            });
                                        }
                                        @Override
                                        public void onFailure(int reason) {
                                            Log.d(TAG, "发现peer失败！" + reason);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.d(TAG, "添加service discovery request失败！" + reason);
                                }
                            });
                        }
                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG, "service request清理失败！"+ reason);
                        }
                    });
                    Thread.sleep(period);
                }

               /* wifiP2pManager.removeLocalService(channel, wifiP2pDnsSdServiceInfo, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "本地service删除成功！");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "本地service删除失败！" + reason);

                    }
                });

                //wifiP2pManager.setDnsSdResponseListeners(channel, this, this);
                WifiP2pDnsSdServiceRequest wifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(instanceName, serviceType);
                wifiP2pManager.removeServiceRequest(channel, wifiP2pDnsSdServiceRequest, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "删除service discovery request成功！");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "删除service discovery request失败！" + reason);
                    }
                });
                wifiP2pManager.addServiceRequest(channel, wifiP2pDnsSdServiceRequest, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "添加service discovery request成功！");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "添加service discovery request失败！" + reason);
                    }
                });
                wifiP2pManager.discoverServices(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "发现service成功！");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "发现service失败！" + reason);
                    }
                });*/


            } catch (InterruptedException e) {
                Log.d(TAG, "UpdateServiceThread终止！" );
                break;
            }
        }
    }
    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        Log.d("DnsSdServiceAvailable", instanceName + " " + registrationType + " " + srcDevice.deviceName);
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
        Log.d("DnsSdTxtRecordAvailable", fullDomainName + " " + txtRecordMap.get("power") + " " + srcDevice.deviceName);
    }
}
