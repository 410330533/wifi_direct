package com.wzj.handover.algorithm;

import android.util.Log;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/4.
 */

public class FNQDAlgorithmSimple implements Runnable{
    public static final String TAG = "FNQDAlgorithm";
    private Map<String, Network> candidateNetwork;
    double mParameters[][];
    double weights[];
    double t;

    public FNQDAlgorithmSimple(Map<String, Network> candidateNetwork, double[][] mParameters, double[] weights, double t) {
        this.candidateNetwork = candidateNetwork;
        this.mParameters = mParameters;
        this.weights = weights;
        this.t = t;
    }

    @Override
    public void run() {

    }
    public Network fnqdProcess(){
        Log.d(TAG, "FNQD处理----------------------------------------------");
        Network optimalNetwork = null;
        /*for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Log.d(entry.getValue().getWifiP2pDevice().deviceName, " / " + entry.getValue().getRssi() +" / "+ entry.getValue().getLoadBalance() + " / " +entry.getValue().getBandwidth() + " / " + entry.getValue().getGroupOwnerPower());
        }*/
        double factors[][] = new double[candidateNetwork.size()][mParameters.length];
        int count = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            factors[count++] = entry.getValue().getFactors();
        }
        /*for(int i = 0;i < candidateNetwork.size();i++){
            factors[i] = candidateNetwork.get(i).getFactors();
        }*/

        //double pev[] = new double[factors.length];
        //FNQD核心步骤
        int i = 0;
        Network currentNetwrok = null;
        double cPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            Log.d(TAG, "第 "+ i +" 个候选网络开始处理---------------");
            Matrix u = fuzzification(factors[i], mParameters);
            Matrix nqv = computeNQV(factors[i], mParameters);
            //计算隶属度量化值MQV
            Matrix tMQV = nqv.times(u.transpose());
            double aMQV[] = new double[tMQV.getRowDimension()];
            for(int j = 0;j < tMQV.getRowDimension();j++){
                aMQV[j] = tMQV.get(j,j);
            }
            Matrix mqv = new Matrix(aMQV, aMQV.length);
            Log.d(TAG, "计算MQV：");
            //mqv.print(mqv.getColumnDimension(), 4);
            Matrix mWeights = new Matrix(weights, weights.length);
            //计算PEV值
            entry.getValue().setPev((mqv.transpose()).times(mWeights).get(0,0));
            Log.d(TAG, "计算PEV：" + entry.getValue().getPev());
            if(entry.getValue().isGroupOwner()){
                Log.d(TAG, "找到当前网络！");
                currentNetwrok = entry.getValue();
                cPEV = currentNetwrok.getPev();
            }
            Log.d(TAG, "第 "+ i++ +" 个候选网络处理结束---------------");

        }
        /*count = 0;
        double cPEV = 0;
        Network currentNetwrok = null;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            entry.getValue().setPev(pev[count++]);
            if(entry.getValue().isGroupOwner()){
                //找到当前网络
                currentNetwrok = entry.getValue();
                //candidateNetwork.remove(entry.getKey());
                cPEV = currentNetwrok.getPev();
                //Log.d(TAG, "找到当前网络："+currentNetwrok.getWifiP2pDevice().deviceName+"/"+cPEV);
            }
        }*/
        /*for(int i = 0;i<candidateNetwork.size();i++){
            candidateNetwork.get(i).setPev(pev[i]);
        }*/
        //计算当前网络的PEV
        /*System.out.println("当前网络开始处理---------------------");
        Log.d("当前网络：", " " + currentNetwrok.getRssi() +" "+ currentNetwrok.getBandwidth() + " " + currentNetwrok.getLoadBalance() + " " + currentNetwrok.getGroupOwnerPower());
        Matrix u = fuzzification(currentNetwrok.getFactors(), mParameters);
        Matrix nqv = computeNQV(currentNetwrok.getFactors(), mParameters);
        Matrix tMQV = nqv.times(u.transpose());
        double aMQV[] = new double[tMQV.getRowDimension()];
        for(int j = 0;j < tMQV.getRowDimension();j++){
            aMQV[j] = tMQV.get(j,j);
        }
        Matrix mqv = new Matrix(aMQV, aMQV.length);
        System.out.println("计算MQV：");
        //mqv.print(mqv.getColumnDimension(), 4);
        Matrix mWeights = new Matrix(weights, weights.length);
        double cPEV = (mqv.transpose()).times(mWeights).get(0,0);
        currentNetwrok.setPev(cPEV);
        System.out.println("计算PEV："+ cPEV);
        //System.out.println("当前网络处理结束---------------------");*/
        //筛选候选网络、选择最优切换网络
        double maxPEV = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            if(entry.getValue().getPev() > cPEV + t && entry.getValue().getPev() > maxPEV){
                maxPEV = entry.getValue().getPev();
                optimalNetwork = entry.getValue();
            }
        }

        //选择最优切换网络

        /*for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            if(entry.getValue().getPev() > maxPEV){
                maxPEV = entry.getValue().getPev();
                optimalNetwork = entry.getValue();
            }
        }*/
        /*for(Network n : candidateNetwork){
            if(n.getPev() > maxPEV){
                maxPEV = n.getPev();
                optimalNetwork = n;
            }
        }*/
        if(null != optimalNetwork){
            Log.d(TAG, "最优切换网络："+ optimalNetwork.getWifiP2pDevice().deviceName+"/"+optimalNetwork.getPev());
            optimalNetwork.setGroupOwner(true);
            if(currentNetwrok != null){
                currentNetwrok.setGroupOwner(false);
            }
            for(Entry<String, Network> entry : candidateNetwork.entrySet()){
                if(!entry.getValue().isGroupOwner()){
                    candidateNetwork.remove(entry.getKey());
                }
            }
        }else {
            Log.d(TAG, "无最优切换网络");
        }


        Log.d(TAG, "FNQD处理完成----------------------------------------------");
        return optimalNetwork;
    }

    //模糊化
    public Matrix fuzzification(double factors[], double mParameters[][]){
        double u[][] = new double[factors.length][mParameters[0].length];
        for(int i = 0;i < factors.length;i++){
            u[i] = membershipFunction(factors[i], mParameters[i]);
        }
        //隶属度矩阵
        Matrix matrix = new Matrix(u);
        Log.d(TAG, "模糊化：" );
        //matrix.print(matrix.getColumnDimension(), 4);
        return matrix;
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
    //
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
