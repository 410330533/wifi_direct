package com.wzj.communication;

import android.content.ContentResolver;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.wzj.bean.Member;
import com.wzj.util.AddressObtain;
import com.wzj.util.GetPath;
import com.wzj.util.StringToLong;
import com.wzj.wifi_direct.BatteryReceiver;
import com.wzj.wifi_direct.DeviceDetailFragment;
import com.wzj.wifi_direct.R;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import static com.wzj.communication.MulticastThread.GO_MULTICAST_MEMMAP;
import static com.wzj.communication.MulticastThread.MULTICAST_WRITE;
import static com.wzj.communication.MulticastThread.P2P_INTERFACE_REGREX;
import static com.wzj.communication.MulticastThread.TAG;
import static com.wzj.communication.MulticastThread.WLAN_INTERFACE_REGREX;

/**
 * Created by wzj on 2017/3/3.
 */
//ServerWrite目前并未使用，写操作都通过ClientWrite实现
public class ServerThread implements Runnable {
    private WiFiDirectActivity wiFiDirectActivity;
    private static ServerSocket serverSocket;
    private int count = 1;
    private Handler mHandler;
    private String type;
    private Socket socket;
    private Uri uri;
    private Map<String, Socket> tcpConnections;
    private static Map<String, Member> memberMap;
    private WifiP2pDevice myDevice;
    private static Timer broadcastTimer;
    private static Timer broadcastMemTimer;
    private static Timer multicastTimer;

    private long broadcastPeriod = 1000*60;

