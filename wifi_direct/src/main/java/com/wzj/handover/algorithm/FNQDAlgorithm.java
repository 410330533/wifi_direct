package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by WZJ on 2017/11/4.
 */

public class FNQDAlgorithm {
    /*public static void main(String[] args){

        List<Network> candidateNetwork = new ArrayList<>();
        Network currentNetwork = new Network(3, -70, 0.7, 60, 0.2);
        *//*Network a = new Network(0, -40, 0.6, 50, 0.5);
        Network b = new Network(1, -50, 0.8, 70, 0.8);
        Network c = new Network(2, -90, 0.2, 20, 1);*//*
        for(int i=0;i<1000;i++){
            Network network = new Network(i, -i, 0.6, 50, 0.5);
            candidateNetwork.add(network);
        }
        *//*candidateNetwork.add(a);
        candidateNetwork.add(b);
        candidateNetwork.add(c);*//*
        double mParameters[][] = new double[][]{{-100, -60, -30}, {0, 0.5, 1}, {0, 50, 100}, {0, 0.5, 1}};
        double weights[] = new double[]{0.22, 0.1, 0.47, 0.21};
        double t = 0;
        FNQDAlgorithm fnqdAlgorithm = new FNQDAlgorithm();
        long startTime=System.currentTimeMillis();   //获取开始时间
        Network optimalNetwork = fnqdAlgorithm.fnqdProcess(currentNetwork, candidateNetwork, mParameters, weights, t);
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
    }*/
    public Network fnqdProcess(Network currentNetwrok, List<Network> candidateNetwork, double mParameters[][], double[] weights, double t){
        long startTime=System.currentTimeMillis();   //获取开始时间
        System.out.println("FNQD处理----------------------------------------------");
        Network optimalNetwork = null;
        double pev[] = new double[candidateNetwork.size()+1];
        candidateNetwork.add(currentNetwrok);
        //创建线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        Thread fuzzification = new Thread(new Fuzzification(mParameters));
        Thread membershipQuantitativeValue = new Thread(new MembershipQuantitativeValue(mParameters));
        Thread quantitativeDecision = new Thread(new QuantitativeDecision(weights, pev, candidateNetwork.size()));
        threadPool.execute(fuzzification);
        threadPool.execute(membershipQuantitativeValue);
        threadPool.execute(quantitativeDecision);
        for(int i = 0;i < candidateNetwork.size();i++){
            Fuzzification.blockingQueue.add(candidateNetwork.get(i));
        }
        synchronized (pev){
            try {
                System.out.println("线程等待，等待pev计算完成.............");
                pev.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
        for(int i = 0;i<candidateNetwork.size();i++){
            candidateNetwork.get(i).setPev(pev[i]);
        }
        //得到当前网络的PEV
        double cPEV = currentNetwrok.getPev();
        candidateNetwork.remove(candidateNetwork.size()-1);

        //筛选候选网络
        for(int i = 0;i<candidateNetwork.size();i++){
            if(candidateNetwork.get(i).getPev() <= cPEV + t){
                candidateNetwork.remove(i--);
            }
        }
        //选择最优切换网络
        double maxPEV = 0;
        for(Network n : candidateNetwork){
            if(n.getPev() > maxPEV){
                maxPEV = n.getPev();
                optimalNetwork = n;
            }
        }
        System.out.println("FNQD处理完成----------------------------------------------");

        //中断线程
       /* fuzzification.interrupt();
        membershipQuantitativeValue.interrupt();
        quantitativeDecision.interrupt();*/
        //关闭线程池
        List<Runnable> runnableList = threadPool.shutdownNow();
        if(null != optimalNetwork){
            System.out.println("最优切换网络："+ optimalNetwork.getWifiP2pDevice().deviceName);
        }else {
            System.out.println("无最优切换网络");
        }
        return optimalNetwork;
    }
}
