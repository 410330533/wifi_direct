package com.wzj.bean;

/**
 * Created by WZJ on 2018/4/18.
 */

public class BaseMember {
    private String ip;
    private String name;
    private String mac;

    public BaseMember(String ip, String name, String mac) {
        this.ip = ip;
        this.name = name;
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}
