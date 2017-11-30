package com.wzj.communication;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wzj.bean.Member;
import com.wzj.util.GetPath;
import com.wzj.util.StringToLong;
import com.wzj.wifi_direct.BatteryReceiver;
import com.wzj.wifi_direct.DeviceDetailFragment;
import com.wzj.wifi_direct.WiFiDirectActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wzj on 2017/3/3.
 */
//ServerWrite目前并未使用，写操作都通过ClientWrite实现
public class ServerThread implements Runnable {
    private Context context;
    private static ServerSocket serverSocket;
    private int count = 1;
    private Handler mHandler;
    private String type;
    private Socket socket;
    private Uri uri;
    private Map<String, Socket> tcpConnections;
    private static Map<String, Map<String, Member>> memberMap;
    private WifiP2pDevice myDevice;
    public static Timer timer;
    private long broadcastPeriod = 1000*60;

    public ServerThread(Context context, Map<String, Map<String, Member>> memberMap, Handler mHandler, String type, Map<String, Socket> tcpConnections, WifiP2pDevice myDevice) {
        this.context = context;
        this.memberMap = memberMap;
        this.mHandler = mHandler;
        this.type = type;
        this.tcpConnections = tcpConnections;
        this.myDevice = myDevice;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setFile(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void run() {
        try {
            if(serverSocket == null){
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(DeviceDetailFragment.GO_ADDRESS, DeviceDetailFragment.PORT));
                //服务器端开启广播读
                UDPBroadcast udpBroadcast = new UDPBroadcast(mHandler);
                udpBroadcast.setType(1);
                udpBroadcast.setIpAddress(DeviceDetailFragment.GO_ADDRESS);
                new Thread(udpBroadcast).start();
                //周期性广播本机信息
                final Member member = new Member(DeviceDetailFragment.GO_ADDRESS, "(GO)"+myDevice.deviceName, myDevice.deviceAddress, BatteryReceiver.power);
                if(timer == null){
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            member.setPower(BatteryReceiver.power);
                            UDPBroadcast udpBroadcastWrite = new UDPBroadcast(0, "1", member);
                            new Thread(udpBroadcastWrite).start();
                        }
                    }, broadcastPeriod, broadcastPeriod);
                }
            }
            Log.d(WiFiDirectActivity.TAG, "ServerThread：线程启动");
            if(type.equals("read")){
                while (true) {
                    System.out.println("ServerThread:执行次数 "+ count++);
                    Socket client = serverSocket.accept();
                    System.out.println("连接到新客户端！！！"+client.getInetAddress().getHostAddress());
                    //this.socket = client;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String deviceName = bufferedReader.readLine();
                    String macAddress = bufferedReader.readLine();
                    float power = Float.valueOf(bufferedReader.readLine());
                    System.out.println("看这里power："+ power);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                    writer.write(client.getInetAddress().getHostAddress());
                    writer.newLine();
                    writer.flush();
                    //更新tcpConnections
                    tcpConnections.put(client.getInetAddress().getHostAddress(), client);
                    //更新memberMap
                    Member member = new Member(client.getInetAddress().getHostAddress(), deviceName, macAddress, power);
                    //加入GO的信息
                    Member groupOwner = new Member(DeviceDetailFragment.GO_ADDRESS, "(GO)"+myDevice.deviceName, myDevice.deviceAddress, BatteryReceiver.power);
                    System.out.println("看这里power："+ member.getPower());
                    Map<String, Member> tempMap = new HashMap<>();
                    if(!memberMap.containsKey(myDevice.deviceAddress)){
                        Map<String, Member> tempMapG = new HashMap<>();
                        tempMapG.put(DeviceDetailFragment.GO_ADDRESS, groupOwner);
                        memberMap.put(myDevice.deviceAddress, tempMapG);
                    }
                    tempMap.put(client.getInetAddress().getHostAddress(), member);
                    memberMap.put(macAddress, tempMap);
                    Message msg = new Message();
                    msg.what = 6;
                    mHandler.sendMessage(msg);
                    UDPBroadcast udpBroadcast = new UDPBroadcast(memberMap);
                    new Thread(udpBroadcast).start();
                    System.out.println("ServerThread: "+ memberMap.size()+" " +memberMap.get(macAddress));
                    new Thread(new ServerRead(client)).start();
                    //再发一遍广播
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            UDPBroadcast udpBroadcast = new UDPBroadcast(memberMap);
                            new Thread(udpBroadcast).start();
                        }
                    }, 5000);

                }
            }else if(type.equals("write")) {
                new Thread(new ServerWrite(socket)).start();
            }
        } catch (IOException e) {
            Log.e(WiFiDirectActivity.TAG, "ServerThread 118");
            e.printStackTrace();
        }
    }
    public static void close(){
        if(serverSocket != null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
                serverSocket = null;
                System.out.println("ServerSocket关闭！！！！！！");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "ServerThread 130");
                e.printStackTrace();
            }
        }
    }
    class ServerRead implements Runnable{
        private Socket socket;

        public ServerRead(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                while (socket.isConnected()){
                    Log.d(WiFiDirectActivity.TAG, "ServerRead: 连接到client " + socket.toString());
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    //无数据，阻塞
                    System.out.println("阻塞前！！！！！！！！");
                    long flag = inputStream.readLong();
                    System.out.println("阻塞后！！！！！！！！");
                    if(flag == StringToLong.transfer("Messagem")){
                        //文本消息
                        String message = "";
                        message = inputStream.readUTF();
                        //message = new String(message.getBytes(), "utf-8");
                        System.out.println("----Gson: "+ message);
                        Message msg = new Message();
                        msg.what = 7;
                        Bundle bundle = new Bundle();
                        bundle.putString("message", message);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        synchronized (WiFiDirectActivity.dataSize){
                            WiFiDirectActivity.dataSize += 8;
                            WiFiDirectActivity.dataSize += message.getBytes().length;
                        }

                    }else{
                        long totalLength = flag;
                        File file = new File(Environment.getExternalStorageDirectory() + "/"
                                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                                + ".jpg");
                        File dirs = new File(file.getParent());
                        if (!dirs.exists()) {
                            dirs.mkdirs();
                        }

                        //读开始
                        byte buf[] = new byte[1024*1024*10];
                        int len = 0;
                        int fileLength = 0;
                        FileOutputStream outputStream = new FileOutputStream(file);
                        Log.d(WiFiDirectActivity.TAG, "ServerRead: -" + count++ + "- AsyncTask处理client请求 " + file.toString());
                        Log.d(WiFiDirectActivity.TAG, "ServerRead:处理client请求" + file.toString());
                        while (fileLength < totalLength) {
                            len = inputStream.read(buf);
                            outputStream.write(buf, 0, len);
                            fileLength += len;
                            synchronized (WiFiDirectActivity.dataSize) {
                                WiFiDirectActivity.dataSize += len;
                            }
                        }

                        System.out.println("ServerRead: 读取完毕。。。。");
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("file", file.getAbsolutePath());
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);

                    }

                }

            } catch (IOException e){
                Log.e(WiFiDirectActivity.TAG, "ServerRead 223");
                e.printStackTrace();
            } finally {
                try {
                    if(socket != null && !socket.isClosed()){
                        socket.close();
                        System.out.println("socket 关闭");
                    }
                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, "ServerThread 233");
                    e.printStackTrace();
                }
            }
        }

    }

    private class ServerWrite implements Runnable{
        private Socket socket;

        public ServerWrite(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = context.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(context, uri));
                stream.writeLong(file.length());
                System.out.println("ServerWrite:服务端写入开始 "+socket.getInetAddress().getHostAddress() + file.length());
                byte buf[] = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    //将buf中从0到length个字节写到输出流
                    stream.write(buf, 0, length);
                }
                System.out.println(GetPath.getPath(context, uri));
                in.close();
                stream.flush();
                //stream.close();
                Log.d(WiFiDirectActivity.TAG, "ServerWrite：写入完毕");
                Message msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);
            } catch (FileNotFoundException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                e.printStackTrace();
            }
        }
    }

}

