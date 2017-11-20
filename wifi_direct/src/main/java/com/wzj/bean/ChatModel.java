package com.wzj.bean;

/**
 * Created by wzj on 2017/5/12.
 */

public class ChatModel {
    private String icon = "";
    private String date;
    private String name;
    private String macAddress;
    private String content;
    public static final int CHAT_A = 1001;
    public static final int CHAT_B = 1002;
    private int type;

    public ChatModel() {
    }

    public ChatModel(String icon, String name, String macAddress, String content, int type) {
        this.icon = icon;
        this.name = name;
        this.macAddress = macAddress;
        this.content = content;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getMacAddress() {
        return macAddress;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
