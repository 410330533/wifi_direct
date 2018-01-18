package com.wzj.communication;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wzj.bean.Member;
import com.wzj.util.GetPath;
import com.wzj.util.StringToLong;
import com.wzj.wifi_direct.DeviceDetailFragment;
import com.wzj.wifi_direct.WiFiDirectActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * Created by wzj on 2017/4/24.
 */

//用于两个组员间通信,只使用了MSRead方式，MSWrite方法目前未使用，所有写的操作都通过ClientThread中相关的写方法来实现
public class MemberServerThread implements Runnable {
    private Context context;
    private static ServerSocket serverSocket;
    private int count = 1;
    private Handler mHandler;
    private String type;
    private Socket socket;
    private Uri uri;
    private Map<String, Socket> tcpConnections;
    private Map<String, Member> memberMap;

    public MemberServerThread(Context context, Map<String, Member> memberMap, Handler mHandler, String type, Map<String, Socket> tcpConnections) {
        this.context = context;
        this.memberMap = memberMap;
        this.mHandler = mHandler;
        this.type = type;
        this.tcpConnections = tcpConnections;
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

    @Override
    public void run() {
        try {
            if(serverSocket == null){
                serverSocket = new ServerSocket(DeviceDetailFragment.MS_PORT);
            }
            Log.d(WiFiDirectActivity.TAG, "MemberServerThread：线程启动");
            if(type.equals("read")){
                while (true) {
                    System.out.println("MemberServerThread:执行次数 "+count);
                    Socket client = serverSocket.accept();
                    this.socket = client;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String deviceName = bufferedReader.readLine();
                    String macAddress = bufferedReader.readLine();
                    //更新tcpConnections；组间通信，IP冲突

                    if(socket.getInetAddress().getHostAddress().equals(DeviceDetailFragment.GO_ADDRESS) && tcpConnections.containsKey(DeviceDetailFragment.GO_ADDRESS)){
                        Log.d("IP冲突", client.getInetAddress().getHostAddress());
                        tcpConnections.put("192.168.49.0", client);
                        memberMap.put(macAddress, new Member("192.168.49.0", deviceName, macAddress));
                        Message msg = new Message();
                        msg.what = 6;
                        mHandler.sendMessage(msg);
                    }else{
                        tcpConnections.put(socket.getInetAddress().getHostAddress(), client);
                    }
                    System.out.println("MemberServerThread: "+ memberMap.size()+" " +memberMap.get(client.getInetAddress().getHostAddress()));
                    new Thread(new MSRead(client)).start();
                }
            }else if(type.equals("write")){
                new Thread(new MSWrite()).start();
            }

        } catch (IOException e) {
            Log.e(WiFiDirectActivity.TAG, "MemberServerThread 84");
            e.printStackTrace();
        }
    }
    public static void close(){
        if(serverSocket != null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
                serverSocket = null;
                System.out.println("MemberServerThread关闭！！！！！！");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "MemberServerThread 96");
                e.printStackTrace();
            }
        }
    }
    private class MSRead implements Runnable{
        private Socket socket;

        public MSRead(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                while (socket.isConnected()){
                    Log.d(WiFiDirectActivity.TAG, "MemberServerRead: 连接到client " + socket.toString());
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    //无数据，阻塞
                    long flag = inputStream.readLong();
                    if(flag == StringToLong.transfer("Messagem")){
                        //文本消息
                        String message = "";
                        message = inputStream.readUTF();
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
                        FileOutputStream outputStream = new FileOutputStream(file);
                        Log.d(WiFiDirectActivity.TAG, "MemberServerRead: -"+ count++ +"- AsyncTask处理client请求 " + file.toString());
                        Log.d(WiFiDirectActivity.TAG,"MemberServerRead:处理client请求"+ file.toString());
                        while (fileLength < totalLength) {
                            len = inputStream.read(buf);
                            outputStream.write(buf, 0, len);
                            fileLength += len;
                        }
                        System.out.println("MemberServerRead: 读取完毕。。。。");
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("file", file.getAbsolutePath());
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }

                }

            } catch (IOException e){
                Log.e(WiFiDirectActivity.TAG, "MSRead 150");
                e.printStackTrace();
            } finally {
                try {
                    if(socket != null && !socket.isClosed()){
                        tcpConnections.remove(socket.getInetAddress().getHostAddress());
                        socket.close();
                        System.out.println("MSsocket 关闭");
                    }
                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, "MSRead 154");
                    e.printStackTrace();
                }
            }
        }
    }


    private class MSWrite implements Runnable{

        @Override
        public void run() {
            try {
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = context.getContentResolver();
                InputStream in = null;
                in = cr.openInputStream(uri);
                File file = new File(GetPath.getPath(context, uri));
                stream.writeLong(file.length());
                System.out.println("MemberServerWrite:服务端写入开始 "+socket.getInetAddress().getHostAddress() + file.length());
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
                Log.d(WiFiDirectActivity.TAG, "MemberServerWrite：写入完毕");
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
