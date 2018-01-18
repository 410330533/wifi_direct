package com.wzj.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wzj.bean.TCPMember;

import java.net.Socket;
import java.util.Map;

/**
 * Created by wzj on 2017/5/14.
 */

//用于向ChatActivity传递Socket对象。通过绑定该service，在DeviceDetailFragment中传入socket对象，在ChatActivity中读出。
public class SocketService extends Service {
    private Socket socket;
    private Map<String, TCPMember> tcpConnections;
    private Handler handler;
    private IBinder mBinder = new MBinder();

    public class MBinder extends Binder{
        public SocketService getService(){
            return SocketService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("创建service！！！！");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("绑定service！！！！");
        return mBinder;
    }

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public Socket getSocket(){
        return this.socket;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public Map<String, TCPMember> getTcpConnections() {
        return tcpConnections;
    }

    public void setTcpConnections(Map<String, TCPMember> tcpConnections) {
        this.tcpConnections = tcpConnections;
    }
}
