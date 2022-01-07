package com.aliyun.alink.devicesdk.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.devicesdk.demo.R;
import com.aliyun.alink.devicesdk.manager.IDemoCallback;
import com.aliyun.alink.devicesdk.manager.InitManager;
import com.aliyun.alink.dm.api.BaseInfo;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.channel.core.base.IOnCallListener;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttConfigure;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttInitParams;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.id2.Id2ItlsSdk;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.aliyun.alink.linksdk.tools.ThreadTools;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

/*
 * Copyright (c) 2014-2016 Alibaba Group. All rights reserved.
 * License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
// Demo 使用注意事项：
// 1.属性上报等与云端的操作 必须在初始化完成onInitDone之后执行；
// 2.初始化的时候请确保网络是OK的，如果初始化失败了，可以在网络连接再次初始化；
//
public class DemoApplication extends Application {
    private static final String TAG = "DemoApplication";


    /**
     * 判断是否初始化完成
     * 未初始化完成，所有和云端的长链通信都不通
     */
    public static boolean isInitDone = false;
    public static boolean userDevInfoError = false;
    public static DeviceInfoData mDeviceInfoData = null;


    public static String productKey = null, deviceName = null, deviceSecret = null, productSecret = null,
            password = null, username = null,clientId = null, deviceToken = null;
    public static Context mAppContext = null;
    private String registerType = null;

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);

        MqttConfigure.itlsLogLevel = Id2ItlsSdk.DEBUGLEVEL_NODEBUG;
        AppLog.setLevel(ALog.LEVEL_DEBUG);
        com.aliyun.isoc.aps.DASLogger.setEnabled(true);
        // 设置心跳时间，默认65秒
        MqttConfigure.setKeepAliveInterval(65);

