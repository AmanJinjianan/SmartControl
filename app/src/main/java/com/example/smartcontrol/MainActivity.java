package com.example.smartcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartcontrol.BLE.BLEService;
import com.example.smartcontrol.BLE.MyListener;
import com.example.smartcontrol.BLE.SendBle;
import com.example.smartcontrol.BLE.Tools;
import com.example.smartcontrol.Util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener{

    public SeekBar theSeek;
    private final static int REQUEST_ENABLE_BT=2001;
    private BluetoothDevice theDevice;
    private List<String> fdArrayList = new ArrayList<String>();
    private boolean connected_flag;
    private boolean exit_activity = false;
    public String tmp,hex;
    public boolean reveiveFlag = false;

    public  byte theRandowData = 0;
    public  byte[] theTwoByte = new byte[]{0x00,0x00};

    //保存下位机设备ID
    public byte theOneByte=0;

    private BLEService.OnWriteOverCallback mOnWriteOverCallback = new BLEService.OnWriteOverCallback(){

        @Override
        public void OnWriteOver(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int statue) {

            if(statue == BluetoothGatt.GATT_SUCCESS){
                Utils.LogE("OnWriteOver..............");
                if(fdArrayList.size()!= 0){
                    fdArrayList.remove(0);
                }
            }else if(statue == BluetoothGatt.GATT_FAILURE){
                myHandler.sendEmptyMessage(8);
            }
        }

    };
    public static String byteArrayToStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        String str = new String(byteArray);
        return str;
    }
    public  String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null){
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    String data = "",theReceiveData;

    int ppCount = 0;
    String remainString = "";

    String filePath = "/sdcard/Test110/";
    String fileName = "log.txt";
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!reveiveFlag)
                return;

            if(identifySysID(scanRecord)){
                String ddString = Utils.toHexString(scanRecord);
                if (remainString.equals(ddString)){
                    return;
                }
                remainString  = ddString;

                Tools.setLog("log1", "系统值匹配成功..........LLL:"+recycleCount+"\n       ddString:"+ddString+"   remainString:"+remainString);
                theReamainDataString = "系统值匹配成功..........LLL:"+recycleCount;
                myHandler.sendEmptyMessage(131415);


                if(identifyRanData(scanRecord)){//匹配校验发送的随机数
                    Tools.setLog("log1", "随机数匹配成功..........:"+recycleCount);
                    theReamainDataString = "随机数匹配成功..........:"+recycleCount;
                    myHandler.sendEmptyMessage(131415);
                    if(recycleCount ==1){//在第一个“发收周期”
                        theOneByte =scanRecord[13];
                        theReceiveData = bytesToHexFun3(scanRecord);
                        myHandler.sendEmptyMessage(13141);
                    }
                }
            }
            if(null == device.getName())
            {
            }else{
                Message m = new Message();
                Bundle b = new Bundle();
                m.what = 1234321;
                b.putString("name",device.getName());
                m.setData(b);
                myHandler.sendMessage(m);
                Tools.setLog("log1", device.getName()+"被发现..........");
            }
        }
    };

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub

            BLEService.LocalBinder binder = (BLEService.LocalBinder)service;
            Tools.mBleService = binder.getService();
            if (Tools.mBleService.initBle()) {
                if(!Tools.mBleService.mBluetoothAdapter.isEnabled()){
                    final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }else {
                    myHandler.sendEmptyMessage(11);
                    //scanBle();
                }
            }
        }
    };
    private Handler myHandler2 = new Handler(){
        public void handleMessage(android.os.Message msg) {
            Utils.LogE("myHandler2 dataTwoByte:   "+Utils.toHexString(dataTwoByte));
        }};
    //代表在第几个“发收周期”，初始为"1"（一发一收代表一个周期）
    int recycleCount=1;
    String theReamainDataString = "";
    @SuppressLint("HandlerLeak")
    private Handler myHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            //Tools.setLog("log1", "myHandler.........."+msg.what);
            switch (msg.what) {
                case 13141:
                    Tools.setLog("log1", "..........................................................................13141");
                    //进行周期递增
                    recycleCount++;
                    if(recycleCount >= 2){
                        //scrollToBottom("连接完成！" + recycleCount+" "+Utils.toHexString(new byte[]{theOneByte}));
                        reveiveFlag = false;
                        Toast.makeText(MainActivity.this, "连上了..."+Utils.toHexString(new byte[]{theOneByte}), Toast.LENGTH_LONG).show();
                        maxSendData("0000000000000000",(byte)0x01);
                        myHandler.sendEmptyMessage(122);
                        btn_once.setEnabled(true);
                        setFourBtnEnable(true);
                    }else {
                        //scrollToBottom("第"+recycleCount+"个周期");
                        maxSendData("0000000000000000",(byte)0xFF);
                    }
                    break;
                case 131415:
                    //scrollToBottom(theReamainDataString);
                    break;
                case 1234321:
                    Bundle d =msg.getData();
                    //scrollToBottom(d.get("name").toString());
                    break;
                case 110:
                    Toast.makeText(MainActivity.this, "广播成功（一次）............", Toast.LENGTH_SHORT).show();
                    break;
                case 112:
                    Toast.makeText(MainActivity.this, "广播失败............", Toast.LENGTH_SHORT).show();
                    break;
                case 1110:
                    Toast.makeText(MainActivity.this, "持续广播成功............", Toast.LENGTH_SHORT).show();
                    break;
                case 11120:
                    Toast.makeText(MainActivity.this, "停止广播成功.............", Toast.LENGTH_SHORT).show();
                    break;
                case 11121:
                    Toast.makeText(MainActivity.this, "停止广播失败.............", Toast.LENGTH_SHORT).show();
                    break;
                case 10:
                    bindService(new Intent(MainActivity.this,BLEService.class), connection, Context.BIND_AUTO_CREATE);
                    break;
                case 11:
                    if(Tools.mBleService != null)
                        Tools.mBleService.scanBle(mLeScanCallback);
                    //Toast.makeText(MainActivity.this, "0000810000000000", Toast.LENGTH_LONG).show();
                    //maxSendData("0000810000000000",(byte)0xFF);
                    break;
                case 1313:
                    Toast.makeText(MainActivity.this, "连接成功:"+data+" ", Toast.LENGTH_LONG).show();
                    break;
                case 1314:
                    ppCount++;
                    //thetr.setText("sleep100ms 共:"+ppCount+"次");
                    //Toast.makeText(BleConnectActivity.this, "连接成功:"+data+" ", Toast.LENGTH_LONG).show();
                    break;
                case 1315:
                    ppCount++;
                    //thetr.setText("Setting 100ms 共:"+ppCount+"次");
                    //Toast.makeText(BleConnectActivity.this, "连接成功:"+data+" ", Toast.LENGTH_LONG).show();
                    break;
                case 12:
                    if(Tools.mBleService != null){
                        Tools.mBleService.stopscanBle(mLeScanCallback);
                        Tools.mBleService.setOnWriteOverCallback(mOnWriteOverCallback);
                    }
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                    Intent intent2 = new Intent("com.qixiang.blesuccess");
                    sendBroadcast(intent2);

                    Intent intent = new Intent(MainActivity.this,MainActivity.class);
                    startActivity(intent);
                    break;
                case 122:
                    if(Tools.mBleService != null){
                        Tools.mBleService.stopscanBle(mLeScanCallback);
                    }
                    break;
                case 13:
                    new MyConnectedThread().start();
                    break;
                case 14:
                    Bundle bundle=msg.getData();
                    String string =(String) bundle.get("Key");
                    Tools.setLog("log1", "msg.what 14:"+string);
                    Tools.mBleService.characterWrite1.setValue(hexToBytes(string));
                    Tools.mBleService.mBluetoothGatt.writeCharacteristic(Tools.mBleService.characterWrite1);
                    break;
                case 456:
                    maxSendData("00001A0000000000",(byte)0x01);
                    break;
                case 457:
                    maxSendData("00002A0000000000",(byte)0x02);
                    break;
                case 458:
                    maxSendData("00003A0000000000",(byte)0x03);
                    break;
                case 459:
                    maxSendData("00004A0000000000",(byte)0x04);
                    break;
                case 4600:
                    Utils.LogE("dataTwoByte:   "+Utils.toHexString(dataTwoByte));
                    //maxSendData("0000"+Utils.toHexString(dataTwoByte)+"00000000",(byte)0x04);
                    break;
                default:
                    break;
            }
        };
    };

    private void setFourBtnEnable(boolean isAble) {
        btn_up.setEnabled(isAble);
        btn_down.setEnabled(isAble);
        btn_left.setEnabled(isAble);
        btn_right.setEnabled(isAble);
    }


    Animation myAnimation;
    private SendBle mSendBle;
    TextView tv_id,mTextStatus;
    EditText et_SysID,et_SysID2,et_data;
    ScrollView mScrollView;
    Button btn_once,btn_more,btn_stop,btn_up,btn_down,btn_left,btn_right;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreen();
        setContentView(R.layout.layout_control);
        checkBluetoothPermission();

        reveiveFlag = false;

        btn_left = findViewById(R.id.btn_left2598);
        btn_right = findViewById(R.id.btn_right2598);
       
        btn_right.setOnTouchListener(MyTai);
        btn_left.setOnTouchListener(MyTai);

        theSeek = findViewById(R.id.seekBar3);
        theSeek.setProgressDrawable(null);
        theSeek.setProgress(50);
        theSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Utils.LogE("progress:"+progress);
               if(progress>50){
                   dataTwoByte[0] = 0x1A;
                   if(timer12 == null){
                       timer12=new Timer();
                       mt12 = new MyTimerTask();
                       mt12.MyFlag = 1;
                       timer12.schedule(mt12,0,200);
                   }
               }else if(progress<50){
                   dataTwoByte[0] = 0x2A;
                   if(timer12 == null){
                       timer12=new Timer();
                       mt12 = new MyTimerTask();
                       mt12.MyFlag = 2;
                       timer12.schedule(mt12,0,200);
                   }
               }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Utils.LogE("progress:  onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Utils.LogE("progress:  onStopTrackingTouch");
                theSeek.setProgress(50);
                if(timer12 != null){
                    dataTwoByte[0] = 0;
                    timer12.cancel();
                    stopSendData();
                    timer12 = null;
                }

            }
        });
    }
    void setFullScreen(){
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        // 定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        // 获得当前窗体对象
        Window window = MainActivity.this.getWindow();
        // 设置当前窗体为全屏显示
        window.setFlags(flag, flag);
    }
    //校验系统ID
    public boolean identifySysID(byte[] data){
        byte[] byteData = toBytes("0201050319C1030716");

        for(int i=0;i<byteData.length;i++){
            if(!(byteData[i] == data[i]))
                return false;
        }
        return true;
    }

    public boolean identifyRanData(byte[] data){

        if(data.length<15){
            Toast.makeText(MainActivity.this, "返回参数异常了", Toast.LENGTH_SHORT).show();
            return false;
        }


        int randomValue = Integer.valueOf(data[10] >> 2);
        Tools.setLog("log1", "randomValue..........:"+randomValue  +"  oRandowData:"+oRandowData);
        if(randomValue == oRandowData || (oRandowData-randomValue == 64))//如果相等或互补
            return true;
        else
            return false;

    }
    public static String bytesToHexFun3(byte[] bytes) {
        byte[] iData = new byte[6];
        for (int i= 0;i<6;i++){
            iData[i] = bytes[i+9];
        }

        StringBuilder buf = new StringBuilder(iData.length * 2);
        for(byte b : iData) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }

        return buf.toString();
    }

    private void InitBle(){
        BluetoothManager bManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bAdapter = bManager.getAdapter();
        if(bAdapter == null){
            Toast.makeText(MainActivity.this, "not support", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (bAdapter.isEnabled()) {
            setBroadcastReveiver();
            myHandler.sendEmptyMessage(10);
            //bindService(new Intent(this,MainActivity.class), connection, Context.BIND_AUTO_CREATE);
        }else {
            bAdapter.enable();
        }
        //setBroadcastReveiver();
    }

    /**
     * 将16进制字符串转换为byte[]
     *
     * @param str
     * @return
     */
    public  byte[] toBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }

        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }

        return bytes;
    }

    ArrayList<String> unPermissionList;
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 23) {

            String[] mPermission = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
            unPermissionList = new ArrayList<String>();
            try {
                for (int i = 0; i < mPermission.length; i++) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, mPermission[i]) != PackageManager.PERMISSION_GRANTED) {
                        unPermissionList.add(mPermission[i]);
                    }
                }
                for (int i = 0; i < mPermission.length; i++) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                        if (isLocationEnabled()) {

                        }else {
                            Toast.makeText(MainActivity.this, "Please open the location switch.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 0);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Exception0:"+e.toString(), Toast.LENGTH_LONG).show();
            }

        }else {
            InitBle();
            Toast.makeText(MainActivity.this, "Init.............:", Toast.LENGTH_LONG).show();
            return;
        }
        if (unPermissionList.isEmpty()) {
            if (isLocationEnabled())
                InitBle();
            //Toast.makeText(BleConnectActivity.this, "Init.............:", Toast.LENGTH_LONG).show();
            //都授权了。。。
        }else {
            try {
                String[] permissionStrings = unPermissionList.toArray(new String[unPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissionStrings, 1);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Exception11:"+e.toString(), Toast.LENGTH_LONG).show();
            }
        }

    }
    /**
     * 判断定位服务是否开启
     *
     * @param
     * @return true 表示开启
     */

    public boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Toast.makeText(TestAct.this, "requestCode:"+requestCode, Toast.LENGTH_LONG).show();
        for (int i = 0; i < grantResults.length; i++) {
            //Toast.makeText(ScanActivity.this, permissions[i]+"     "+grantResults[i], Toast.LENGTH_LONG).show();

            if (permissions[i].equals("android.permission.ACCESS_COARSE_LOCATION")) {
                if ((grantResults[i] ==0)) {
                    if(!isLocationEnabled()){
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 0);
                    }else {
                        InitBle();
                        Toast.makeText(MainActivity.this, "Init.............:", Toast.LENGTH_LONG).show();
                    }
                }else {
                    finish();
                }

            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0){
            if (isLocationEnabled()) {
                checkBluetoothPermission();
            }else {
                Toast.makeText(MainActivity.this, "Please open the location switch.", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                }, 1000);
            }
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }
    //设置广播接收器
    private void setBroadcastReveiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_STATE_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_STATE_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_WRITE_DESCRIPTOR_OVER);
        //intentFilter.addAction(BLEService.ACTION_CHARACTER_CHANGE);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        bluetoothReceiver = new BluetoothReceiver();
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    private BluetoothReceiver bluetoothReceiver = null;




    public MyListener listener = new MyListener() {
        @Override
        public void readData(byte[] var1, String var2) {

        }

        @Override
        public void onSendStatus(Boolean var1) {
            Tools.setLog("log1", "..........................onSendStatus......");
       /* if (var1)
            Toast.makeText(MainActivity.this, "...........成功...............", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "...........失败...............", Toast.LENGTH_SHORT).show();*/
        }
    };


    public class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();

            if(BLEService.ACTION_CHARACTER_CHANGE.equals(action)){
                //tmp_byte = characteristic.getValue();
                byte[] tmp_byte = intent.getByteArrayExtra("value");
                tmp = "";
                for (int i = 0; i < tmp_byte.length; i++) {
                    hex = Integer.toHexString(tmp_byte[i] & 0xFF);
                    if (hex.length() == 1) {
                        hex = '0' + hex;
                    }
                    tmp = tmp + hex;
                }
            }else if(BLEService.ACTION_STATE_CONNECTED.equals(action)){

            }else if(BLEService.ACTION_STATE_DISCONNECTED.equals(action)){
                connected_flag = false;
                myHandler.sendEmptyMessage(11);
            }else if (BLEService.ACTION_WRITE_DESCRIPTOR_OVER.equals(action)) {
                connected_flag = true;
                theDevice = null;
                myHandler.sendEmptyMessage(12);
            }else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch(blueState){
                    case BluetoothAdapter.STATE_ON:
                        //开始扫描
                        bindService(new Intent(MainActivity.this,MainActivity.class), connection, Context.BIND_AUTO_CREATE);
                        break;
                }

            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        exit_activity = true;
        //unbindService(connection);
    }

    public byte[] hexToBytes(String hexString) {
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

    public class MyConnectedThread extends Thread{

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();

            try {
                while (true) {
                    connected_flag = false;
                    if (exit_activity) return;
                    Tools.setLog("log1", "connectBle..........");
                    Tools.mBleService.connectBle(theDevice);

                    for(int j=0;j<50;j++){

                        if (connected_flag) {
                            break;
                        }
                        sleep(100);
                    }

                    if(connected_flag)
                        break;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    private String sendstr="";
    private String sss1="";
    private byte[] command_ = new byte[16];
    int data1 = 0;
    int data2 = 0;
    int data3 = 0;
    int data4 = 0;
    int data5 = 0;
    int data6 = 0;
    int data7 = 0;
    int data8 = 0;
    int data9 = 0;
    int data10 = 0;
    int data11 = 0;
    int data12 = 0;
    int data13 = 0;
    int data14 = 0;
    int data15 = 0;
    int data16 = 0;
    /**
     * 初始化操作指令
     */
    private void initData() {
        command_[0] = (byte) data16;
        command_[1] = (byte) data15;
        command_[2] = (byte) data14;
        command_[3] = (byte) data13;
        command_[4] = (byte) data12;
        command_[5] = (byte) data11;
        command_[6] = (byte) data10;
        command_[7] = (byte) data9;
        command_[8] = (byte) data8;
        command_[9] = (byte) data7;
        command_[10] = (byte) data6;
        command_[11] = (byte) data5;
        command_[12] = (byte) data4;
        command_[13] = (byte) data3;
        command_[14] = (byte) data2;
        command_[15] = (byte) data1;
        sss1 = bytesToHexString(command_);//需要发送的字节需反向拼接
        sendstr= sss1.substring(0,8)+"-"+sss1.substring(8,12)+"-"+sss1.substring(12,16)+"-"+sss1.substring(16,20)+"-"+sss1.substring(20,32);//"01020304-0506-0708-1112-131415161718"
    }
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    StringBuilder sb=  new StringBuilder();
    int switchFlag=0;
    @Override
    public void onClick(View v) {
        switch (v.getId()){
//            case R.id.btn_once:
//                switchFlag++;
//                if(switchFlag%2 == 0){
//                    myHandler.sendEmptyMessage(11);
//                    Toast.makeText(MainActivity.this,"收" ,Toast.LENGTH_SHORT).show();
//                }
//                else{
//                    myHandler.sendEmptyMessage(122);
//                    Toast.makeText(MainActivity.this,"停" ,Toast.LENGTH_SHORT).show();
//                }
//                break;
            case R.id.btn_more2598:
                reveiveFlag = true;
                //initData();
                //getIDValue();
                //String data = et_data.getText().toString();
                //data="0d2be7f7-e6d6-4f48-a4bc-e521f9fd8eff";
                maxSendData("0000810000000000",(byte)0xFF);
                break;
            case R.id.btn_left2598:
                maxSendData("00003A0000000000",(byte)0x03);
                break;
            case R.id.btn_right2598:
                maxSendData("00004A0000000000",(byte)0x04);
                break;
        }
    }
    class MyTimerTask extends TimerTask {
        public int MyFlag = 0;
        @Override
        public void run() {
            Utils.LogE(".............................");
            myHandler2.sendEmptyMessage(4600);
//            switch (MyFlag){
//                case 1: myHandler.sendEmptyMessage(456); break;
//                case 2: myHandler.sendEmptyMessage(457); break;
//                case 3: myHandler.sendEmptyMessage(458); break;
//                case 4: myHandler.sendEmptyMessage(459); break;
//            }
        }
    }
    MyTimerTask mt12,mt34;
    Timer timer12,timer34;
    @Override
    public boolean onLongClick(View v) {
        //threadFlag = true;
        return false;
    }

    byte[] dataTwoByte = new byte[2];
    private View.OnTouchListener MyTai = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_CANCEL || event.getAction()==MotionEvent.ACTION_UP){
                Toast.makeText(MainActivity.this, "UP", Toast.LENGTH_SHORT).show();
                if(v.getId() == R.id.btn_left2598 || v.getId() == R.id.btn_right2598){
                    dataTwoByte[1] = 0;
                    if(timer34 != null){
                        timer34.cancel();
                        stopSendData();
                    }
                }
            } else  if(event.getAction()==MotionEvent.ACTION_DOWN){
                Toast.makeText(MainActivity.this, "ACTION_DOWN", Toast.LENGTH_SHORT).show();
                switch (v.getId()){
                    case R.id.btn_left2598:
                        dataTwoByte[1] = 0x3A;
                        timer34=new Timer();
                        mt34 = new MyTimerTask();
                        timer34.schedule(mt34,0,200);
                        break;
                    case R.id.btn_right2598:
                        dataTwoByte[1] = 0x4A;
                        timer34=new Timer();
                        mt34 = new MyTimerTask();
                        timer34.schedule(mt34,0,200);
                        break;
                }
            }
            return false;
        }
    };
    private void stopSendData() {
        Toast.makeText(MainActivity.this, "停22", Toast.LENGTH_SHORT).show();
//        findViewById(R.id.btn_stop).setEnabled(false);
//        findViewById(R.id.btn_more).setEnabled(true);
//        mSendBle.stopSend();
    }
    public void maxSendData(String realData,byte theDirection){
        if(recycleCount != 1)
            stopSendData();
        String sendData = getIDValue(realData,theDirection);
        if("".equals(sendData)){
            Toast.makeText(MainActivity.this, "发送数据异常", Toast.LENGTH_SHORT).show();
        }else {
            mSendBle.startSendMore(sendData, 3,myHandler);
            mSendBle.setListener(listener);
        }
    }

    //得到校验结果（存在输入风险 未严格筛选非法字符）
    private String getIDValue(String data,byte derectionFlag) {
        //非法字符校验
        String theData = validataData(data);
        if("".equals(theData)){
            return "";
        }
        byte[] theByte = toBytes(theData);

        Tools.setLog("log1","recycleCount:::::::::::::::::::::::::::::："+recycleCount+ "  data:"+data);
        switch (recycleCount)
        {
            case 1://代表第一发送
                //得到随机数
                theByte[0] = getSecondData();
                break;
            case 2:
                //第二次发送
                theByte[0] = getSecondData();
                theByte[1] = theOneByte;
                recycleCount++;
                //theByte[2] = 0x1A;
                break;
            default://非配对阶段
                //theByte = getDirectionData(derectionFlag);
                theByte[0] = getSecondData();
                theByte[1] = theOneByte;
                break;
        }
        //将随机数整合到数组中，并显示在view中
        //String theString = byteArrayToHexStr(theByte);

       /* //Tools.setLog("log1","最终出口数据："+theString);
        SpannableStringBuilder style=new SpannableStringBuilder(theString);
        //str代表要显示的全部字符串
        style.setSpan(new ForegroundColorSpan(Color.RED),6,8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //３代表从第几个字符开始变颜色，注意第一个字符序号是０．
        //８代表变色到第几个字符．
        et_data.setText(style);*/

        //颠倒数组顺序
        for (int i=0;i<theByte.length/2;i++){
            byte temp = theByte[i];
            theByte[i] = theByte[7-i];
            theByte[7-i] = temp;
        }
        byte theResult = 0;
        //依次"亦或"九个byte
        for (int i=0;i<theByte.length;i++){
            theResult = (byte)(theByte[i]^theResult);
        }
        theResult = (byte)(theResult^0x5A);
        Utils.LogE(("校验值：\n"+theResult));
        //组织新的数据 附加校验位
        byte[] newByte = new byte[theByte.length+1];
        for (int i=0;i<newByte.length;i++){
            if(i == 0)
                newByte[i] = theResult;
            else
                newByte[i] = theByte[i-1];
        }

        String stringData = bytesToHexString(newByte);
        return formData(stringData);
    }

    private String validataData(String data) {
        String regex="^[A-Fa-f0-9]+$";
        if(!data.matches(regex) || (data.length()%2 !=0)){
            Toast.makeText(MainActivity.this, "字符串不是十六进制字符串", Toast.LENGTH_SHORT).show();
            return "";
        }

        if(data.length() != 16){
            Toast.makeText(MainActivity.this, "数据长度异常..", Toast.LENGTH_SHORT).show();
            return "";
        }
        return data;
    }

    boolean mgFlag= true;
    byte[] df;
    byte windowNum =0;
    int oRandowData=0;
    //6位随机数+2位窗口号
    public byte getSecondData(){
        if(mgFlag){
            mgFlag = false;
            oRandowData = (int)(1+Math.random()*(62));
            df=Utils.intToButeArray(oRandowData);
        }
        if(df != null){
            windowNum++;
            if(windowNum>3)
                windowNum=0;
            return (byte)(df[1]*4 + windowNum);
        }else {
            Toast.makeText(MainActivity.this, "窗口数产生异常了", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }
    /* public String getRandomData(){
         int n = (int)(1+Math.random()*(253));
         String[] hexArray = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

         if (n < 0) {
             n = n + 256;
         }
         int d1 = n / 16;
         int d2 = n % 16;
         return hexArray[d1] + hexArray[d2];
     }*/
    public String formData(String stringData){
        if(stringData.length() != 18)
            Toast.makeText(MainActivity.this, "数据长度异常2..", Toast.LENGTH_SHORT).show();

        return stringData.substring(0,8)+"-"+stringData.substring(8,12)+"-"+stringData.substring(12,16)+"-"+stringData.substring(16);
    }


}

