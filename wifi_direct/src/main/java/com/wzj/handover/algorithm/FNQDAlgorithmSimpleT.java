package com.wzj.handover.algorithm;

import com.wzj.bean.Network;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import Jama.Matrix;

/**
 * Created by WZJ on 2017/11/4.
 */

public class FNQDAlgorithmSimpleT implements Runnable{
    public static int counter = 0;
    public static void main(String[] args){
        Map<String, Network> candidateNetwork = new ConcurrentHashMap<>();
        Network currentNetwork = new Network(null, -80, 2/20.0, 0.7, 0.8, true);
        currentNetwork.setName("a");
/*
        Network a = new Network(null, -70, 10/20.0, 0.2, 1, true);
        a.setName("a");
        Network b = new Network(null, -30, 12/20.0, 0.1, 0.6, false);
        b.setName("b");
        candidateNetwork.put("a", a);
        candidateNetwork.put("b", b);*/

        /*Network a = new Network(null, -30, 10/20.0, 0.25, 1, false);
        a.setName("a");
        Network b = new Network(null, -70, 12/20.0, 0.3, 0.6, true);
        b.setName("b");*/


        candidateNetwork.put("a", currentNetwork);
        long times = 0;
        for(int j =0 ;j < 10;j++) {
            for (int i = 0; i < 90; i++) {
                Network network = new Network(null, -i, 0.6, 50, 0.5, false);
                candidateNetwork.put("" + i, network);
            }
            double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
            double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
            double t = 0.3;
            FNQDAlgorithmSimpleT fnqdAlgorithm = new FNQDAlgorithmSimpleT(candidateNetwork, mParameters, weights, t);
            long startTime=System.nanoTime();   //获取开始时间
            Network optimalNetwork = fnqdAlgorithm.fnqdProcess();
            long endTime=System.nanoTime(); //获取结束时间
            System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
            times += endTime-startTime;
        }
        System.out.println("程序平均运行时间： "+times/(10*1000000.0)+"ms");
        /*double mParameters[][] = new double[][]{{-100, -60, 0}, {0, 0.5, 1}, {0, 0.5, 1}, {0, 0.5, 1}};
        double weights[] = new double[]{0.38, 0.17, 0.34, 0.11};
        double t = 0.3;
        for(int i = 1; i < 21; i ++){
            FNQDAlgorithmSimpleT fnqdAlgorithm = new FNQDAlgorithmSimpleT(candidateNetwork, mParameters, weights, t);
            Network optimalNetwork = fnqdAlgorithm.fnqdProcess();
            if(i%2 == 0){
                a.setBandwidth(a.getBandwidth()+0.025);
                b.setBandwidth(b.getBandwidth()+0.1);
            }
        }*/
        System.out.println(counter);

    }
    public static final String TAG = "FNQDAlgorithm";
    private Map<String, Network> candidateNetwork;
    double mParameters[][];
    double weights[];
    double t;

    public FNQDAlgorithmSimpleT(Map<String, Network> candidateNetwork, double[][] mParameters, double[] weights, double t) {
        this.candidateNetwork = candidateNetwork;
        this.mParameters = mParameters;
        this.weights = weights;
        this.t = t;
    }

    @Override
    public void run() {

    }
    public Network fnqdProcess(){
        System.out.println("FNQD处理----------------------------------------------");
        Network optimalNetwork = null;
        /*for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            //System.out.println(entry.getValue().getWifiP2pDevice().deviceName, " / " + entry.getValue().getRssi() +" / "+ entry.getValue().getLoadBalance() + " / " +entry.getValue().getBandwidth() + " / " + entry.getValue().getGroupOwnerPower());
            System.out.println(entry.getKey()+" / " + entry.getValue().getRssi() +" / "+ entry.getValue().getLoadBalance() + " / " +entry.getValue().getBandwidth() + " / " + entry.getValue().getGroupOwnerPower());
        }*/
        double factors[][] = new double[candidateNetwork.size()][mParameters.length];
        int count = 0;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            factors[count++] = entry.getValue().getFactors();
        }
        /*for(int i = 0;i < candidateNetwork.size();i++){
            factors[i] = candidateNetwork.get(i).getFactors();
        }*/

        double pev[] = new double[factors.length];
        //FNQD核心步骤
        for(int i = 0;i < factors.length;i++){
            System.out.println("第 "+ i +" 个候选网络开始处理---------------");
            Matrix u = fuzzification(factors[i], mParameters);
            Matrix nqv = computeNQV(factors[i], mParameters);
            //计算隶属度量化值MQV
            Matrix tMQV = nqv.times(u.transpose());
            double aMQV[] = new double[tMQV.getRowDimension()];
            for(int j = 0;j < tMQV.getRowDimension();j++){
                aMQV[j] = tMQV.get(j,j);
            }
            Matrix mqv = new Matrix(aMQV, aMQV.length);
            //System.out.println("计算MQV：");
            Matrix mWeights = new Matrix(weights, weights.length);
            //计算PEV值
            pev[i] = (mqv.transpose()).times(mWeights).get(0,0);
            //System.out.println("计算PEV："+ pev[i]);
            System.out.println("第 "+ i +" 个候选网络处理结束---------------");

        }
        count = 0;
        double cPEV = 0;
        Network currentNetwrok = null;
        for(Entry<String, Network> entry : candidateNetwork.entrySet()){
            entry.getValue().setPev(pev[count++]);
            if(entry.getValue().isGroupOwner()){
                //找到当前网络
                currentNetwrok = entry.getValue();
                //candidateNetwork.remove(entry.getKey());
                cPEV = currentNetwrok.getPev();
                //System.out.println(TAG, "找到当前网络："+currentNetwrok.getWifiP2pDevice().deviceName);
                System.out.println("找到当前网络："+entry.getKey());
            }
        }
        /*for(int i = 0;i<candidateNetwork.size();i++){
            candidateNetwork.get(i).setPev(pev[i]);
        }*/
        //计算当前网络的PEV
        /*System.out.println("当前网络开始处理---------------------");
        System.out.println("当前网络：", " " + currentNetwrok.getRssi() +" "+ currentNetwrok.getBandwidth() + " " + currentNetwrok.getLoadBalance() + " " + currentNetwrok.getGroupOwnerPower());
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
            //System.out.println(TAG, "最优切换网络："+ optimalNetwork.getWifiP2pDevice().deviceName);
            System.out.println("最优切换网络："+ optimalNetwork.getName());
            //optimalNetwork.setGroupOwner(true);
            //currentNetwrok.setGroupOwner(false);
            double rssi = optimalNetwork.getRssi();
            //optimalNetwork.setRssi(currentNetwrok.getRssi());
            //currentNetwrok.setRssi(rssi);
            counter++;
        }else {
            System.out.println("无最优切换网络");
            double rssi = candidateNetwork.get("a").getRssi();
            //candidateNetwork.get("a").setRssi(candidateNetwork.get("b").getRssi());
            //candidateNetwork.get("b").setRssi(rssi);
        }
        //candidateNetwork.clear();
        System.out.println("FNQD处理完成----------------------------------------------");
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
        //System.out.println("模糊化：" );
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
