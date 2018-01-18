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
    public static final String ADD_MEMMAP = "0";
    public static final String ADD_MEMBER = "1";
    public static final String DELETE_MEMBER = "2";
    public static final int BROADCAST_READ = 1;
    public static final int BROADCAST_WRITE = 0;
    private final static String BD_ADDRESS = "192.168.49.255";
    private final static int PORT = 30001;
    private final static int DATA_LENGTH = 1024;
    private byte[] buf = new byte[DATA_LENGTH];
    private static DatagramSocket datagramSocket;
    private DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
    private DatagramPacket outPacket = null;
    private int type = BROADCAST_WRITE;
    private Map<String, Member> memberMap;
    private Handler mHandler;
    private String messageType = ADD_MEMMAP;
    private Member member;
    private String ipAddress;

    public void setType(int type) {
        this.type = type;
    }


    public void setMemberMap(Map<String, Member> memberMap) {
        this.memberMap = memberMap;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public UDPBroadcast(int type, Handler mHandler) {
        this.type = type;
        this.mHandler = mHandler;
    }

    public UDPBroadcast(Map<String, Member> memberMap) {
        this.memberMap = memberMap;
    }

    public UDPBroadcast(int type, String messageType, Member member) {
        this.type = type;
        this.messageType = messageType;
        this.member = member;
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
        if (type == BROADCAST_WRITE){
            //发广播包
            System.out.println("发送广播包！！！！！！");
            if(messageType.equals(ADD_MEMMAP)){
                try {
                    Gson gson = new Gson();
                    String str = messageType + "/" +gson.toJson(memberMap);
                    System.out.println("JSON字符串: "+ str.trim());
                    buf = str.getBytes();
                    outPacket = new DatagramPacket(buf, str.length(), InetAddress.getByName(BD_ADDRESS), PORT);
                    datagramSocket.send(outPacket);

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(messageType.equals(ADD_MEMBER)){
                try {
                    Gson gson = new Gson();
                    String str = messageType + "/" +gson.toJson(member);
                    System.out.println("JSON字符串: "+ str.trim());
                    buf = str.getBytes();
                    outPacket = new DatagramPacket(buf, str.length(), InetAddress.getByName(BD_ADDRESS), PORT);
                    datagramSocket.send(outPacket);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(messageType.equals(DELETE_MEMBER)){
                try {
                    Gson gson = new Gson();
                    String str = messageType + "/" +gson.toJson(member);
                    System.out.println("JSON字符串: "+ str.trim());
                    buf = str.getBytes();
                    outPacket = new DatagramPacket(buf, str.length(), InetAddress.getByName(BD_ADDRESS), PORT);
                    datagramSocket.send(outPacket);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }else if (type == BROADCAST_READ) {
            //收广播包
            try {
                while (true){
                    System.out.println("接收广播包！！！！！！");
                    datagramSocket.receive(inPacket);
                    if(!inPacket.getAddress().getHostAddress().equals(ipAddress)){
                        String str = new String(inPacket.getData(), 0, inPacket.getLength());
                        System.out.println(inPacket.getAddress().getHostAddress()+"收到广播包："+ str);
                        String mStr[] = str.split("/");
                        if(mStr[0].equals(ADD_MEMMAP)){
                            Message msg = new Message();
                            msg.what = 5;
                            Bundle bundle = new Bundle();
                            bundle.putString("memberMap", mStr[1]);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);

                        }else if(mStr[0].equals(ADD_MEMBER)){
                            Message msg = new Message();
                            msg.what = 8;
                            Bundle bundle = new Bundle();
                            bundle.putString("member", mStr[1]);
                            bundle.putString("sourceIp", inPacket.getAddress().getHostAddress());
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }else if(mStr[0].equals(DELETE_MEMBER)){
                            Message msg = new Message();
                            msg.what = 9;
                            Bundle bundle = new Bundle();
                            bundle.putString("member", mStr[1]);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    }
                }

            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "UDP 98");
                if(ClientThread.timer != null){
                    ClientThread.timer.cancel();
                    ClientThread.timer = null;
                }
                if(ServerThread.timer != null){
                    ServerThread.timer.cancel();
                    ServerThread.timer = null;
                }

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
