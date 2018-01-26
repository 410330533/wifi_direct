package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by WZJ on 2017/11/16.
 */

public class RSSIBasedAlgorithmT {
    public static void main(String[] args){
        Map<String, Network> candidateNetwork = new ConcurrentHashMap<>();
        Network currentNetwork = new Network(null, -70, 2/20.0, 0.7, 0.8, true);
        currentNetwork.setName("a");
        /*Network b = new Network(null, -30, 10/20.0, 0.5, 0.7, false);
        b.setName("b");
        Network c = new Network(null, -35, 2/20.0, 0.9, 0.8, true);
        c.setName("c");
        Network d = new Network(null, -40, 0, 1, 0.4, false);
        d.setName("d");*/
        candidateNetwork.put("a", currentNetwork);
        /*candidateNetwork.put("b", b);
        candidateNetwork.put("c", c);
        candidateNetwork.put("d", d);*/
        long times = 0;
        for(int j =0 ;j < 10;j++){
            for(int i=0;i<100;i++){
                Network network = new Network(null, -i, 0.6, 50, 0.5, false);
                candidateNetwork.put(""+i, network);
            }
            double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
            double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
            double t = 0.2;
            RSSIBasedAlgorithmT rssiBasedAlgorithmT = new RSSIBasedAlgorithmT(candidateNetwork, -70, 20);
            long startTime=System.nanoTime();   //获取开始时间
            Network optimalNetwork = rssiBasedAlgorithmT.process();
            long endTime=System.nanoTime(); //获取结束时间
            System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
            times += endTime-startTime;
        }
        System.out.println("程序平均运行时间： "+times/(10*1000000.0)+"ms");
        /*RSSIBasedAlgorithmT rssiBasedAlgorithmT = new RSSIBasedAlgorithmT(candidateNetwork, -70, 20);
        long startTime=System.currentTimeMillis();   //获取开始时间
        Network optimalNetwork = rssiBasedAlgorithmT.process();
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");*/
    }
    public static final String TAG = "RSSIBasedAlgorithm";
    private Map<String, Network> candidateNetwork;
    private float rssi;
    private int t;

    public RSSIBasedAlgorithmT(Map<String, Network> candidateNetwork, float rssi, int t) {
        this.candidateNetwork = candidateNetwork;
        this.rssi = rssi;
        this.t = t;
    }

    public Network process(){
        System.out.println("开始处理-----------------  " );
        System.out.println("当前网络RSSI：" +rssi);
        Network optimalNetwork = null;
        double maxRssi = -100;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            System.out.println("处理：" +entry.getKey());
            Network network = entry.getValue();
            if(!network.isGroupOwner()){
                if(network.getRssi() > maxRssi && network.getRssi() - rssi > t){
                    maxRssi = network.getRssi();
                    optimalNetwork = network;
                }
            }
        }
        if(optimalNetwork != null){
            System.out.println("最优切换网络："+optimalNetwork.getName()+" " + maxRssi);
        }else {
            System.out.println("无最优候选网络 "+ maxRssi);
        }
        System.out.println("处理完成-----------------");
        return optimalNetwork;

    }
}