    public ServerThread(WiFiDirectActivity wiFiDirectActivity, Map<String, Member> memberMap, Handler mHandler, String type, Map<String, Socket> tcpConnections, WifiP2pDevice myDevice) {
        this.wiFiDirectActivity = wiFiDirectActivity;
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

    public void memberMapPut(String key, Member value){
        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
        if(deviceDetailFragment.getCurrentGroupMemberMap().containsKey(key)){
            memberMap.put(key, value);
            deviceDetailFragment.getCurrentGroupMemberMap().put(key, value);
        }else {
            memberMap.put(key, value);
        }
    }

    @Override
    public void run() {
        try {
            if(serverSocket == null){
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(DeviceDetailFragment.GO_ADDRESS, DeviceDetailFragment.PORT));
                //服务器端开启广播读
                UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_READ, mHandler);
                udpBroadcast.setIpAddress(DeviceDetailFragment.GO_ADDRESS);
                new Thread(udpBroadcast).start();
                DeviceDetailFragment.setUdpBroadcastRead(udpBroadcast);
                //周期性广播本机信息
               /* final Member member = new Member(DeviceDetailFragment.GO_ADDRESS, "(GO)"+myDevice.deviceName, myDevice.deviceAddress, BatteryReceiver.power);
                if(broadcastTimer == null){
                    broadcastTimer = new Timer();
                    broadcastTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                        member.setPower(BatteryReceiver.power);
                        UDPBroadcast udpBroadcastWrite = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMBER, member);
                        new Thread(udpBroadcastWrite).start();
                        }
                    }, broadcastPeriod, broadcastPeriod);
                }*/
                if(broadcastMemTimer == null){
                    broadcastMemTimer = new Timer();
                    broadcastMemTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                            UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMMAP, memberMap);
                            new Thread(udpBroadcast).start();
                            /*UDPBroadcast udpBroadcastCurrentMemMap = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_CURRENT_MEMMAP, deviceDetailFragment.getCurrentGroupMemberMap());
                            new Thread(udpBroadcastCurrentMemMap).start();*/
                        }
                    }, 0, broadcastPeriod);
                }
                if(multicastTimer == null){
                    multicastTimer = new Timer();
                    multicastTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                            if(deviceDetailFragment.getOtherGroupMemberMap().size() == 0 && memberMap.size() != 0){
                                Log.d(TAG, "p2p组播memberMap");
                                MulticastThread multicastThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                                new Thread(multicastThread).start();
                            }else if(deviceDetailFragment.getCurrentGroupMemberMap().size() != 0){
                                Log.d(TAG, "p2p组播currentGroupMemberMap");
                                MulticastThread multicastThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, deviceDetailFragment.getCurrentGroupMemberMap(), myDevice.deviceAddress);
                                new Thread(multicastThread).start();
                            }
                        }
                    }, 0, broadcastPeriod);
                    if(wiFiDirectActivity.getWiFiBroadcastReceiver().isWFDConnected()){
                        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                        deviceDetailFragment.setPeriodMulticastWlanWrite();
                    }
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
                    writer.write(myDevice.deviceAddress);
                    writer.newLine();
                    writer.write("(GO)"+myDevice.deviceName);
                    writer.newLine();
                    writer.flush();
                    //更新tcpConnections
                    tcpConnections.put(client.getInetAddress().getHostAddress(), client);
                    //更新memberMap
                    Member member = new Member(client.getInetAddress().getHostAddress(), deviceName, macAddress, power);
                    //加入GO的信息
                    Member groupOwner = new Member(DeviceDetailFragment.GO_ADDRESS, "(GO)"+myDevice.deviceName, myDevice.deviceAddress, BatteryReceiver.power);
                    System.out.println("看这里power："+ member.getPower());

                    if(!memberMap.containsKey(myDevice.deviceAddress)){
                        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                        memberMap.put(myDevice.deviceAddress, groupOwner);
                        deviceDetailFragment.getCurrentGroupMemberMap().put(myDevice.deviceAddress, groupOwner);
                    }
                    memberMapPut(macAddress, member);
                    Message msg = new Message();
                    msg.what = 6;
                    mHandler.sendMessage(msg);
                    //将当前MemberMap用TCP传输给client
                    Gson gson = new Gson();
                    String memMapStr = gson.toJson(memberMap);
                    writer.write(memMapStr);
                    writer.newLine();
                    writer.flush();
                    //将新增加的成员广播给现有组内成员
                    /*UDPBroadcast udpBroadcast = new UDPBroadcast(memberMap);
                    new Thread(udpBroadcast).start();*/
                    Log.d(TAG, "将新增加的成员广播给现有组内成员，组播给组外GO");
                    UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMMAP, memberMap);
                    new Thread(udpBroadcast).start();

                    //组播
                    DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                    if(deviceDetailFragment.getOtherGroupMemberMap().size() == 0){
                        Log.d(TAG, "p2p/wlan组播memberMap");
                        MulticastThread multicastThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                        new Thread(multicastThread).start();
                        MulticastThread multicastWlanThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, WLAN_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                        new Thread(multicastWlanThread).start();
                    }else {
                        Log.d(TAG, "p2p/wlan组播currentGroupMemberMap");
                        MulticastThread multicastThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, deviceDetailFragment.getCurrentGroupMemberMap(), myDevice.deviceAddress);
                        new Thread(multicastThread).start();
                        MulticastThread multicastP2pThread = new MulticastThread(wiFiDirectActivity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, deviceDetailFragment.getCurrentGroupMemberMap(), myDevice.deviceAddress);
                        new Thread(multicastP2pThread).start();

                    }


                    //再次广播
                    udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMMAP, memberMap);
                    new Thread(udpBroadcast).start();

                    System.out.println("ServerThread: "+ memberMap.size()+" " +memberMap.get(macAddress));
                    new Thread(new ServerRead(client)).start();


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
        if(broadcastTimer != null){
            broadcastTimer.cancel();
            broadcastTimer = null;
        }
        if(broadcastMemTimer != null){
            broadcastMemTimer.cancel();
            broadcastMemTimer = null;
        }
        if(multicastTimer != null){
            multicastTimer.cancel();
            multicastTimer = null;
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

                    }else if(flag == StringToLong.transfer("Relayngo")){//自右向左通信，GO中继
                        Log.d(TAG, "自右向左通信，GO中继");
                        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                        Map<String, Member> memberMap = deviceDetailFragment.getMemberMap();
                        Map<String, Socket> tcpConnections = deviceDetailFragment.getTcpConnections();
                        flag = inputStream.readLong();
                        //由该mac辨别目标设备
                        byte macArray[] = new byte[17];
                        inputStream.read(macArray);
                        String mac = new String(macArray);
                        Member member = memberMap.get(mac);
                        if(flag == StringToLong.transfer("Messagem")){

                        }else {
                            Log.d(TAG, "目标设备不在本组内，继续relay");
                            //组主选择RelayClient
                            //...
                            //...
                            String choiceIp = member.getIpAddress();
                            Socket client = null;
                            if(choiceIp.equals(DeviceDetailFragment.GO_ADDRESS)){
                                for(Entry<String, Member> entry : memberMap.entrySet()){
                                    if(!entry.getValue().getisCurrentGroup() && tcpConnections.containsKey(entry.getValue().getIpAddress())){
                                        Log.d(TAG, "自右向左通信，GO中继，找到relayClient");
                                        client = tcpConnections.get(entry.getValue().getIpAddress());
                                    }
                                }
                                DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                                long type = StringToLong.transfer("Relaynod");
                                long totalLength = flag;
                                dataOutputStream.writeLong(type);
                                dataOutputStream.writeLong(totalLength);
                                dataOutputStream.writeBytes(mac);

                                byte buf[] = new byte[1024];
                                int length;
                                int fileLength = 0;
                                while (fileLength < totalLength) {
                                    length = dataInputStream.read(buf);
                                    dataOutputStream.write(buf, 0, length);
                                    fileLength += length;
                                }
                                dataOutputStream.flush();
                                Log.d(TAG, "组间通信：客户端写入完毕");
                                Message msg = new Message();
                                msg.what = 2;
                                mHandler.sendMessage(msg);
                            }else {
                                //正常写
                                client = tcpConnections.get(choiceIp);
                                if(client != null){
                                    DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                                    long totalLength = flag;
                                    dataOutputStream.writeLong(totalLength);

                                    byte buf[] = new byte[1024];
                                    int length;
                                    int fileLength = 0;
                                    while (fileLength < totalLength) {
                                        length = dataInputStream.read(buf);
                                        dataOutputStream.write(buf, 0, length);
                                        fileLength += length;
                                    }
                                    dataOutputStream.flush();
                                    Log.d(TAG, "组间通信：客户端写入完毕");
                                    Message msg = new Message();
                                    msg.what = 2;
                                    mHandler.sendMessage(msg);
                                }else {
                                    Log.d(TAG, "TCP连接未建立完全");
                                }
                            }

                        }

                    } else if(flag == StringToLong.transfer("Relaynod")){ //组间通信
                        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment)wiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                        Map<String, Member> memberMap = deviceDetailFragment.getMemberMap();
                        Map<String, Socket> tcpConnections = deviceDetailFragment.getTcpConnections();
                        flag = inputStream.readLong();
                        //由该mac辨别目标设备
                        byte macArray[] = new byte[17];
                        inputStream.read(macArray);
                        String mac = new String(macArray);
                        Member member = memberMap.get(mac);
                        if(flag == StringToLong.transfer("Messagem")){

                        }else {
                            if(mac.equals(AddressObtain.getWlanMACAddress()) || mac.equals(AddressObtain.getP2pMACAddress())){
                                Log.d(TAG, "目标设备为当前设备");
                                long totalLength = flag;
                                File file = new File(Environment.getExternalStorageDirectory() + "/"
                                        + wiFiDirectActivity.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
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
                                outputStream.flush();
                                outputStream.close();
                                Log.d(TAG, "组间通信ClientRead: 读取完毕。。。。"+ fileLength);
                                Message msg = new Message();
                                msg.what = 1;
                                Bundle bundle = new Bundle();
                                bundle.putString("file", file.getAbsolutePath());
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }else if(member.getisCurrentGroup()){
                                Log.d(TAG, "目标设备在本组内");
                                String choiceIp = member.getIpAddress();
                                Socket client = tcpConnections.get(choiceIp);
                                DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                                long totalLength = flag;
                                dataOutputStream.writeLong(totalLength);
                                byte buf[] = new byte[1024];
                                int length;
                                int fileLength = 0;
                                while (fileLength < totalLength) {
                                    length = dataInputStream.read(buf);
                                    dataOutputStream.write(buf, 0, length);
                                    fileLength += length;
                                }
                                dataOutputStream.flush();
                                Log.d(TAG, "组间通信：客户端写入完毕");
                                Message msg = new Message();
                                msg.what = 2;
                                mHandler.sendMessage(msg);

                            }else if(memberMap.containsKey(mac)){
                                Log.d(TAG, "目标设备不在本组内，继续relay");
                                //组主选择RelayClient
                                //...
                                //...
                                String macAddressofRelay = member.getMacAddressofRelay();
                                String choiceIp = memberMap.get(macAddressofRelay).getIpAddress();
                                Socket client = tcpConnections.get(choiceIp);
                                DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                                long type = StringToLong.transfer("Relaynod");
                                long totalLength = flag;
                                dataOutputStream.writeLong(type);
                                dataOutputStream.writeLong(totalLength);
                                dataOutputStream.writeBytes(mac);

                                byte buf[] = new byte[1024];
                                int length;
                                int fileLength = 0;
                                while (fileLength < totalLength) {
                                    length = dataInputStream.read(buf);
                                    dataOutputStream.write(buf, 0, length);
                                    fileLength += length;
                                }
                                dataOutputStream.flush();
                                Log.d(TAG, "组间通信：客户端写入完毕");
                                Message msg = new Message();
                                msg.what = 2;
                                mHandler.sendMessage(msg);

                            }
                        }
                    } else{
                        long totalLength = flag;
                        File file = new File(Environment.getExternalStorageDirectory() + "/"
                                + wiFiDirectActivity.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
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
                        outputStream.flush();
                        outputStream.close();
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
                ContentResolver cr = wiFiDirectActivity.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(wiFiDirectActivity, uri));
                stream.writeLong(file.length());
                System.out.println("ServerWrite:服务端写入开始 "+socket.getInetAddress().getHostAddress() + file.length());
                byte buf[] = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    //将buf中从0到length个字节写到输出流
                    stream.write(buf, 0, length);
                }
                System.out.println(GetPath.getPath(wiFiDirectActivity, uri));
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

