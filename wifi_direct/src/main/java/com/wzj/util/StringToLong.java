package com.wzj.util;

import java.nio.ByteBuffer;

/**
 * Created by wzj on 2017/4/24.
 */

public class StringToLong {
    public static Long transfer(String str){
        //byte转long
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(str.getBytes());
        byteBuffer.flip();
        return byteBuffer.getLong();
    }
}
