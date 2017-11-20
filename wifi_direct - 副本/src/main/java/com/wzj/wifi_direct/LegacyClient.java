package com.wzj.wifi_direct;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wzj.communication.UDPBroadcast;
import com.wzj.util.GetPath;
import com.wzj.util.StringToLong;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Map;

/**
 * Created by wzj on 2017/6/6.
 */

public class LegacyClient implements Runnable{
    private static final int SOCKET_TIMEOUT = 1000 * 60 * 60;
    private Context context;
    private Handler mHandler;
    private Uri uri;
    private String address;
    private int port;
    private Socket socket;
    private WifiP2pDevice myDevice;
    private Map<String, Socket> tcpConnections;
    private String type = "read";
    private String message;

    public LegacyClient(Context context, Handler mHandler, String address, int port, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections) {
        this.context = context;
        this.mHandler = mHandler;
        this.address = address;
        this.port = port;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
    }
    public LegacyClient(Context context, Handler mHandler, WifiP2pDevice myDevice, Map<String, Socket> tcpConnections) {
        this.context = context;
        this.mHandler = mHandler;
        this.myDevice = myDevice;
        this.tcpConnections = tcpConnections;
    }

    public LegacyClient(Socket socket, String type, String message) {
        this.socket = socket;
        this.type = type;
        this.message = message;
    }
    public LegacyClient(String type, String message) {
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
            /*try {
                socket.setReuseAddress(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }*/
            socket = new Socket();

            try {
                socket.setReuseAddress(true);
                System.out.println("----- "+NetworkInterface.getByName("wlan0").getInetAddresses().nextElement().getHostAddress());
                socket.bind(new InetSocketAddress("192.168.49.167",47809));
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                socket.connect((new InetSocketAddress(DeviceDetailFragment.GO_ADDRESS, DeviceDetailFragment.PORT)));
                //socket.connect((new InetSocketAddress(address, port)));
                //更新tcpConnections
                tcpConnections.put(socket.getInetAddress().getHostAddress(), socket);
                //更新memberList
               /* Message msg = new Message();
                Gson gson = new Gson();
                Map<String, Member> map = new HashMap<>();
                Map<String, Map<String, Member>> memberMap = new HashMap<>();
                map.put(socket.getInetAddress().getHostAddress(), new Member(socket.getInetAddress().getHostAddress(),
                        "(GO)"+group.getOwner().deviceName));
                memberMap.put(group.getOwner().deviceAddress, map);
                String str = gson.toJson(memberMap);
                msg.what = 5;
                Bundle bundle = new Bundle();
                bundle.putString("memberMap", str.trim());*/
                //将本机信息发送给GO
                if(socket.getInetAddress().getHostAddress().equals(DeviceDetailFragment.GO_ADDRESS)){
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    writer.write(myDevice.deviceName);
                    writer.newLine();
                    writer.write(myDevice.deviceAddress);
                    writer.newLine();
                    writer.flush();
                    //启动广播包接收线程
                    UDPBroadcast udpBroadcast = new UDPBroadcast(mHandler);
                    udpBroadcast.setType(1);
                    new Thread(udpBroadcast).start();
                }
            }
            Log.d(WiFiDirectActivity.TAG, "ClientThread：client socket已连接" + socket.isConnected());
            if(type.equals("read")){
                new Thread(new LegacyRead()).start();
            }else if(type.equals("write")){
                new Thread(new LegacyWrite()).start();

            }else if(type.equals("rw")){
                new Thread(new LegacyWrite()).start();
                new Thread(new LegacyRead()).start();
            }else if(type.equals("message")){
                new Thread(new LegacyWriteMessage()).start();
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

    private class LegacyWriteMessage implements Runnable {

        @Override
        public void run() {

            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入message开始 ");
                long flag = StringToLong.transfer("Messagem");
                stream.writeLong(flag);
                //stream.writeLong(message.length());
                /*System.out.println("-----message "+message);
                stream.writeBytes(message);*/
                stream.writeUTF(message);
                stream.flush();
                //stream.close();
                Log.d(WiFiDirectActivity.TAG, "ClientWrite：客户端写入message完毕");
                /*Message msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);*/
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
    private class LegacyWrite implements Runnable {

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
                //Demo代码不完整
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
                System.out.println(e.getMessage());
                e.printStackTrace();

                if(e.getMessage().equals("sendto failed: EPIPE (Broken pipe)")){
                    try {
                        socket.close();
                        socket = null;
                        Log.d(WiFiDirectActivity.TAG, "ClientThread：关闭client socket");
                        //提示用户重新连接
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

    private class LegacyRead implements Runnable {

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
                        /*long totalLength = inputStream.readLong();
                        byte buf[] = new byte[4096];
                        int len = 0;
                        int msg_leng = 0;
                        while(msg_leng < totalLength){
                            len = inputStream.read(buf);
                            String str = new String(buf, 0, len);
                            message += str;
                            msg_leng += len;
                        }*/
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
