package com.wzj.communication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wzj on 2017/3/22.
 */

public class MulticastThread implements Runnable {
    private static final String MULTICAST_IP = "239.0.0.1";
    public static final int MULTICSAT_PORT = 30000;
    private static final int DATA_LEN = 4096;
    private MulticastSocket multicastSocket = null;
    private static InetAddress multicastAddress = null;
    private byte[] buf = new byte[DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
    private DatagramPacket outPacket = null;
    private int type = 0;
    private Handler mHandler;
    private NetworkInterface networkInterface = null;
    private String readInterfaceRegrex = "^p2p";
    private String writeInterfaceRegrex = "^wlan";
    public void setType(int type) {
        this.type = type;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void run() {

        //System.out.println("看这里 "+multicastSocket.getInterface().getHostName());
        if(type == 0){
            try {
                if(multicastSocket == null || multicastSocket.isClosed()){
                    multicastAddress = InetAddress.getByName(MULTICAST_IP);
                    multicastSocket = new MulticastSocket();
                    //multicastSocket.setLoopbackMode(true);
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    String regrex = writeInterfaceRegrex;
                    Pattern pattern = Pattern.compile(regrex);
                    while(networkInterfaces.hasMoreElements()){
                        NetworkInterface networkInterface1 = networkInterfaces.nextElement();
                        Matcher matcher = pattern.matcher(networkInterface1.getName());
                        if(matcher.find()){
                            networkInterface = networkInterface1;
                            Log.d("NetworkInterface", "匹配/"+networkInterface.getName()+"/"+ networkInterface.getInetAddresses().nextElement().getHostAddress());
                            break;
                        }
                    }
                    if(networkInterface != null){
                        //Specify the network interface for outgoing multicast datagrams sent on this socket.
                        multicastSocket.setNetworkInterface(networkInterface);
                        //multicastSocket.joinGroup(new InetSocketAddress(multicastAddress, MULTICSAT_PORT), networkInterface);
                        Log.d("多播写", "初始化完成！");
                    }
                }
                System.out.println("看这里 "+multicastSocket.getInterface().getHostName());
                buf = "1231".getBytes();
                outPacket = new DatagramPacket(buf, buf.length, multicastAddress, MULTICSAT_PORT);
                multicastSocket.send(outPacket);
                this.writeClose();
            } catch (SocketException e) {
                e.printStackTrace();
                this.writeClose();
            } catch (IOException e) {
                e.printStackTrace();
                this.writeClose();
            }

        }else if(type == 1){
            try {
                if (multicastSocket == null || multicastSocket.isClosed()) {
                    multicastAddress = InetAddress.getByName(MULTICAST_IP);
                    multicastSocket = new MulticastSocket(MULTICSAT_PORT);
                    multicastSocket.setLoopbackMode(true);
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    String regrex = readInterfaceRegrex;
                    Pattern pattern = Pattern.compile(regrex);
                    while (networkInterfaces.hasMoreElements()) {
                        NetworkInterface networkInterface1 = networkInterfaces.nextElement();
                        Matcher matcher = pattern.matcher(networkInterface1.getName());
                        if (matcher.find()) {
                            networkInterface = networkInterface1;
                            Log.d("NetworkInterface", "匹配/" + networkInterface.getName() +"/"+ networkInterface.getInetAddresses().nextElement().getHostAddress());
                            break;
                        }
                    }
                    if(networkInterface != null){
                        multicastSocket.joinGroup(new InetSocketAddress(multicastAddress, MULTICSAT_PORT), networkInterface);
                        //multicastSocket.setNetworkInterface(networkInterface);
                        Log.d("多播读", "初始化完成！");
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
                this.readClose();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                this.readClose();
            } catch (IOException e) {
                e.printStackTrace();
                this.readClose();
            }
            while (true){
                try {
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
                } catch (IOException e) {
                    e.printStackTrace();
                    this.readClose();
                    break;
                }
            }
        }
    }
    public void readClose(){
        if(multicastSocket != null && !multicastSocket.isClosed()) {

                //multicastSocket.leaveGroup(InetAddress.getByName(MULTICAST_IP));
                multicastSocket.close();
                multicastSocket = null;



        }
        System.out.println("MulticastSocket读关闭！！！！！！");
    }
    public void writeClose(){
        if(multicastSocket != null && !multicastSocket.isClosed()) {
                multicastSocket.close();
                multicastSocket = null;

        }
        System.out.println("MulticastSocket写关闭！！！！！！");
    }
}
