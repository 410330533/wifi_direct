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
import com.google.gson.reflect.TypeToken;
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
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;

/**
 * Created by wzj on 2017/3/3.
 */

public class ClientThread implements Runnable {
    public static final String TAG = "ClientThread";
    private static final int SOCKET_TIMEOUT = 1000 * 60 * 60;
    private WiFiDirectActivity wiFiDirectActivity;
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
    private String macAddress;
    public ClientThread(WiFiDirectActivity wiFiDirectActivity, Handler mHandler, String address, int port, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections) {
        this.wiFiDirectActivity = wiFiDirectActivity;
        this.mHandler = mHandler;
        this.address = address;
        this.port = port;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
    }

    public ClientThread(WiFiDirectActivity wiFiDirectActivity, Handler mHandler, String address, int port, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections, Map<String, Member> memberMap) {
        this.wiFiDirectActivity = wiFiDirectActivity;
        this.mHandler = mHandler;
        this.address = address;
        this.port = port;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
        this.memberMap = memberMap;
    }


    public ClientThread(WiFiDirectActivity wiFiDirectActivity, Handler mHandler, WifiP2pDevice myDevice) {
        this.wiFiDirectActivity = wiFiDirectActivity;
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

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
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
                    BroadcastThread broadcastThread = new BroadcastThread(BroadcastThread.BROADCAST_READ, mHandler);
                    broadcastThread.setIpAddress(ipAddress);
                    new Thread(broadcastThread).start();
                    DeviceDetailFragment.setBroadcastThreadRead(broadcastThread);
                    //周期性广播本机信息
                    /*final Member member = new Member(ipAddress, myDevice.deviceName, myDevice.deviceAddress, power);
                    if(timer == null){
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                member.setPower(power);
                                BroadcastThread udpBroadcastWrite = new BroadcastThread(BroadcastThread.BROADCAST_WRITE, BroadcastThread.ADD_MEMBER, member);
                                new Thread(udpBroadcastWrite).start();
                            }
                        }, broadcastPeriod, broadcastPeriod);
                    }*/
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
                   /* BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.write(myDevice.deviceName);
                    writer.newLine();
                    writer.write(myDevice.deviceAddress);
                    writer.newLine();
                    writer.flush();*/
                    tcpConnections.put(socket.getInetAddress().getHostAddress(), socket);
                }
            }
            Log.d(WiFiDirectActivity.TAG, "ClientThread：client socket已连接" + socket.isConnected());
            if(type.equals("read")){
                new Thread(new ClientRead(wiFiDirectActivity, socket)).start();
            }else if(type.equals("write")){
                new Thread(new ClientWrite()).start();
            }else if(type.equals("rw")){
                new Thread(new ClientWrite()).start();
                new Thread(new ClientRead(wiFiDirectActivity, socket)).start();
            }else if(type.equals("message")){
                new Thread(new ClientWriteMessage()).start();
            }else if(type.equals("relay")){
                new Thread(new RelayClientWrite(macAddress)).start();
            }else if(type.equals("relay-p")){
                new Thread(new RelayClientP2pWrite(macAddress)).start();
            } else if(type.equals("relay-rw")){
                new Thread(new RelayClientWrite(macAddress)).start();
                new Thread(new ClientRead(wiFiDirectActivity, socket)).start();
            }else if(type.equals("relay-message")){
                new Thread(new RelayClientWriteMessage()).start();
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
    //relay写文本消息
    private class RelayClientWriteMessage implements Runnable {

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入message开始 ");
                long flag = StringToLong.transfer("Relaynod");
                stream.writeLong(flag);
                flag = StringToLong.transfer("Messagem");
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
    //Relay-p写图片
    private class RelayClientP2pWrite implements Runnable {
        private String mac;

        public RelayClientP2pWrite(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = wiFiDirectActivity.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(wiFiDirectActivity, uri));
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入开始 " + GetPath.getPath(wiFiDirectActivity, uri));
                long flag = StringToLong.transfer("Relayngo");
                stream.writeLong(flag);
                stream.writeLong(file.length());
                stream.writeBytes(mac);
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
                    if(socket.getInetAddress().getHostAddress() != null){
                        if(tcpConnections.containsKey(socket.getInetAddress().getHostAddress())){
                            tcpConnections.remove(socket.getInetAddress().getHostAddress());
                        }
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

    //Relay写图片
    private class RelayClientWrite implements Runnable {
        private String mac;

        public RelayClientWrite(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = wiFiDirectActivity.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(wiFiDirectActivity, uri));
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入开始 " + GetPath.getPath(wiFiDirectActivity, uri));
                long flag = StringToLong.transfer("Relaynod");
                stream.writeLong(flag);
                stream.writeLong(file.length());
                stream.writeBytes(mac);
                byte buf[] = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    //将buf中从0到length个字节写到输出流
                    stream.write(buf, 0, length);
                }
                in.close();
                stream.flush();
                /*DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                int bit = 1024*1024 - 72;
                int byteSize = bit/8;
                Load load = new Load(byteSize);
                ByteArrayInputStream in = load.offeredLoad();
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入开始 " +System.nanoTime());
                long flag = StringToLong.transfer("Relaynod");
                stream.writeLong(flag);
                stream.writeLong(byteSize);
                stream.writeBytes(mac);
                byte buf[] = new byte[byteSize];
                int length;
                while ((length = in.read(buf)) != -1) {
                    //将buf中从0到length个字节写到输出流
                    stream.write(buf, 0, length);
                }
                in.close();
                stream.flush();*/
                //stream.close();
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入完毕"+System.nanoTime());
                Message msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);
            } catch (FileNotFoundException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                e.printStackTrace();

            } catch (IOException e) {
                Log.d(WiFiDirectActivity.TAG, e.toString());
                if(socket != null ){
                    if(socket.getInetAddress().getHostAddress() != null){
                        if(tcpConnections.containsKey(socket.getInetAddress().getHostAddress())){
                            tcpConnections.remove(socket.getInetAddress().getHostAddress());
                        }
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
    //写图片
    private class ClientWrite implements Runnable {

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = wiFiDirectActivity.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(wiFiDirectActivity, uri));
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入开始 " + GetPath.getPath(wiFiDirectActivity, uri));
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
                    if(socket.getInetAddress().getHostAddress() != null){
                        if(tcpConnections.containsKey(socket.getInetAddress().getHostAddress())){
                            tcpConnections.remove(socket.getInetAddress().getHostAddress());
                        }
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
        private WiFiDirectActivity wiFiDirectActivity;
        private Socket socket;
        public ClientRead(WiFiDirectActivity wiFiDirectActivity, Socket socket) {
            this.wiFiDirectActivity = wiFiDirectActivity;
            this.socket = socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

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

                    }else if(flag == StringToLong.transfer("Relaynod")){ //组间通信
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
                                System.out.println("组间通信ClientRead: 读取完毕。。。。"+ fileLength);
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
                                if(client == null){
                                    client = new Socket();
                                    client.connect(new InetSocketAddress(choiceIp, DeviceDetailFragment.MS_PORT));
                                    ClientRead clientRead = new ClientRead(wiFiDirectActivity, client);
                                    new Thread(clientRead).start();
                                    tcpConnections.put(client.getInetAddress().getHostAddress(), client);
                                }
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

                                String macAddressofRelay = member.getMacAddressofRelay();
                                char interfaceType = member.getInterfaceType();
                                String choiceIp = memberMap.get(macAddressofRelay).getIpAddress();
                                Socket client = tcpConnections.get(choiceIp);
                                String messageType = "Relaynod";
                                Log.d(TAG, "目标设备不在本组内，继续relay "+ interfaceType);

                                if(interfaceType == 'p'){
                                    Log.d(TAG, "GO中继");
                                    client = tcpConnections.get(DeviceDetailFragment.GO_ADDRESS);
                                    messageType = "Relayngo";
                                }else {
                                    if(client == null){
                                        Log.d(TAG, "'w'- 自左向右");
                                        client = new Socket();
                                        client.connect(new InetSocketAddress(choiceIp, DeviceDetailFragment.MS_PORT));
                                        ClientRead clientRead = new ClientRead(wiFiDirectActivity, client);
                                        new Thread(clientRead).start();
                                        tcpConnections.put(client.getInetAddress().getHostAddress(), client);
                                    }
                                }

                                DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                                long type = StringToLong.transfer(messageType);
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



