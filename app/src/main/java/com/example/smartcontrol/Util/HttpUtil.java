package com.example.smartcontrol.Util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/8/6.
 */

public class HttpUtil {

    //true代表 脱网模式
    public static Boolean NetState = false;
    public static String sessionid = null;

    //请求的url ，json数据， 服务的类型(获取cookie)
//    public void  netThr(String svrurl, String message,int svrType) {
//        String svraddr = "";
//        svraddr = svrurl.replace(" ", "%20");
//        URL svr = new URL(svraddr);
//        if (message == null)
//            message = "";
//        try {
//            HttpURLConnection httpreq = (HttpURLConnection) svr.openConnection();
//            httpreq.setConnectTimeout(60000);
//            httpreq.setReadTimeout(60000);
//            if (message.length() > 0)
//                httpreq.setRequestMethod("POST");
//            else
//                httpreq.setRequestMethod("GET");
//            httpreq.setDoInput(true);
//
//            if (message.length() > 0) {
//                httpreq.setDoOutput(true);
//                httpreq.setRequestProperty("Charset", "UTF-8");
//                httpreq.setRequestProperty("Content-length", "" + message.getBytes("UTF-8").length);
//                httpreq.setRequestProperty("Content-Type", "application/json");
//            }
//            if (sessionid != null && svrType != HandleRespFun.API_TYPE_GETCOOKIE) {
//                //设置Cookie 到 Headers
//                httpreq.setRequestProperty("Cookie", sessionid);
//            }
//            httpreq.connect();
//            if (message.length() > 0) {
//                OutputStream os = httpreq.getOutputStream();
//                DataOutputStream dos = new DataOutputStream(os);
//                dos.write(message.getBytes("UTF-8"));
//                dos.close();
//            }
//
//            //返回的headers 例子
//            //		Content-Type	application/json
//            //        Set-Cookie	api_authenticated=1
//            //        Set-Cookie	user_authenticated=1
//            //        Set-Cookie	JSESSIONID=16i51l7jwmodpb1u1jfpdk0pg;Path=/laundry
//            //        Expires	Thu, 01 Jan 1970 00:00:00 GMT
//            //        Content-Length	175
//            //        Server	Jetty(9.2.3.v20140905)
//            //        Date	Tue, 27 Jan 2015 02:32:09 GMT
//
//            //获取Cookie
//            if (svrType == HandleRespFun.API_TYPE_GETCOOKIE) {
//                Map<String, List<String>> kk = httpreq.getHeaderFields();
//                List<String> cook = kk.get("Set-Cookie");
//                if (cook != null) {
//                    for (String onecook : cook) {
//                        Log.v("Jashon", "Cookie is:" + onecook);
//                        if (onecook.contains("JSESSIONID")) {
//                            //sessionid=JSESSIONID=16i51l7jwmodpb1u1jfpdk0pg
//                            sessionid = onecook.split(";")[0];
//                        }
//                    }
//                }
//            }
//
//        }
//    }
        public static StringBuffer sb;
    public static void load(Context con,final String path, Map<String,String> params,final OnResponseListner listner) {

        if(!isNetworkAvailable(con)){
            listner.onError("net error");
            return;
        }
        sb =getRequestData(params,"UTF-8");
       /* sb = new StringBuffer();
        if (params!=null && !params.isEmpty()){
            for (Map.Entry<String,String> entry: params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8")).append(entry.getValue()).append("&");
            }
            sb.deleteCharAt(sb.length()-1);
        }*/
        Log.e("falter" ,"Content-Length:"+sb.toString());
        //final String path = "https://www.wangjiayin.cn/us-web-app/login/ls/dhhmlogin";
        new Thread() {
            public void run() {
                try {
                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");
                    conn.setReadTimeout(5000);
                    // Post 请求不能使用缓存
                    //conn.setUseCaches(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //String data = "dhhm=" + phoneNum.getText().toString() + "&yzm=" + phoneCode.getText().toString();
                    conn.setRequestProperty("Content-Length", String.valueOf(sb.length()));


                    if (sessionid != null) {
                        //设置Cookie 到 Headers
                        conn.setRequestProperty("Cookie", sessionid);
                    }
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(sb.toString().getBytes());

                    int code = conn.getResponseCode();
                    Log.e("falter" ,"7878787877:"+code);
                    InputStream hg = conn.getInputStream();
                    String obg = changeInputStream(hg,"UTF-8");
                    //RecoginzeJson(obg);

                    if (code == 200) {
                        //获取Cookie
                        if (true) {
                            Map<String, List<String>> kk = conn.getHeaderFields();
                            List<String> cook = kk.get("Set-Cookie");
                            if (cook != null) {
                                for (String onecook : cook) {
                                    Log.v("Jashon", "Cookie is:" + onecook);
                                    if (onecook.contains("SESSION")) {
                                        //sessionid=JSESSIONID=16i51l7jwmodpb1u1jfpdk0pg
                                        sessionid = onecook.split(";")[0];
                                    }
                                }
                            }
                        }
                        //InputStream is = conn.getInputStream();
                        //String result = StreamTools.ReadStream(is);
                        //Message msg = Message.obtain();
                        listner.onSucess(obg);
                    } else {
                        //Message msg = Message.obtain();
                        //msg.what = ERROR1;
                        //handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    listner.onError("Error");
                    Log.e("falter" ,"5555555555555555555555555:"+e);
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    //Message msg = Message.obtain();
                    //msg.what = ERROR2;
                    //handler.sendMessage(msg);
                }
            }
        }.start();
    }
    /**
     * 将一个输入流转换成指定编码的字符串
     *
     * @param inputStream
     * @param encode
     * @return
     */
    private static String changeInputStream(InputStream inputStream,String encode) {

        // 内存流
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        String result = null;
        if (inputStream != null) {
            try {
                while ((len = inputStream.read(data)) != -1) {
                    byteArrayOutputStream.write(data, 0, len);
                }
                result = new String(byteArrayOutputStream.toByteArray(), encode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    public static  byte[] data;
    public static void submitPostData(final String strUrlPath,Map<String, String> params,final OnResponseListner listner) {

        data = getRequestData(params, "UTF-8").toString().getBytes();//获得请求体

        new Thread(){
            @Override
            public void run() {
                try {
                    //String urlPath = "http://192.168.1.9:80/JJKSms/RecSms.php";
                    URL url = new URL(strUrlPath);

                    HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                    httpURLConnection.setConnectTimeout(3000);     //设置连接超时时间
                    httpURLConnection.setDoInput(true);                  //打开输入流，以便从服务器获取数据
                    httpURLConnection.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
                    httpURLConnection.setRequestMethod("POST");     //设置以Post方式提交数据
                    httpURLConnection.setUseCaches(false);               //使用Post方式不能使用缓存
                    //设置请求体的类型是文本类型
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //设置请求体的长度
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
                    //获得输出流，向服务器写入数据
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(data);

                    int response = httpURLConnection.getResponseCode();            //获得服务器的响应码
                    if(response == HttpURLConnection.HTTP_OK) {
                        InputStream inptStream = httpURLConnection.getInputStream();
                        listner.onSucess(dealResponseResult(inptStream));                     //处理服务器的响应结果
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                   // return "err: " + e.getMessage().toString();
                }
            }
        }.start();

       // return "-1";
    }

    /*
         * Function  :   封装请求体信息
         * Param     :   params请求体内容，encode编码格式
         */
    public static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
        try {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    /*
     * Function  :   处理服务器的响应结果（将输入流转化成字符串）
     * Param     :   inputStream服务器的响应输入流
     */
    public static String dealResponseResult(InputStream inputStream) {
        String resultData = null;      //存储处理结果
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }

    public interface OnResponseListner {
        void onSucess(String response);
        void onError(String error);
    }
    private static void RecoginzeJson(String JSON){
        try {
            JSONTokener jsonParser = new JSONTokener(JSON);
            // 此时还未读取任何json文本，直接读取就是一个JSONObject对象。
            // 如果此时的读取位置在"name" : 了，那么nextValue就是"yuanzhifei89"（String）
            JSONObject person = (JSONObject) jsonParser.nextValue();
            // 接下来的就是JSON对象的操作了
            //person.getJSONArray("phone");
            //person.getString("name");
            int  df =person.getInt("resultCode");
            String tokent = person.getString("token");
            Log.e("falter","de:"+df);
            Log.e("falter","tokent:"+tokent);
            //person.getJSONObject("address");
            //person.getBoolean("married");
        } catch (JSONException ex) {
            // 异常处理代码
        }
    }



    /**
     * 检查网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }

        return true;
    }

//    public void load(View view) {
//        final String path = "https://www.wangjiayin.cn/us-web-app/xsyh/getXsyhByXsid";
//        new Thread() {
//            public void run() {
//                try {
//                    URL url = new URL(path);
//                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                    conn.setRequestMethod("POST");
//                    conn.setReadTimeout(5000);
//                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//                    String data = "xsid=" + "1" + "&token=" + "a7d1e6a5fd5b3e6e30a0387ae3a60d8eb77c9859ebaf5f29410914d2d1c9741b";
//                    conn.setRequestProperty("Content-Length", String.valueOf(data.length()));
//                    conn.setDoOutput(true);
//                    conn.getOutputStream().write(data.getBytes());
//
//
//                    int code = conn.getResponseCode();
//                    InputStream hg = conn.getInputStream();
//                    String obg = changeInputStream(hg,"UTF-8");
//                    Log.e(TAG ,"7878787877:"+code);
//                    if (code == 200) {
//                        InputStream is = conn.getInputStream();
//                        //String result = StreamTools.ReadStream(is);
//                        Message msg = Message.obtain();
//                        //msg.what = SUCCESS;
//                        //msg.obj = result;
//                        //handler.sendMessage(msg);
//                    } else {
//                        Message msg = Message.obtain();
//                        //msg.what = ERROR1;
//                        //handler.sendMessage(msg);
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG ,"5555555555555555555555555:"+e);
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                    Message msg = Message.obtain();
//                    //msg.what = ERROR2;
//                    //handler.sendMessage(msg);
//                }
//            }
//        }.start();
//    }
}
