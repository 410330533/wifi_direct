package com.wzj.communication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.wzj.bean.Member;
import com.wzj.wifi_direct.WiFiDirectActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Created by wzj on 2017/4/21.
 */

public class UDPBroadcast implements Runnable {
    private final static String BD_ADDRESS = "192.168.49.255";
    private final static int PORT = 30001;
    private final static int DATA_LENGTH = 1024;
    private byte[] buf = new byte[DATA_LENGTH];
    private static DatagramSocket datagramSocket;
    private DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
    private DatagramPacket outPacket = null;
    private int type = 0;
    private Map<String, Map<String, Member>> memberMap;
    private Handler mHandler;

    public void setType(int type) {
        this.type = type;
    }


    public void setMemberMap(Map<String, Map<String, Member>> memberMap) {
        this.memberMap = memberMap;
    }


    public UDPBroadcast(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public UDPBroadcast(Map<String, Map<String, Member>> memberMap) {
        this.memberMap = memberMap;
    }

    @Override
    public void run() {
        try {
            if (null == datagramSocket || datagramSocket.isClosed()){
                datagramSocket = new DatagramSocket(PORT);
                datagramSocket.setReuseAddress(true);
                //datagramSocket.bind(new InetSocketAddress(InetAddress.getByName(BD_ADDRESS),PORT));
                System.out.println("UDP地址："+datagramSocket.getLocalSocketAddress());
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (type == 0){
            //发广播包
            System.out.println("发送广播包！！！！！！");
            try {
                Gson gson = new Gson();
                String str = gson.toJson(memberMap);
                System.out.println("JSON字符串: "+ str.trim());
                buf = str.getBytes();
                outPacket = new DatagramPacket(buf, str.length(), InetAddress.getByName(BD_ADDRESS), PORT);
                datagramSocket.send(outPacket);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (type == 1) {
            //收广播包
            try {
                while (true){
                    System.out.println("接收广播包！！！！！！");
                    datagramSocket.receive(inPacket);
                    String str = new String(inPacket.getData(), 0, inPacket.getLength());
                    System.out.println(inPacket.getAddress().getHostAddress()+"收到广播包："+ str);
                    Message msg = new Message();
                    msg.what = 5;
                    Bundle bundle = new Bundle();
                    bundle.putString("memberMap", str);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }

            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "UDP 98");
                e.printStackTrace();
            } finally {
                this.close();
            }
        }
    }
    public static void close(){
        if(datagramSocket != null && !datagramSocket.isClosed()){
            System.out.println("DatagramSocket关闭！！！！！！");
            datagramSocket.close();
        }
    }
}
