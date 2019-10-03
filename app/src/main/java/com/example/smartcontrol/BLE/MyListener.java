package com.example.smartcontrol.BLE;

/**
 * Created by Administrator on 2019/6/17.
 */
public interface MyListener {
    void readData(byte[] var1, String var2);

    void onSendStatus(Boolean var1);
}