//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
        mAppContext = getApplicationContext();
        // 从 raw 读取指定测试文件
        String testData = getFromRaw();
        AppLog.i(TAG, "sdk version = " + LinkKit.getInstance().getSDKVersion());
        // 解析数据
        getDeviceInfoFrom(testData);

        if (userDevInfoError) {
            showToast("三元组文件格式不正确，请重新检查格式");
        }

        if (TextUtils.isEmpty(deviceSecret)) {
            tryGetFromSP();
        }

        /**
         * 动态注册
         * 只有pk dn ps 的时候 需要先动态注册获取ds，然后使用pk+dn+ds进行初始化建联，如果一开始有ds则无需执行动态注册
         * 动态注册之后需要将 ds保存起来，下次应用重新启动的时候，直接拿上次的ds进行初始化建联。
         * 如果需要应用卸载之后仍然可以使用ds建联，需要第一次动态初始化将ds保存到非应用目录，确保卸载应用之后ds仍然存在。
         * 如果动态注册之后，应用卸载了，没有保存ds的话，重新安装执行动态注册是会失败的。
         * 注意：动态注册成功，设备上线之后，不能再次执行动态注册，云端会返回已主动注册。
         */
        if (TextUtils.isEmpty(deviceSecret) && !TextUtils.isEmpty(productSecret) && TextUtils.isEmpty(deviceToken)) {

            if (TextUtils.isEmpty(registerType)) {
                AppLog.d(TAG, "api网关动态注册");
                InitManager.registerDevice(this, productKey, deviceName, productSecret, new IConnectSendListener() {
                    @Override
                    public void onResponse(ARequest aRequest, AResponse aResponse) {
                        AppLog.d(TAG, "registerDevice onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? "null" : aResponse.data) + "]");
                        if (aResponse != null && aResponse.data != null) {
                            // 解析云端返回的数据
                            ResponseModel<Map<String, String>> response = JSONObject.parseObject(aResponse.data.toString(),
                                    new TypeReference<ResponseModel<Map<String, String>>>() {
                                    }.getType());
                            if ("200".equals(response.code) && response.data != null && response.data.containsKey("deviceSecret") &&
                                    !TextUtils.isEmpty(response.data.get("deviceSecret"))) {
                                /**
                                 * ds必须保存在非应用目录，确保卸载之后ds仍然可以读取到。
                                 * 以下代码仅供测试验证使用
                                 */
                                deviceSecret = response.data.get("deviceSecret");
                                // getDeviceSecret success, to build connection.
                                // 持久化 deviceSecret 初始化建联的时候需要
                                // 用户需要按照实际场景持久化设备的三元组信息，用于后续的连接
                                SharedPreferences preferences = getSharedPreferences("deviceAuthInfo", 0);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("deviceId", productKey + deviceName);
                                editor.putString("deviceSecret", deviceSecret);
                                //提交当前数据
                                editor.commit();

                                LinkKit.getInstance().deinit();
                                connect();
                            } else {
                            }
                        }
                    }

                    @Override
                    public void onFailure(ARequest aRequest, AError aError) {
                        AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                    }
                });
            } else {

                // docs: https://help.aliyun.com/document_detail/132111.html?spm=a2c4g.11186623.6.600.4e073f827Y7a8y
                MqttInitParams initParams = new MqttInitParams(productKey, productSecret, deviceName, deviceSecret, MqttConfigure.MQTT_SECURE_MODE_TLS);
                if ("regnwl".equals(registerType)) {
                    initParams.registerType = "regnwl"; // 一型一密免白
                    AppLog.d(TAG, "mqtt 一型一密免白动态注册");
                } else {
                    AppLog.d(TAG, "动态注册registerType无效");
                    showToast("动态注册registerType无效");
                    return;
                }
                LinkKit.getInstance().deviceDynamicRegister(this, initParams, new IOnCallListener() {
                    @Override
                    public void onSuccess(com.aliyun.alink.linksdk.channel.core.base.ARequest request, com.aliyun.alink.linksdk.channel.core.base.AResponse response) {
                        AppLog.i(TAG, "onSuccess() called with: request = [" + request + "], response = [" + response + "]");
                        // response.data is byte array
                        try {
                            String responseData = new String((byte[]) response.data);
                            JSONObject jsonObject = JSONObject.parseObject(responseData);
                            String pk = jsonObject.getString("productKey");
                            String dn = jsonObject.getString("deviceName");
                            // 非一型一密免白返回
                            String ds = jsonObject.getString("deviceSecret");
                            // 一型一密免白返回
                            String ci = jsonObject.getString("clientId");
                            String dt = jsonObject.getString("deviceToken");

                            clientId = ci;
                            deviceToken = dt;
                            deviceSecret = ds;

                            // 持久化 clientId & deviceToken 初始化建联的时候需要
                            // 这里仅为测试代码，请将认证信息持久化到外部存储，确保app清除缓存或者卸载重装后仍能取到
                            SharedPreferences preferences = getSharedPreferences("deviceAuthInfo", 0);
                            if ((!TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(deviceToken)) || (!TextUtils.isEmpty(deviceSecret))) {
                                showToast("一型一密免白动态注册成功");
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("deviceId", productKey + deviceName);
                                editor.putString("clientId", clientId);
                                editor.putString("deviceToken", deviceToken);
                                editor.putString("deviceSecret", deviceSecret);
                                editor.commit();
                                try {
                                    Thread.sleep(2000);
                                } catch (Exception e){

                                }
                                destroyRegisterConnect(true);
                            } else {
                                showToast("一型一密免白动态注册成功失败，返回信息无效 " + responseData);
                                destroyRegisterConnect(false);
                            }
                        } catch (Exception e) {
                            showToast("一型一密免白动态注册成功失败，返回数据信息无效");
                            e.printStackTrace();
                            destroyRegisterConnect(false);
                        }

                    }

                    @Override
                    public void onFailed(com.aliyun.alink.linksdk.channel.core.base.ARequest request, com.aliyun.alink.linksdk.channel.core.base.AError error) {
                        AppLog.w(TAG, "onFailed() called with: request = [" + request + "], error = [" + error + "]");
                        showToast("一型一密免白动态注册失败 " + error);
                        destroyRegisterConnect(false);
                    }

                    @Override
                    public boolean needUISafety() {
                        return false;
                    }
                });
            }
        } else if (!TextUtils.isEmpty(deviceSecret) || !TextUtils.isEmpty(password) || !TextUtils.isEmpty(deviceToken)){
            connect();
        } else {
            AppLog.e(TAG, "res/raw/deviceinfo invalid.");
            if (!userDevInfoError) {
                showToast("三元组信息无效，请重新填写");
            }
            userDevInfoError = true;
        }

    }

    /**
     * 注意该接口不能在 动态注册回调线程里面调用，mqtt 通道会报 Disconnecting is not allowed from a callback method (32107)
     * @param needConnect
     */
    private void destroyRegisterConnect(final boolean needConnect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LinkKit.getInstance().stopDeviceDynamicRegister(10 * 1000, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            AppLog.d(TAG, "onSuccess() called with: iMqttToken = [" + iMqttToken + "]");
                            if (needConnect) {
                                connect();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                            AppLog.w(TAG, "onFailure() called with: iMqttToken = [" + iMqttToken + "], throwable = [" + throwable + "]");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 初始化建联
     * 如果初始化建联失败，需要用户重试去完成初始化，并确保初始化成功。如应用启动的时候无网络，导致失败，可以在网络可以的时候再次执行初始化，成功之后不需要再次执行。
     * 初始化成功之后，如果因为网络原因连接断开了，用户不需要执行初始化建联操作，SDK会处理建联。
     *
     * onError 初始化失败
     * onInitDone 初始化成功
     *
     * SDK 支持以userName+password+clientId 的方式登录（不推荐，建议使用三元组建联）
     * 设置如下参数，InitManager.init的时候 deviceSecret, productSecret 可以不填
     * MqttConfigure.mqttUserName = username;
     * MqttConfigure.mqttPassWord = password;
     * MqttConfigure.mqttClientId = clientId;
     *
     */
    private void connect() {
        AppLog.d(TAG, "connect() called");
        // SDK初始化
//        MqttConfigure.mqttUserName = username;
//        MqttConfigure.mqttPassWord = password;
//        MqttConfigure.mqttClientId = clientId;
        InitManager.init(this, productKey, deviceName, deviceSecret, productSecret, new IDemoCallback() {

            @Override
            public void onError(AError aError) {
                AppLog.d(TAG, "onError() called with: aError = [" + aError + "]");
                AppLog.d(TAG,Log.getStackTraceString(new Throwable()));
                // 初始化失败，初始化失败之后需要用户负责重新初始化
                // 如一开始网络不通导致初始化失败，后续网络回复之后需要重新初始化
                showToast("初始化失败");
            }

            @Override
            public void onInitDone(Object data) {
                AppLog.d(TAG, "onInitDone() called with: data = [" + data + "]");
                showToast("初始化成功");
                isInitDone = true;
            }
        });

    }

    /**
     * 尝试读取deviceSecret，适用于动态注册的设备
     * 注意：仅做参考，不适合于正式产品使用
     * 动态注册的deviceSecret应该保存在非应用目录，确保应用删除之后，该数据没有被删除。
     */
    private void tryGetFromSP() {
        AppLog.d(TAG, "tryGetFromSP() called");
        SharedPreferences authInfo = getSharedPreferences("deviceAuthInfo", Activity.MODE_PRIVATE);
        String pkDn = authInfo.getString("deviceId", null);
        String ci = authInfo.getString("clientId", null);
        String dt = authInfo.getString("deviceToken", null);
        String ds = authInfo.getString("deviceSecret", null);
        if (pkDn != null && pkDn.equals(productKey + deviceName) &&
                (!TextUtils.isEmpty(ds) || !TextUtils.isEmpty(dt))) {
            AppLog.d(TAG, "tryGetFromSP update ds from sp.");
            deviceSecret = ds;
            clientId = ci;
            deviceToken = dt;
        } else {
            AppLog.d(TAG, "tryGetFromSP no cache data.");
        }
    }

    private void getDeviceInfoFrom(String testData) {
        AppLog.d(TAG, "getDeviceInfoFrom() called with: testData = [" + testData + "]");
        try {
            if (testData == null) {
                AppLog.e(TAG, "getDeviceInfoFrom: data empty.");
                userDevInfoError = true;
                return;
            }
            Gson mGson = new Gson();
            DeviceInfoData deviceInfoData = mGson.fromJson(testData, DeviceInfoData.class);
            if (deviceInfoData == null) {
                AppLog.e(TAG, "getDeviceInfoFrom: file format error.");
                userDevInfoError = true;
                return;
            }
            AppLog.d(TAG, "getDeviceInfoFrom deviceInfoData=" + deviceInfoData);
            if (checkValid(deviceInfoData)) {
                mDeviceInfoData = new DeviceInfoData();
                mDeviceInfoData.productKey = deviceInfoData.productKey;
                mDeviceInfoData.productSecret = deviceInfoData.productSecret;
                mDeviceInfoData.deviceName = deviceInfoData.deviceName;
                mDeviceInfoData.deviceSecret = deviceInfoData.deviceSecret;
                mDeviceInfoData.username = deviceInfoData.username;
                mDeviceInfoData.password = deviceInfoData.password;
                mDeviceInfoData.clientId = deviceInfoData.clientId;
                mDeviceInfoData.deviceToken = deviceInfoData.deviceToken;
                mDeviceInfoData.registerType = deviceInfoData.registerType;

                userDevInfoError = false;

                mDeviceInfoData.subDevice = new ArrayList<>();
                if (deviceInfoData.subDevice == null) {
                    AppLog.d(TAG, "getDeviceInfoFrom: subDevice empty..");
                    return;
                }
                for (int i = 0; i < deviceInfoData.subDevice.size(); i++) {
                    if (checkValid(deviceInfoData.subDevice.get(i))) {
                        mDeviceInfoData.subDevice.add(deviceInfoData.subDevice.get(i));
                    } else {
                        AppLog.d(TAG, "getDeviceInfoFrom: subDevice info invalid. discard.");
                    }
                }

                productKey = mDeviceInfoData.productKey;
                deviceName = mDeviceInfoData.deviceName;
                deviceSecret = mDeviceInfoData.deviceSecret;
                productSecret = mDeviceInfoData.productSecret;
                password = mDeviceInfoData.password;
                username = mDeviceInfoData.username;
                clientId = mDeviceInfoData.clientId;
                deviceToken = mDeviceInfoData.deviceToken;
                registerType = mDeviceInfoData.registerType;

                AppLog.d(TAG, "getDeviceInfoFrom: final data=" + mDeviceInfoData);
            } else {
                AppLog.e(TAG, "res/raw/deviceinfo error.");
                userDevInfoError = true;
            }

        } catch (Exception e) {
            AppLog.e(TAG, "getDeviceInfoFrom: e", e);
            userDevInfoError = true;
        }

    }

    private boolean checkValid(BaseInfo baseInfo) {
        if (baseInfo == null) {
            return false;
        }
        if (TextUtils.isEmpty(baseInfo.productKey) || TextUtils.isEmpty(baseInfo.deviceName)) {
            return false;
        }
        if (baseInfo instanceof DeviceInfoData) {
            if (TextUtils.isEmpty(((DeviceInfo) baseInfo).productSecret) && TextUtils.isEmpty(((DeviceInfo) baseInfo).deviceSecret) && TextUtils.isEmpty(((DeviceInfoData) baseInfo).password)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 注意：该场景只适合于设备未激活的场景
     * 验证一型一密 需要以下步骤：
     * 1.云端创建产品，开启产品的动态注册功能；
     * 2.创建一个设备，在文件中(raw/deviceinfo)填写改设备信息 productKey，deviceName， productSecret;
     * 3.通过这三个信息可以去云端动态拿到deviceSecret，并建立长连接；
     *
     * @return
     */
    public String getFromRaw() {
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            inputReader = new InputStreamReader(getResources().openRawResource(R.raw.deviceinfo));
            bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                }
                if (inputReader != null){
                    inputReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    private void showToast(final String message) {
        ThreadTools.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DemoApplication.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static Context getAppContext() {
        return mAppContext;
    }
}
