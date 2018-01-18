package com.wzj.communication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * Created by wzj on 2017/3/22.
 */

public class MulticastThread implements Runnable {
    private static final String MULTICAST_IP = "230.0.0.1";
    public static final int MULTICSAT_PORT = 30000;
    private static final int DATA_LEN = 4096;
    private MulticastSocket multicastSocket = null;
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
            multicastSocket = new MulticastSocket(MULTICSAT_PORT);
            multicastAddress = InetAddress.getByName(MULTICAST_IP);

            //multicastSocket.setLoopbackMode(false);
            multicastSocket.joinGroup(multicastAddress);
            //System.out.println("看这里 "+multicastSocket.getInterface().getHostName());
            if(type == 0){
                NetworkInterface networkInterface = NetworkInterface.getByName("p2p-wlan0-7");
                multicastSocket.setNetworkInterface(networkInterface);
                System.out.println("多播写启动！！！！！！！！！！！！！");
                buf = "1231".getBytes();
                outPacket = new DatagramPacket(buf, buf.length, multicastAddress, MULTICSAT_PORT);
                multicastSocket.send(outPacket);
            }else if(type == 1){
                while (true){
                    System.out.println("多播读启动！！！！！！！！！！！！！");
                    NetworkInterface networkInterface = NetworkInterface.getByName("p2p-wlan0-7");
                    multicastSocket.setNetworkInterface(networkInterface);
                    multicastSocket.receive(inPacket);
                    System.out.println("收到的信息为："+ new String(buf, 0, inPacket.getLength()));
                    System.out.println("地址："+ inPacket.getAddress().getHostAddress());
                    Message msg = new Message();
                    msg.what = 4;
                    Bundle bundle = new Bundle();
                    bundle.putString("data", new String(buf, 0, inPacket.getLength()));
                    bundle.putString("address", inPacket.getAddress().getHostAddress());
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
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
            e.printStackTrace();
        } finally {
            multicastSocket.close();
        }
    }
}
