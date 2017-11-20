package com.wzj.communication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wzj.wifi_direct.WiFiDirectActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by wzj on 2017/3/22.
 */

public class MulticastThread implements Runnable {
    private static final String MULTICAST_IP = "239.0.0.1";
    public static final int MULTICAST_PORT = 30000;
    private static final int DATA_LEN = 4096;
    private static MulticastSocket multicastSocket = null;
    private InetAddress multicastAddress = null;
    private byte[] buf = new byte[DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
    private DatagramPacket outPacket = null;
    private int type = 0;
    private Handler mHandler;

    public void setType(int type) {
        this.type = type;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void run() {
        try {
            multicastAddress = InetAddress.getByName(MULTICAST_IP);
            if(multicastSocket == null ){
                multicastSocket = new MulticastSocket(MULTICAST_PORT);
                multicastSocket.setLoopbackMode(true);
                multicastSocket.joinGroup(multicastAddress);
                //System.out.println("看这里 "+multicastSocket.getInterface().getHostName());
            }
            if(type == 1){
                NetworkInterface networkInterface = null;
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while(networkInterfaces.hasMoreElements()){
                    NetworkInterface nInterface = networkInterfaces.nextElement();
                    String name = nInterface.getName();
                    if(name.startsWith("wlan0")){
                        networkInterface = nInterface;
                        System.out.println(networkInterface.getInetAddresses().nextElement().getHostName());
                        break;
                    }
                }
                multicastSocket.setNetworkInterface(networkInterface);
                System.out.println("多播写启动！！！！！！！！！！！！！");
                buf = "1231".getBytes();
                outPacket = new DatagramPacket(buf, buf.length, multicastAddress, MULTICAST_PORT);
                multicastSocket.send(outPacket);
            }else if(type == 0){
                NetworkInterface networkInterface = null;
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while(networkInterfaces.hasMoreElements()){
                    NetworkInterface nInterface = networkInterfaces.nextElement();
                    String name = nInterface.getName();
                    if(name.startsWith("wlan0")){
                        networkInterface = nInterface;
                        System.out.println(networkInterface.getInetAddresses().nextElement().getHostName());
                        break;
                    }
                }
                multicastSocket.setNetworkInterface(networkInterface);
                multicastSocket.setTimeToLive(2);
                System.out.println(multicastSocket.getNetworkInterface()+" "+multicastSocket.getTimeToLive());
                while (true){
                    System.out.println("多播读启动！！！！！！！！！！！！！");
                    multicastSocket.receive(inPacket);
                    System.out.println("收到的信息为："+ new String(buf, 0, inPacket.getLength()));
                    System.out.println("地址："+ inPacket.getAddress().getHostAddress());
                    Message msg = new Message();
                    msg.what = 4;
                    Bundle bundle = new Bundle();
                    bundle.putString("data", new String(buf, 0, inPacket.getLength()));
                    bundle.putString("address", inPacket.getAddress().getHostAddress());
                    msg.setData(bundle);
                    if(mHandler!=null){
                        mHandler.sendMessage(msg);
                    }

                }
            }

        } catch (IOException e) {
            if (multicastSocket != null){
                try {
                    multicastSocket.leaveGroup(multicastAddress);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            Log.e(WiFiDirectActivity.TAG, "MulticastThread 84");
            e.printStackTrace();
        }
    }

    static public void close(){
        if(multicastSocket != null && !multicastSocket.isClosed()){
            try {
                multicastSocket.leaveGroup(InetAddress.getByName(MULTICAST_IP));
            } catch (IOException e) {
                e.printStackTrace();
            }
            multicastSocket.close();
            multicastSocket = null;
            System.out.println("multicastSocket关闭！！！！！！！");
        }
    }
}
