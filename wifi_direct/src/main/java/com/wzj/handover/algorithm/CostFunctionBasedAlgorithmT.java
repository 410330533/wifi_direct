package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.wzj.handover.algorithm.FNQDAlgorithmSimpleT.counter;

/**
 * Created by WZJ on 2017/11/16.
 */

public class CostFunctionBasedAlgorithmT {

    public static void main(String[] args){
        Map<String, Network> candidateNetwork = new ConcurrentHashMap<>();
        Network currentNetwork = new Network(null, -70, 2/20.0, 0.7, 0.8, true);
        currentNetwork.setName("a");
        /*Network a = new Network(null, -70, 10/20.0, 0.35, 1, true);
        a.setName("a");
        Network b = new Network(null, -30, 12/20.0, 0.7, 0.6, false);
        b.setName("b");*/

        /*Network a = new Network(null, -30, 10/20.0, 0.2, 1, false);
        a.setName("a");
        Network b = new Network(null, -70, 12/20.0, 0.1, 0.6, true);
        b.setName("b");
        candidateNetwork.put("a", a);
        candidateNetwork.put("b", b);*/
        candidateNetwork.put("a", currentNetwork);

        long times = 0;
        for(int j =0 ;j < 10;j++){
            for(int i=0;i<100;i++){
                Network network = new Network(null, -i, 0.6, 50, 0.5, false);
                candidateNetwork.put(""+i, network);
            }
            double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
            double weights[] = new double[]{0.38, -0.17, 0.34, 0.11};
            double t = 0.2;
            CostFunctionBasedAlgorithmT costFunctionBasedAlgorithmT = new CostFunctionBasedAlgorithmT(candidateNetwork, t, weights);
            long startTime=System.nanoTime();   //获取开始时间
            Network optimalNetwork = costFunctionBasedAlgorithmT.process();
            long endTime=System.nanoTime(); //获取结束时间
            System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
            times += endTime-startTime;
        }
        System.out.println("程序平均运行时间： "+times/(10*1000000.0)+"ms");
        /*double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
        double weights[] = new double[]{0.38, -0.17, 0.34, 0.11};
        double t = 0.3;

        for(int i = 1; i < 21; i ++){
            CostFunctionBasedAlgorithmT costFunctionBasedAlgorithmT = new CostFunctionBasedAlgorithmT(candidateNetwork, t, weights);
            Network optimalNetwork = costFunctionBasedAlgorithmT.process();
            if(i%2 == 0){
                a.setBandwidth(a.getBandwidth()+0.025);
                b.setBandwidth(b.getBandwidth()+0.1);
            }
        }
        System.out.println(counter);*/
    }
    public static final String TAG = "CFBdAlgorithm";
    private Map<String, Network> candidateNetwork;
    private double t;
    private double[] weights;

    public CostFunctionBasedAlgorithmT(Map<String, Network> candidateNetwork, double t, double[] weights) {
        this.candidateNetwork = candidateNetwork;
        this.t = t;
        this.weights = weights;
    }

    public Network process(){
        System.out.println("开始处理------------------------");
        Network optimalNetwork = null;
        Network currentNetwork = null;
        double cPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Network network = entry.getValue();
            if(network.isGroupOwner()){
                currentNetwork = network;
                double[] factors = network.getFactors();
                factors[0] = (factors[0] + 100) / 100;
                for(int i = 0; i<factors.length; i++){
                    cPEV += weights[i] * factors[i];
                }
                System.out.println("计算组主PEV "+ cPEV);
            }
        }
        double maxPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Network network = entry.getValue();
            if(!network.isGroupOwner()){
                double[] factors = network.getFactors();
                double pev = 0;
                factors[0] = (factors[0] + 100) / 100;
                for(int i = 0; i<factors.length; i++){
                    pev += weights[i] * factors[i];
                }
                System.out.println(""+pev);
                if(pev > maxPEV && pev - cPEV > t){
                    maxPEV = pev;
                    optimalNetwork = network;
                }
            }

        }
        if(optimalNetwork != null){
            System.out.println("最优切换网络："+optimalNetwork.getName() + " " +maxPEV);
            //optimalNetwork.setGroupOwner(true);
            //currentNetwork.setGroupOwner(false);
            double rssi = optimalNetwork.getRssi();
            //optimalNetwork.setRssi(currentNetwork.getRssi());
            //currentNetwork.setRssi(rssi);
            counter++;
        }else {
            System.out.println("无最优候选网络 "+ maxPEV);
            double rssi = candidateNetwork.get("a").getRssi();
            //candidateNetwork.get("a").setRssi(candidateNetwork.get("b").getRssi());
            //candidateNetwork.get("b").setRssi(rssi);
        }
        System.out.println("处理完成------------------------");
        return optimalNetwork;
    }
}
