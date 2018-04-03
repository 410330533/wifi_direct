package com.wzj.bean;

/**
 * Created by wzj on 2017/4/23.
 */

public class Member {
    private String ipAddress;
    private String deviceName;
    private String macAddress;
    private String macAddressofRelay;
    private char interfaceType;
    private boolean isCurrentGroup = true;
    private float power;
    //private Socket socket = null;

    public Member(String ipAddress, String deviceName, String macAddress) {
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    public Member(String ipAddress, String deviceName, String macAddress, float power) {
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.power = power;
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

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public String getMacAddressofRelay() {
        return macAddressofRelay;
    }

    public void setMacAddressofRelay(String macAddressofRelay) {
        this.macAddressofRelay = macAddressofRelay;
    }

    public boolean getisCurrentGroup() {
        return isCurrentGroup;
    }

    public void setisCurrentGroup(boolean currentGroup) {
        isCurrentGroup = currentGroup;
    }

    public char getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(char interfaceType) {
        this.interfaceType = interfaceType;
    }

/*
    public Socket getSocket() {
        return socket;
    }

    public boolean isTCPConnection(){
        return !(this.socket == null);
    }*/
}
