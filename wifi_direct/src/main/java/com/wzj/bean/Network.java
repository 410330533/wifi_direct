package com.wzj.bean;

import android.net.wifi.p2p.WifiP2pDevice;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/4.
 */

public class Network {
    private WifiP2pDevice wifiP2pDevice;
    private double rssi;
    private String ssid;
    private double loadBalance;
    private double bandwidth = 5;
    private double groupOwnerPower;
    private double factors[];
    private int fSize = 0;
    private double pev;
    private Matrix mDegree;
    private Matrix mqv;
    private Matrix nqv;
    private boolean isGroupOwner = false;
    private String name;

    public Network(WifiP2pDevice wifiP2pDevice, double rssi, double loadBalance, double groupOwnerPower) {
        this.wifiP2pDevice = wifiP2pDevice;
        this.rssi = rssi;
        fSize++;
        this.loadBalance = loadBalance;
        fSize++;
        this.groupOwnerPower = groupOwnerPower;
        fSize++;
        factors = new double[++fSize];
        int i = 0;
        factors[i++] = rssi;
        factors[i++] = loadBalance;
        factors[i++] = bandwidth;
        factors[i] = groupOwnerPower;
    }

    public Network(WifiP2pDevice wifiP2pDevice, double rssi, double loadBalance, double bandwidth, double groupOwnerPower, boolean isGroupOwner) {
        this.wifiP2pDevice = wifiP2pDevice;
        this.rssi = rssi;
        this.loadBalance = loadBalance;
        this.bandwidth = bandwidth;
        this.groupOwnerPower = groupOwnerPower;
        this.isGroupOwner = isGroupOwner;
        factors = new double[4];
    }

    public Network(WifiP2pDevice wifiP2pDevice, double rssi, double loadBalance, double groupOwnerPower, boolean isGroupOwner) {
        this.wifiP2pDevice = wifiP2pDevice;
        this.rssi = rssi;
        fSize++;
        this.loadBalance = loadBalance;
        fSize++;
        this.groupOwnerPower = groupOwnerPower;
        fSize++;
        this.isGroupOwner = isGroupOwner;
        factors = new double[fSize];
        /*int i = 0;
        factors[i++] = rssi;
        factors[i++] = loadBalance;
        factors[i++] = bandwidth;
        factors[i] = groupOwnerPower;*/
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public double getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(double loadBalance) {
        this.loadBalance = loadBalance;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }



    public double[] getFactors() {
        int i = 0;
        factors[i++] = rssi;
        factors[i++] = loadBalance;
        factors[i++] = bandwidth;
        factors[i] = groupOwnerPower;
        return factors;
    }

    public int getfSize() {
        return fSize;
    }

    public double getPev() {
        return pev;
    }

    public void setPev(double pev) {
        this.pev = pev;
    }


    public Matrix getmDegree() {
        return mDegree;
    }

    public void setmDegree(Matrix mDegree) {
        this.mDegree = mDegree;
    }

    public Matrix getMqv() {
        return mqv;
    }

    public void setMqv(Matrix mqv) {
        this.mqv = mqv;
    }

    public Matrix getNqv() {
        return nqv;
    }

    public void setNqv(Matrix nqv) {
        this.nqv = nqv;
    }

    public WifiP2pDevice getWifiP2pDevice() {
        return wifiP2pDevice;
    }

    public void setWifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }

    public double getGroupOwnerPower() {
        return groupOwnerPower;
    }

    public void setGroupOwnerPower(float groupOwnerPower) {
        this.groupOwnerPower = groupOwnerPower;
    }

    public boolean isGroupOwner() {
        return isGroupOwner;
    }

    public void setGroupOwner(boolean groupOwner) {
        isGroupOwner = groupOwner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
}
