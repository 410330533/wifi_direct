package com.wzj.bean;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by WZJ on 2018/3/23.
 */
public class Node {
    public static final String TAG = "Node";
    private Map<String, Node> mChildren;
    private Node mParent;
    private Member mData;

    public Node(Map<String, Node> mChildren, Member mData) {
        this.mChildren = mChildren;
        this.mData = mData;
    }

    public Node(Member mData) {
        this.mData = mData;
    }

    //是否为根节点
    public boolean isRoot(){
        return mParent == null;
    }


    //是否为叶节点
    public boolean isLeaf(){
        return mChildren == null;
    }


    public Map<String, Node> getmChildren() {
        return mChildren;
    }

    public void setmChildren(Map<String, Node> mChildren) {
        this.mChildren = mChildren;
    }

    public Node getmParent() {
        return mParent;
    }

    public void setmParent(Node mParent) {
        this.mParent = mParent;
    }

    public Member getmData() {
        return mData;
    }

    public void setmData(Member mData) {
        this.mData = mData;
    }

    //this为父节点，addedNode将添加在this节点下
    public void addNode(Node addedNode){
        if(this.mChildren == null){
            this.mChildren = new HashMap<>();
        }
        String mac = addedNode.getmData().getMacAddress();
        if(mChildren.containsKey(mac) && !mChildren.get(mac).isLeaf()){
            Map<String, Node> tempChildren = mChildren.get(mac).getmChildren();
            this.mChildren.put(addedNode.getmData().getMacAddress(), addedNode);
            addedNode.setmChildren(tempChildren);
        }else {
            this.mChildren.put(addedNode.getmData().getMacAddress(), addedNode);
        }
        addedNode.setmParent(this);

    }

    public void addNodes(Node[] nodes){
        if(this.mChildren == null){
            this.mChildren = new HashMap<>();
        }
        for(int i = 0; i < nodes.length; i++){
            this.mChildren.put(nodes[i].getmData().getMacAddress(), nodes[i]);
            nodes[i].setmParent(this);
        }
    }


    public void removeNode(Node removedNode, List<String> removedMembers, boolean flag){

        if(flag){
            if(!removedNode.isRoot()){
                Node parent = removedNode.getmParent();
                Iterator<Entry<String, Node>> i = parent.getmChildren().entrySet().iterator();

                while (i.hasNext()){
                    Entry<String, Node> entry = i.next();
                    if(entry.getValue().getmData().getMacAddress().equals(removedNode.getmData().getMacAddress())){
                        removedMembers.add(entry.getValue().getmData().getMacAddress());
                        i.remove();
                        break;
                    }
                }

            }else {

            }
        }

        if(removedNode.isLeaf()){
            //Log.d(TAG, "删除叶节点");
            Map<String, Node> children = removedNode.getmParent().getmChildren();

            if(children.containsKey(removedNode.getmData().getMacAddress())){
                children.remove(children.get(removedNode.getmData().getMacAddress()));
                removedMembers.add(removedNode.getmData().getMacAddress());
                //Log.d(TAG, "删除叶节点成功！");
                return;
            }

        }else {
            //Log.d(TAG, "删除非叶节点");
            Map<String, Node> children = removedNode.getmChildren();
            Iterator<Entry<String, Node>> iterator = children.entrySet().iterator();

            while (iterator.hasNext()){
                Entry<String, Node> entry = iterator.next();
                Node node = entry.getValue();
                if(!entry.getValue().isLeaf()){
                    this.removeNode(node, removedMembers, false);
                    removedMembers.add(node.getmData().getMacAddress());
                    iterator.remove();
                }else {
                    removedMembers.add(node.getmData().getMacAddress());
                    iterator.remove();
                }

            }


        }
    }

    public void displayNodes(Node node){
        System.out.println(node.getmData().getMacAddress());
        Map<String, Node> children = node.getmChildren();
        if(children != null && children.size() != 0){
            for (Entry<String, Node> entry : children.entrySet()){
                displayNodes(entry.getValue());
            }
        }
    }

