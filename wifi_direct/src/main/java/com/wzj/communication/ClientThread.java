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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import static com.wzj.wifi_direct.BatteryReceiver.power;

/**
 * Created by wzj on 2017/3/3.
 */

public class ClientThread implements Runnable {
    private static final int SOCKET_TIMEOUT = 1000 * 60 * 60;
    private Context context;
    private Handler mHandler;
    private Uri uri;
    private String address;
    //private String macAddress = "";
    private int port;
    private Socket socket;
    private WifiP2pDevice myDevice;
    private Map<String, Socket> tcpConnections;
    private String type = "read";
    private String message;
    public static Timer timer;
    private long broadcastPeriod = 1000*30;
    private Map<String, Member> memberMap;
    public ClientThread(Context context, Handler mHandler, String address, int port, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections) {
        this.context = context;
        this.mHandler = mHandler;
        this.address = address;
        this.port = port;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
    }

    public ClientThread(Context context, Handler mHandler, String address, int port, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections, Map<String, Member> memberMap) {
        this.context = context;
        this.mHandler = mHandler;
        this.address = address;
        this.port = port;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
        this.memberMap = memberMap;
    }


    public ClientThread(Context context, Handler mHandler, WifiP2pDevice myDevice) {
        this.context = context;
        this.mHandler = mHandler;
        this.myDevice = myDevice;
    }
    public ClientThread(Socket socket, String type, String message) {
        this.socket = socket;
        this.type = type;
        this.message = message;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        if (socket == null) {
            socket = new Socket();
            /*try {
                socket = new Socket();
                String bindAddress = "";
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                String regrex = "^wlan0";
                Pattern pattern = Pattern.compile(regrex);
                while(networkInterfaces.hasMoreElements()){
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    Matcher matcher = pattern.matcher(networkInterface.getName());
                    if(matcher.find()){
                        bindAddress = networkInterface.getInetAddresses().nextElement().getHostAddress();
                        Log.d("bindAddress", "匹配/"+bindAddress);
                    }
                }
                socket.bind(new InetSocketAddress(bindAddress, 9002));
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }
        try {
            Log.d(WiFiDirectActivity.TAG, "ClientThread：打开client socket");
            if (!socket.isConnected()) {
                Log.d(WiFiDirectActivity.TAG, "ClientThread：client socket未连接");
                //this.address = new InetSocketAddress(goAddress, goPort);
                //socket.connect(this.address, SOCKET_TIMEOUT);
                //修改！！！！1
                //socket.connect((new InetSocketAddress(goAddress, goPort)), SOCKET_TIMEOUT);
                //socket.bind(new InetSocketAddress("192.168.49.207",8763));
                //客户登录初始化操作
                socket.connect((new InetSocketAddress(address, port)));
                String macAddress = "";
                String deviceName = "";
                //将本机信息发送给GO
                if(socket.getInetAddress().getHostAddress().equals(DeviceDetailFragment.GO_ADDRESS)){
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.write(myDevice.deviceName);
                    writer.newLine();
                    writer.write(myDevice.deviceAddress);
                    writer.newLine();
                    writer.write(String.valueOf(BatteryReceiver.power));
                    writer.newLine();
                    writer.flush();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String ipAddress = bufferedReader.readLine();
                    macAddress = bufferedReader.readLine();
                    deviceName = bufferedReader.readLine();
                    //memMapStr长度问题，超过一行则不能接收
                    String memMapStr = bufferedReader.readLine();
                    /*String tempStr = null;
                    while ((tempStr = bufferedReader.readLine()) != null){
                        memMapStr += tempStr;
                    }*/
                    Log.d("ClientThread:", "TCP收到字符串/"+memMapStr);
                    Gson gson = new Gson();
                    Map<String, Member> memberMapTemp = gson.fromJson(memMapStr.trim(), new TypeToken<Map<String, Member>>(){}.getType());
                    for(Entry<String, Member> entry : memberMapTemp.entrySet()){
                        memberMap.put(entry.getKey(), entry.getValue());
                    }
                    Message message = new Message();
                    message.what = 6;
                    mHandler.sendMessage(message);
                    //启动广播包接收线程
                    UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_READ, mHandler);
                    udpBroadcast.setIpAddress(ipAddress);
                    new Thread(udpBroadcast).start();
                    //周期性广播本机信息
                    final Member member = new Member(ipAddress, myDevice.deviceName, myDevice.deviceAddress, power);
                    if(timer == null){
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                member.setPower(power);
                                UDPBroadcast udpBroadcastWrite = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMBER, member);
                                new Thread(udpBroadcastWrite).start();
                            }
                        }, broadcastPeriod, broadcastPeriod);
                    }
                    if(tcpConnections.containsKey(socket.getInetAddress().getHostAddress())){
                        tcpConnections.put("192.168.49.0", socket);
                        memberMap.put(macAddress, new Member("192.168.49.0", deviceName, macAddress));
                        Message msg = new Message();
                        msg.what = 6;
                        mHandler.sendMessage(msg);
                    }else{
                        tcpConnections.put(socket.getInetAddress().getHostAddress(), socket);
                        memberMap.put(macAddress, new Member(socket.getInetAddress().getHostAddress(), deviceName, macAddress));
                        Message msg = new Message();
                        msg.what = 6;
                        mHandler.sendMessage(msg);
                    }
                }else {
                    //更新tcpConnections；组员间通信
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.write(myDevice.deviceName);
                    writer.newLine();
                    writer.write(myDevice.deviceAddress);
                    writer.newLine();
                    writer.flush();
                    tcpConnections.put(socket.getInetAddress().getHostAddress(), socket);
                }
            }
            Log.d(WiFiDirectActivity.TAG, "ClientThread：client socket已连接" + socket.isConnected());
            if(type.equals("read")){
                new Thread(new ClientRead()).start();
            }else if(type.equals("write")){
                new Thread(new ClientWrite()).start();

            }else if(type.equals("rw")){
                new Thread(new ClientWrite()).start();
                new Thread(new ClientRead()).start();
            }else if(type.equals("message")){
                new Thread(new ClientWriteMessage()).start();
            }


        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (!socket.isConnected()) {
                try {
                    socket.close();
                    socket = null;
                    Log.d(WiFiDirectActivity.TAG, "ClientThread：关闭client socket");
                } catch (IOException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
    //写文本消息
    private class ClientWriteMessage implements Runnable {

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入message开始 ");
                long flag = StringToLong.transfer("Messagem");
                stream.writeLong(flag);
                stream.writeUTF(message);
                stream.flush();
                //stream.close();
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入message完毕");
            } catch (FileNotFoundException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                e.printStackTrace();

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    //写图片
    private class ClientWrite implements Runnable {

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = context.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(context, uri));
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入开始 " + GetPath.getPath(context, uri));
                stream.writeLong(file.length());
                byte buf[] = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    //将buf中从0到length个字节写到输出流
                    stream.write(buf, 0, length);
                }
                in.close();
                stream.flush();
                //stream.close();
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入完毕");
                Message msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);
            } catch (FileNotFoundException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                e.printStackTrace();

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                if(socket != null ){
                    if(tcpConnections.containsKey(socket.getInetAddress().getHostAddress())){
                        tcpConnections.remove(socket.getInetAddress().getHostAddress());
                    }
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                System.out.println(e.getMessage());
                e.printStackTrace();

                if(e.getMessage().equals("sendto failed: EPIPE (Broken pipe)")){
                    try {
                        socket.close();
                        socket = null;
                        Log.d(WiFiDirectActivity.TAG, "ClientThread：关闭client socket");
                        Message msg = new Message();
                        msg.what = 3;
                        mHandler.sendMessage(msg);
                    } catch (IOException e1) {
                        Log.d(WiFiDirectActivity.TAG, e1.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    //读取消息，对文本与图片类型消息进行判断，并采取不同的处理方式
    private class ClientRead implements Runnable {
        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    Log.d(WiFiDirectActivity.TAG, "ClientRead: 已连接到服务端，开始读操作，阻塞ing "+socket.getInetAddress().getHostAddress());
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    //无数据，阻塞
                    long flag = inputStream.readLong();
                    if(flag == StringToLong.transfer("Messagem")){
                        //文本消息
                        String message = "";
                        message = inputStream.readUTF();
                        System.out.println("----Gson: "+ message);
                        Message msg = new Message();
                        msg.what = 7;
                        Bundle bundle = new Bundle();
                        bundle.putString("message", message);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);

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
                        byte buf[] = new byte[1024];
                        int len;
                        int fileLength = 0;
                        //此处开始创建文件
                        FileOutputStream outputStream = new FileOutputStream(file);
                        while (fileLength < totalLength) {
                            len = inputStream.read(buf);
                            outputStream.write(buf, 0, len);
                            fileLength += len;
                        }
                        System.out.println("ClientRead: 读取完毕。。。。"+ fileLength);
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("file", file.getAbsolutePath());
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "ClientRead 244");
                e.printStackTrace();
            } finally {
                try {
                    if(socket != null && !socket.isClosed()){
                        socket.close();
                        System.out.println("socket关闭");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }


}



