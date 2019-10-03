package com.example.smartcontrol.BLE;

/**
 * Created by Administrator on 2018/8/3.
 */
import android.util.Log;

public class Tools {

    public static  BLEService mBleService ;

    public static void setLog(String falter,String s){

        Log.e(falter, s);
    }
    public static boolean connectedFlag =false;

    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789ABCDEF";
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            int h = hexDigits.indexOf(hexChars[pos]) << 4;
            int l = hexDigits.indexOf(hexChars[pos + 1]);
            if (h == -1 || l == -1) {
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }
}
