package com.example.smartcontrol.BLE;
import java.util.UUID;

        import android.app.Service;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCallback;
        import android.bluetooth.BluetoothGattCharacteristic;
        import android.bluetooth.BluetoothGattDescriptor;
        import android.bluetooth.BluetoothGattServer;
        import android.bluetooth.BluetoothGattService;
        import android.bluetooth.BluetoothManager;
        import android.bluetooth.BluetoothProfile;
        import android.content.Context;
        import android.content.Intent;
        import android.net.NetworkInfo.State;
        import android.os.Binder;
        import android.os.IBinder;
        import android.util.Log;

import com.example.smartcontrol.Util.Utils;

public class BLEService extends Service {

    public final static String ACTION_DATA_CHANGE_STRING = "";
    public final static String ACTION_STATE_CONNECTED = "com.qixiang.bleskip_teacher.ACTION_STATE_CONNECTED";
    public final static String ACTION_STATE_DISCONNECTED = "com.qixiang.bleskip_teacher.ACTION_STATE_DISCONNECTED";
    public final static String ACTION_SERVICESDISCOVERED_OVER = "com.qixiang.bleskip_teacher.ACTION_SERVICESDISCOVERED_OVER";
    public final static String ACTION_WRITE_DESCRIPTOR_OVER = "com.qixiang.bleskip_teacher.ACTION_WRITE_DESCRIPTOR_OVER";

    public final static String ACTION_WRITE_DATA_OVER = "com.qixiang.bleskip_teacher.ACTION_WRITE_DATA_OVER";
    public final static String ACTION_CHARACTER_CHANGE="com.qixiang.bleskip_teacher.ACTION_CHARACTER_CHANGE";

    //跳绳_老师端
    UUID uuid_s=UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    UUID uuid_c=UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    UUID uuid_w=UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    UUID uuid_d=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGatt mBluetoothGatt;

    public BluetoothGattService service;
    public BluetoothGattCharacteristic characterNotify,characterWrite1,characterWrite2;

    private boolean connected_flag = false;
    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    public class LocalBinder extends Binder{

        public BLEService getService() {
            return BLEService.this;
        }
    }
    //通知数据接口
    public interface OnDataAvailableListener {
        public void OnDataAvailable(BluetoothGatt gatt, BluetoothGattCharacteristic character) ;
    }

    private OnDataAvailableListener onDataAvailableListener;
    public void setOnDataAvailable(OnDataAvailableListener l) {
        onDataAvailableListener = l;
    }
    //写回调接口
    public interface OnWriteOverCallback {
        public void OnWriteOver(BluetoothGatt gatt, BluetoothGattCharacteristic character, int statue);
    }
    private OnWriteOverCallback onWriteOverCallback;
    public void setOnWriteOverCallback(OnWriteOverCallback l) {
        onWriteOverCallback =l;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if(gatt == null)
                Utils.LogE("gatt ========================================= null");
            super.onConnectionStateChange(gatt, status, newState);


            //Tools.setLog("log1", "onConnectionStateChange.........."+newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected_flag = true;
                mBluetoothGatt.discoverServices();
                broadcastUpdate(ACTION_STATE_CONNECTED);
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){

                Log.e(Utils.TAG,"BluetoothProfile.STATE_DISCONNECTED..............");

                Tools.connectedFlag = false;
                connected_flag = false;
                broadcastUpdate(ACTION_STATE_DISCONNECTED);
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //Log.e("falter","status:::::::::::::::::::::::::::::::"+status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                service = mBluetoothGatt.getService(uuid_s);
                broadcastUpdate(ACTION_SERVICESDISCOVERED_OVER);

                if (service != null) {
                    characterNotify = service.getCharacteristic(uuid_c);
                    characterWrite1 = service.getCharacteristic(uuid_w);

                    if(characterNotify != null){
                        mBluetoothGatt.setCharacteristicNotification(characterNotify, true);
                        BluetoothGattDescriptor descriptor = characterNotify.getDescriptor(uuid_d);
                        if(descriptor != null){
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mBluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        };
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            //Tools.setLog("log1", "onCharacteristicWrite............................status."+status);

            broadcastUpdate(ACTION_WRITE_DATA_OVER, status);
            if(onWriteOverCallback != null)
                onWriteOverCallback.OnWriteOver(gatt, characteristic, status);
        };
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Tools.connectedFlag = true;
            broadcastUpdate(ACTION_WRITE_DESCRIPTOR_OVER, status);

        };
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Tools.connectedFlag = true;
            broadcastUpdate(ACTION_CHARACTER_CHANGE, characteristic.getValue());
//			if (onDataAvailableListener != null) {
//				onDataAvailableListener.OnDataAvailable(gatt, characteristic);
//			}
        };
    };


    //初始BLe
    public boolean initBle() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == mBluetoothManager) {
            return false;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (null == mBluetoothAdapter) {
            return false;
        }
        return true;
    }

    public void scanBle(BluetoothAdapter.LeScanCallback callback) {
        mBluetoothAdapter.startLeScan(callback);
    }

    public void stopscanBle(BluetoothAdapter.LeScanCallback callback){
        mBluetoothAdapter.stopLeScan(callback);
    }

    public boolean connectBle(BluetoothDevice device) {
        disConnectedBle();
        BluetoothDevice  device_tmp = mBluetoothAdapter.getRemoteDevice(device.getAddress());
        if (device_tmp == null) {
            return false;
        }
        mBluetoothGatt = device_tmp.connectGatt(getApplicationContext(), false, mGattCallback);
        return true;
    }
    public void disConnectedBle() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            connected_flag = false;
        }
    }
    //发送广播消息1
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);

    }
    //发送广播消息2
    private void broadcastUpdate(final String action,int value) {
        final Intent intent = new Intent(action);
        intent.putExtra("value", value);
        sendBroadcast(intent);
    }
    //发送广播消息3
    private void broadcastUpdate(final String action,byte[] value) {

        final Intent intent = new Intent(action);
        intent.putExtra("value", value);
        sendBroadcast(intent);

    }
}
