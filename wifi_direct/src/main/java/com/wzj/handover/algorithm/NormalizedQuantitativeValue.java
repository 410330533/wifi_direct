package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/5.
 */
//归一量化隶属度，计算NQV值(factor第二个元素为负载均衡LB)
public class NormalizedQuantitativeValue implements Runnable {
    public static BlockingQueue<Network> blockingQueue = new LinkedBlockingQueue<>();
    private double[][] mParameters;

    public NormalizedQuantitativeValue(double[][] mParameters) {
        this.mParameters = mParameters;
    }

    @Override
    public void run() {
        while (true){
            try {
                Network network = blockingQueue.take();
                System.out.println("计算NQV "+blockingQueue.size());
                 long startTime=System.nanoTime();   //获取开始时间
                double factors[] = network.getFactors();
                //计算NQV
                Matrix nqv = computeNQV(factors, mParameters);
                network.setNqv(nqv);
                MembershipQuantitativeValue.blockingQueue.add(network);
                long endTime=System.nanoTime(); //获取结束时间
                System.out.println("NoQV程序运行时间： "+(endTime-startTime)+"ms");
            } catch (InterruptedException e) {
                System.out.println("MembershipQuantitativeValue线程中止");
                break;
            }
        }

    }
    public Matrix computeNQV(double factors[], double mParameters[][]){
        double nqv[][] = new double[factors.length][mParameters[0].length];
        for(int i = 0;i < factors.length;i++){
            double min = mParameters[i][0];
            double max = mParameters[i][2];
            double normalized = (factors[i] - min)/(max - min);
            double tNQV[];
            if(i == 1){
                double ttNQV[] = {1.0/2.0 + normalized, 1.0/4.0 + normalized/2 , normalized/2 - 1.0/4.0};
                tNQV = ttNQV;
            }else{
                double ttNQV[] = {normalized/2, 1.0/4.0 + normalized/2.0 , normalized};
                tNQV = ttNQV;
            }
            nqv[i] = tNQV;
        }
        Matrix matrix = new Matrix(nqv);
        //System.out.print("计算NQV：");
        //matrix.print(matrix.getColumnDimension(), 4);
        return matrix;
    }
}
