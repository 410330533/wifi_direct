package com.wzj.bean;

import static com.wzj.wifi_direct.BatteryReceiver.power;

/**
 * Created by wzj on 2017/4/23.
 */

public class Member {
    private String ip;
    private String name;
    private String mac;
    private String macofRelay;
    private char type;
    private boolean flag = true; //isCurrentGroup
    //private float power;
    //private Socket socket = null;

    public Member(String ipAddress, String deviceName, String macAddress) {
        //super(ipAddress, deviceName, macAddress);
        this.ip = ipAddress;
        this.name = deviceName;
        this.mac = macAddress;
    }

    public Member(String ipAddress, String deviceName, String macAddress, float power) {
        //super(ipAddress, deviceName, macAddress);
        this.ip = ipAddress;
        this.name = deviceName;
        this.mac = macAddress;
       // this.power = power;
    }

    public void setMacAddress(String macAddress) {
        this.mac = macAddress;
    }

    public String getMacAddress() {
        return mac;
    }

    public void setIpAddress(String ipAddress) {
        this.ip = ipAddress;
    }

    public void setDeviceName(String deviceName) {
        this.name = deviceName;
    }

    /*public void setSocket(Socket socket) {
        this.socket = socket;
    }*/

    public String getIpAddress() {
        return ip;
    }

    public String getDeviceName() {
        return name;
    }

    public float getPower() {
        return power;
    }

    /*public void setPower(float power) {
        this.power = power;
    }*/

    public String getMacAddressofRelay() {
        return macofRelay;
    }

    public void setMacAddressofRelay(String macAddressofRelay) {
        this.macofRelay = macAddressofRelay;
    }

    public boolean getisCurrentGroup() {
        return flag;
    }

    public void setisCurrentGroup(boolean currentGroup) {
        flag = currentGroup;
    }

    public char getInterfaceType() {
        return type;
    }

    public void setInterfaceType(char interfaceType) {
        this.type = interfaceType;
    }

/*
    public Socket getSocket() {
        return socket;
    }

    public boolean isTCPConnection(){
        return !(this.socket == null);
    }*/
}
