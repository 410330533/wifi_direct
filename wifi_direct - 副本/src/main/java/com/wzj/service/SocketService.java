package com.wzj.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.net.Socket;

/**
 * Created by wzj on 2017/5/14.
 */

public class SocketService extends Service {
    private Socket socket;
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
}
