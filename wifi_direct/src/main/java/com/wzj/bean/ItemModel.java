package com.wzj.bean;

/**
 * Created by wzj on 2017/5/12.
 */

public class ItemModel {
    public static final int CHAT_A = 1001;
    public static final int CHAT_B = 1002;
    private int type;
    private ChatModel model;

    public ItemModel(int type, ChatModel model) {
        this.type = type;
        this.model = model;
    }

    public int getType() {
        return type;
    }

    public ChatModel getModel() {
        return model;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setModel(ChatModel model) {
        this.model = model;
    }
}
