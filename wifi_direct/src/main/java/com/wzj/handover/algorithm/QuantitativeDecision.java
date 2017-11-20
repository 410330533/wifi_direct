package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/5.
 */

public class QuantitativeDecision implements Runnable {
    public static BlockingQueue<Network> blockingQueue = new LinkedBlockingQueue<>();
    double weights[];
    double[] pev;
    private int counter = 0;
    private int candidateSize;

    public QuantitativeDecision(double[] weights, double[] pev, int candidateSize) {
        this.weights = weights;
        this.pev = pev;
        this.candidateSize = candidateSize;
    }

    @Override
    public void run() {
        while (true){
            try {
                Network network = blockingQueue.take();
                System.out.println("计算PEV开始 "+ blockingQueue.size());
                //long startTime=System.nanoTime();   //获取开始时间
                Matrix mWeights = new Matrix(weights, weights.length);
                Matrix mqv = network.getMqv();
                //基于mWeights与mqv，计算PEV值
                double p =(mqv.transpose()).times(mWeights).get(0,0);
                pev[counter] = p;
                //System.out.println("计算PEV" + network.getId() + "："+ p);
                //System.out.println("第 "+ network.getId() +" 个候选网络处理结束---------------");
                counter++;
                //当所有PEV都计算完成后，释放锁
                if(counter == candidateSize){
                    synchronized (pev){
                        pev.notify();
                        System.out.println("释放对象锁.........."+counter);
                    }
                }
                //long endTime=System.nanoTime(); //获取结束时间
                //System.out.println("QD程序运行时间： "+(endTime-startTime)+"ms");
            } catch (InterruptedException e) {
                System.out.println("QuantitativeDecision线程中止");
                break;
            }
        }


    }
}
