package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/5.
 */
//模糊化
public class Fuzzification implements Runnable {

    public static BlockingQueue<Network> blockingQueue = new LinkedBlockingQueue<>();
    private double[][] mParameters;

    public Fuzzification(double[][] mParameters) {
        this.mParameters = mParameters;
    }

    @Override
    public void run() {
        while (true){
            try {
                Network network = blockingQueue.take();
                System.out.println("模糊化：" +blockingQueue.size());
                long startTime=System.nanoTime();   //获取开始时间
                System.out.println("第 "+ network.getName() +" 个候选网络开始处理---------------");
                double u[][] = new double[mParameters.length][mParameters[0].length];
                double factors[] = network.getFactors();
                for(int i = 0;i < factors.length;i++){
                    u[i] = membershipFunction(factors[i], mParameters[i]);
                }
                //隶属度矩阵
                Matrix matrix = new Matrix(u);
                matrix.print(matrix.getColumnDimension(), 4);
                network.setmDegree(matrix);
                MembershipQuantitativeValue.blockingQueue.add(network);
                long endTime=System.nanoTime(); //获取结束时间
                System.out.println("模糊化程序运行时间： "+(endTime-startTime)+"ms");
            } catch (InterruptedException e) {
                System.out.println("Fuzzification线程中止");
                break;
            }
        }

    }
    //计算隶属度
    private double[] membershipFunction(double factor, double[] mParameters){
        double mDegrees[] = new double[3];
        double a,b,c;
        a = mParameters[0];
        b = mParameters[1];
        c = mParameters[2];
        //Low(Weak)
        if(factor <= a){
            mDegrees[0] = 1;
        }else if(a < factor && factor <= b){
            if(factor - b == 0){
                mDegrees[0] = 0;
            }else{
                mDegrees[0] = (factor - b)/(a - b);
            }
        }else if(factor > b){
            mDegrees[0] = 0;
        }
        //Medium
        if(factor <= a || factor > c){
            mDegrees[1] = 0;
        }else if(a < factor && factor <= b){
            mDegrees[1] = (factor - a)/(b - a);
        }else if(b < factor && factor <= c){
            if(factor - c == 0){
                mDegrees[1] =0;
            }else{
                mDegrees[1] = (factor - c)/(b - c);
            }
        }
        //High(Strong)
        if(factor <= b){
            mDegrees[2] = 0;
        }else if(b < factor && factor <= c){
            mDegrees[2] = (factor - b)/(c - b);
        }else if(factor > c){
            mDegrees[2] = 1;
        }
        return mDegrees;
    }
}