    public Node findNode(String macAddress){
        if(this.getmData().getMacAddress().equals(macAddress)){
            return this;
        }

        Node node = null;
        Map<String, Node> children = this.getmChildren();
        if(children != null && children.size() != 0){
            for (Entry<String, Node> entry : children.entrySet()){
                Node n = entry.getValue();
                if(n.getmData().getMacAddress().equals(macAddress)){
                    return n;
                }
                node = n.findNode(macAddress);
                if(node != null){
                    return node;
                }

            }
        }
        return node;
    }

    public Map<String, Member> getCurrentGroupMemberMap(){
        Map<String, Member> currentGroupMemberMap = new HashMap<>();
        currentGroupMemberMap.put(this.getmData().getMacAddress(), this.getmData());  //添加自己（GO）
        Map<String, Node> nodeMap = this.getmChildren();  //this为根节点
        for(Node node : nodeMap.values()){
            currentGroupMemberMap.put(node.getmData().getMacAddress(), node.getmData());
        }
        return currentGroupMemberMap;
    }

    public void clear(){
        Log.d(TAG, "MemberNode清除了！！！！！！");
        Map<String, Node> children = this.getmChildren();
        Iterator<Entry<String, Node>> iterator = children.entrySet().iterator();
        while (iterator.hasNext()){
            Node node = iterator.next().getValue();

            if(!node.isLeaf()){
                node.clear();
                iterator.remove();
            }else {
                iterator.remove();
            }

        }
    }
    public static void main(String[] args) {
        Node node = new Node(new HashMap<String, Node>(), new Member("root", "root", "root"));

        Node n1 = new Node(new Member(""+1, ""+1, ""+1));
        Node n2 = new Node(new Member(""+2, ""+2, ""+2));
        Node n3 = new Node(new Member(""+3, ""+3, ""+3));
        Node n4 = new Node(new Member(""+4, ""+4, ""+4));
        Node n5 = new Node(new Member(""+5, ""+5, ""+5));
        Node n6 = new Node(new Member(""+6, ""+6, ""+6));
        Node n7 = new Node(new Member(""+7, ""+7, ""+7));
        Node n8 = new Node(new Member(""+8, ""+8, ""+8));
        Node n9 = new Node(new Member(""+9, ""+9, ""+9));

        node.addNode(n1);
        node.addNode(n2);
        node.addNode(n3);

        n3.addNode(n4);
        n3.addNode(n5);
        n3.addNode(n6);

        n6.addNode(n7);

        n7.addNode(n8);

        n8.addNode(n9);

        Node a = node.findNode("6");
        Node n0 = new Node(new Member(""+3, ""+3, ""+3));
        node.addNode(n0);
        node.displayNodes(node);

        List<String> removedList = new ArrayList<>();

        node.removeNode(a, removedList, true);
        //node.clear();
        System.out.println("------------ "+removedList.size());
        node.displayNodes(node);



    }
}
/*
public class Node {
    public static final String TAG = "Node";
    private List<Node> mChildren;
    private Node mParent;
    private Member mData;

    public Node(List<Node> mChildren, Node mParent, Member mData) {
        this.mChildren = mChildren;
        this.mParent = mParent;
        this.mData = mData;
    }

    public Node(Member mData) {
        this.mData = mData;
    }

    //是否为根节点
    public boolean isRoot(){
        return mParent == null;
    }


    //是否为叶节点
    public boolean isLeaf(){
        return mChildren == null;
    }


    public List<Node> getmChildren() {
        return mChildren;
    }

    public void setmChildren(List<Node> mChildren) {
        this.mChildren = mChildren;
    }

    public Node getmParent() {
        return mParent;
    }

    public void setmParent(Node mParent) {
        this.mParent = mParent;
    }

    public Member getmData() {
        return mData;
    }

    public void setmData(Member mData) {
        this.mData = mData;
    }

    //this为父节点，addedNode将添加在this节点下
    public void addNode(Node addedNode){
        if(this.mChildren == null){
            this.mChildren = new ArrayList<>();
        }
        this.mChildren.add(addedNode);
        addedNode.setmParent(this);

    }

    public void addNodes(Node[] nodes){
        if(this.mChildren == null){
            this.mChildren = new ArrayList<>();
        }
        for(int i = 0; i < nodes.length; i++){
            this.mChildren.add(nodes[i]);
            nodes[i].setmParent(this);
        }
    }


    public void removeNode(Node removedNode, List<String> removedMembers, boolean flag){

        if(flag){
            if(!removedNode.isRoot()){
                Node parent = removedNode.getmParent();
                Iterator i = parent.getmChildren().iterator();
                while (i.hasNext()){
                    Node n = (Node) i.next();
                    if(n.getmData().getMacAddress().equals(removedNode.getmData().getMacAddress())){
                        i.remove();
                        removedMembers.add(n.getmData().getMacAddress());
                        break;
                    }
                }
            }else {

            }
        }

        if(removedNode.isLeaf()){
            //Log.d(TAG, "删除叶节点");
            List<Node> children = removedNode.getmParent().getmChildren();
            for(Node node : children){
                if(node.getmData().equals(removedNode.getmData().getMacAddress())){
                    children.remove(node);
                    removedMembers.add(node.getmData().getMacAddress());
                    //Log.d(TAG, "删除叶节点成功！");
                    return;
                }
            }
        }else {
            //Log.d(TAG, "删除非叶节点");
            List<Node> children = removedNode.getmChildren();
            Iterator iterator = children.iterator();

            while (iterator.hasNext()){
                Node node = (Node) iterator.next();
                if(!node.isLeaf()){
                    this.removeNode(node, removedMembers, false);
                    removedMembers.add(node.getmData().getMacAddress());
                    iterator.remove();
                }else {
                    removedMembers.add(node.getmData().getMacAddress());
                    iterator.remove();
                }

            }


        }
    }

    public void displayNodes(Node node){
        System.out.println(node.getmData().getMacAddress());
        List<Node> nodeList = node.getmChildren();
        if(nodeList != null && nodeList.size() != 0){
            for (Node n : nodeList){
                displayNodes(n);
            }
        }
    }

    public Node findNode(String macAddress){
        if(this.getmData().getMacAddress().equals(macAddress)){
            return this;
        }

        Node node = null;
        List<Node> nodeList = this.getmChildren();
        if(nodeList != null && nodeList.size() != 0){
            for (Node n : nodeList){
                if(n.getmData().getMacAddress().equals(macAddress)){
                    return n;
                }
                node = n.findNode(macAddress);
                if(node != null){
                    return node;
                }

            }
        }
        return node;
    }
    public static void main(String[] args) {
        Node node = new Node(new ArrayList<Node>(), null, new Member("root", "root", "root"));

        Node n1 = new Node(new Member(""+1, ""+1, ""+1));
        Node n2 = new Node(new Member(""+2, ""+2, ""+2));
        Node n3 = new Node(new Member(""+3, ""+3, ""+3));
        Node n4 = new Node(new Member(""+4, ""+4, ""+4));
        Node n5 = new Node(new Member(""+5, ""+5, ""+5));
        Node n6 = new Node(new Member(""+6, ""+6, ""+6));
        Node n7 = new Node(new Member(""+7, ""+7, ""+7));
        Node n8 = new Node(new Member(""+8, ""+8, ""+8));
        Node n9 = new Node(new Member(""+9, ""+9, ""+9));

        node.addNode(n1);
        node.addNode(n2);
        node.addNode(n3);

        n3.addNode(n4);
        n3.addNode(n5);
        n3.addNode(n6);

        n6.addNode(n7);

        n7.addNode(n8);

        n8.addNode(n9);

        Node a = node.findNode("3");


        node.displayNodes(node);

        List<String> removedList = new ArrayList<>();

        node.removeNode(a, removedList, true);
        System.out.println("------------ "+removedList.size());
        node.displayNodes(node);



    }
}*/
