package com.wzj.bean;

/**
 * Created by wzj on 2017/4/23.
 */

public class Member {
    private String ipAddress;
    private String deviceName;
    private String macAddress;
    //private Socket socket = null;

    public Member(String ipAddress, String deviceName, String macAddress) {
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /*public void setSocket(Socket socket) {
        this.socket = socket;
    }*/

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }
/*
    public Socket getSocket() {
        return socket;
    }

    public boolean isTCPConnection(){
        return !(this.socket == null);
    }*/
}
