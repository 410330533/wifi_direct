package com.wzj.bean;

import java.net.Socket;

/**
 * Created by wzj on 2017/4/19.
 */

public class TCPMember {
    private String macAddress;
    private Socket socket;

    public TCPMember(String macAddress, Socket socket) {
        this.macAddress = macAddress;
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
