package com.wzj.bean;

import java.net.Socket;

/**
 * Created by wzj on 2017/4/19.
 */

public class TCPMember {
    private Socket socket;
    private String deviceName;

    public TCPMember(Socket socket, String deviceName) {
        this.socket = socket;
        this.deviceName = deviceName;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress(){
        return socket.getInetAddress().getHostAddress();
    }

}
