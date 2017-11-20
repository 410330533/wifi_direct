package com.wzj.bean;

/**
 * Created by wzj on 2017/5/10.
 */

public class Message {
    String date;
    String name;
    String macAddress;
    String content;

    public Message(String name, String macAddress, String content) {
        this.name = name;
        this.macAddress = macAddress;
        this.content = content;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getContent() {
        return content;
    }
}
