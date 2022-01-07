package com.aliyun.alink.devicesdk.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliyun.alink.devicesdk.app.AppLog;
import com.aliyun.alink.devicesdk.app.DemoApplication;
import com.aliyun.alink.devicesdk.app.DeviceInfoData;
import com.aliyun.alink.devicesdk.manager.DASHelper;
import com.aliyun.alink.devicesdk.manager.IDemoCallback;
import com.aliyun.alink.devicesdk.manager.InitManager;
import com.aliyun.alink.dm.api.IThing;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tmp.listener.IPublishResourceListener;
import com.aliyun.alink.linksdk.tmp.utils.GsonUtils;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;


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

public class DemoActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "DemoActivity";

    private TextView errorTV = null;
    private AtomicInteger testDeviceIndex = new AtomicInteger(0);

    //fix
    boolean flag = false;

    //变量定义
    TextView tv1 = null, tv2 = null, tv3 = null;//光照值文本显示，
    SensorManager sm;//传感器管理器
    Sensor sensorLight;//光照传感器
    Sensor sensorProximity;//距离传感器
    Sensor sensorHumidity;//湿度传感器
    MyListener sensorListener = new MyListener();//自定义监听器
    Activity context;//Activity
    int id = 200;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppLog.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        errorTV = findViewById(R.id.id_error_info);
        setListener();

        flag = true;

        //将各传感器文本值与控件绑定
        tv1 = (TextView) findViewById(R.id.edt1);
        tv2 = (TextView) findViewById(R.id.edt2);
        tv3 = (TextView) findViewById(R.id.edt3);

        //传感器部分
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//注册传感器服务
        sensorLight = sm.getDefaultSensor(Sensor.TYPE_LIGHT);//绑定光照传感器
        sensorProximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);//绑定距离传感器
        sensorHumidity = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);//绑定湿度传感器
        //获取当前生命周期活动
        context = this;
        //订阅
        LinkKit.getInstance().registerOnPushListener(notifyListener);
        Log.e(TAG, "启用！");

    }

    //设置屏幕亮度
    private void setLight(Activity context, int brightness) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.screenBrightness = Float.valueOf(brightness) * (1f / 255f);
        context.getWindow().setAttributes(lp);
    }

    //监听器,需要重构以下两个函数
    public class MyListener implements SensorEventListener {
        //当传感器监测到的数值发生变化时
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                //当传感器为光照传感器时
                //获取传感器的实时值event.values
                int value = (int) event.values[0];
                tv1.setText(String.valueOf(value));//将光照传感器的值显示在页面上
                sendData("LightLux", String.valueOf(value));//发送光照值到阿里云服务器上
                //设置屏幕亮度
                if (value > 11000.0f)
                    setLight(context, 255);
                else
                    setLight(context, (int) (value * 255 / 11000.0));
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                //当传感器为距离传感器时
                int value = (int) event.values[0];
                tv2.setText(String.valueOf(value));//将距离传感器的值显示在页面上
                sendData("Distance", String.valueOf(value));//发送距离值到阿里云服务器上
            } else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                //当传感器为湿度传感器时
                float value = event.values[0];
                tv3.setText(String.valueOf(value));//将湿度传感器的值显示在页面上
                sendData("Humidity", String.valueOf(value));//发送湿度值到阿里云服务器上
            }
        }

        //当传感器精度发生变化时
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    /**
     * 下行监听器，云端 MQTT 下行数据都会通过这里回调
     */
    //订阅需要部分
    private static IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        /**
         * onNotify 会触发的前提是 shouldHandle 没有指定不处理这个topic
         * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param topic 下行的topic
         * @param aMessage 下行的数据内容
         */
        @Override
        public void onNotify(String connectId, String topic, AMessage aMessage) {
            String data = new String((byte[]) aMessage.data);
            // 服务端返回数据示例  data = {"method":"thing.service.test_service","id":"123374967","params":{"vv":60},"version":"1.0.0"}
            Log.d("*****************", data);
        }

        /**
         * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param topic 下行topic
         * @return 是否要处理这个topic，如果为true，则会回调到onNotify；如果为false，onNotify不会回调这个topic相关的数据。建议默认为true。
         */
        @Override
        public boolean shouldHandle(String connectId, String topic) {
            return true;
        }

        /**
         * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param connectState {@link ConnectState}
         *     CONNECTED, 连接成功
         *     DISCONNECTED, 已断链
         *     CONNECTING, 连接中
         *     CONNECTFAIL; 连接失败
         */
        @Override
        public void onConnectStateChange(String connectId, ConnectState connectState) {
            Log.d(TAG, "onConnectStateChange() called with: connectId = [" + connectId + "], connectState = [" + connectState + "]");
        }
    };

    /**
     * 发布数据
     *
     * @param deviceId 标识符，与阿里云服务器上产品的物模型上属性一致
     * @param value    传感器值
     */
    void sendData(String deviceId, String value) {
        // 发布
        MqttPublishRequest request = new MqttPublishRequest();
        // 设置是否需要应答。
        request.isRPC = false;
        // 设置topic，设备通过该Topic向物联网平台发送消息。
        request.topic = "/sys/gr6pt8ESbuO/bracelet/thing/event/property/post";
        // 设置 qos，默认为0
        request.qos = 0;

        // data 设置需要发布的数据 json String，其中id字段需要保持自增。
        //示例 属性上报，json格式数据
        // {"id":"160865432","method":"thing.event.property.post","params":{"LightSwitch":1},"version":"1.0"}
        request.payloadObj = "{\"id\":" + id + ",\"params\":{\"" + deviceId + "\":" + value + "},\"method\":\"thing.event.property.post\"}";
        id += 100;
        LinkKit.getInstance().publish(request, new IConnectSendListener() {
            @Override
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                Log.e(TAG, "发布成功！");
                //数据发布成功的同时，进行订阅服务
                subScript();
            }

            @Override
            public void onFailure(ARequest aRequest, AError aError) {
                // 发布失败
                Log.e(TAG, "发布失败！");
            }
        });
    }

    // 订阅
    void subScript() {
        // 订阅
        MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
        // subTopic 替换成您需要订阅的 topic
        subscribeRequest.topic = "/sys/gr6pt8ESbuO/bracelet/thing/event/property/post_reply";
        subscribeRequest.isSubscribe = true;
        LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                // 订阅成功
                Log.e(TAG, "订阅成功！");
            }

            @Override
            public void onFailure(AError aError) {
                // 订阅失败
                Log.e(TAG, "订阅失败！");
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        //判断是否有光照传感器
        if (sensorLight != null) {
            //传感器不为空时，进行传感器的注册
            sm.registerListener(sensorListener, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("当前手机没有光照传感器");
        }
        //判断是否有距离传感器
        if (sensorProximity != null) {
            //传感器不为空时，进行传感器的注册
            sm.registerListener(sensorListener, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("当前手机没有距离传感器");
        }
        //判断是否有湿度传感器
        if (sensorHumidity != null) {
            //传感器不为空时，进行传感器的注册
            sm.registerListener(sensorListener, sensorHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("当前手机没有湿度传感器");
        }
    }


    // fix end
    @Override
    protected void onStop() {
        super.onStop();
    }

    private void setListener() {
        try {
            LinearLayout demoLayout = findViewById(R.id.id_demo_layout);
            int size = demoLayout.getChildCount();
            for (int i = 0; i < size; i++) {
                if (i == size - 1) {
                    break;
                }
                View child = demoLayout.getChildAt(i);
                child.setOnClickListener(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AppLog.w(TAG, "setListener exception " + e);
        }
    }

    public void startOTATest(View view) {
        if (!checkReady()) {
            return;
        }

        Intent intent = new Intent(this, OTAActivity.class);
        startActivity(intent);
    }


    public void startBreezeOTATest(View view) {
        if (!checkReady()) {
            return;
        }

//        Intent intent = new Intent(this, BreezeOtaActivity.class);
//        startActivity(intent);
    }

    public void startLPTest(View view) {
        if (!checkReady()) {
            return;
        }
        if (LinkKit.getInstance().getDeviceThing() == null) {
            showToast("物模型功能未启用");
            return;
        }
        Intent intent = new Intent(this, TSLActivity.class);
        startActivity(intent);
    }

    public void startLabelTest(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, LabelActivity.class);
        startActivity(intent);
    }

    public void startCOTATest(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, COTAActivity.class);
        startActivity(intent);
    }

    public void startShadowTest(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, ShadowActivity.class);
        startActivity(intent);
    }


    public void startGatewayTest(View view) {
        if (!checkReady()) {
            return;
        }
        if (LinkKit.getInstance().getGateway() == null) {
            showToast("网关功能未启用");
            return;
        }
        Intent intent = new Intent(this, GatewayActivity.class);
        startActivity(intent);
    }

    private boolean checkReady() {
        if (DemoApplication.userDevInfoError) {
            showToast("设备三元组信息res/raw/deviceinfo格式错误");
            return false;
        }
        if (!DemoApplication.isInitDone) {
            showToast("初始化尚未成功，请稍后点击");
            return false;
        }
        errorTV.setVisibility(View.GONE);
        return true;
    }

    public void startH2FileTest(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, H2FileManagerActivity.class);
        startActivity(intent);
    }

    public void startLogPush(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, LogPushActivity.class);
        startActivity(intent);
    }

    //Mqtt连接
    public void startMqttTest(View view) {
        if (!checkReady()) {
            return;
        }
        Intent intent = new Intent(this, MqttActivity.class);
        startActivity(intent);
    }


    private void startResetTest(View v) {
        Intent intent = new Intent(this, ResetActivity.class);
        startActivity(intent);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_start_LP:
                startLPTest(v);
                break;
            case R.id.id_start_label:
                startLabelTest(v);
                break;
            case R.id.id_start_cota:
                startCOTATest(v);
                break;
            case R.id.id_start_shadow:
                startShadowTest(v);
                break;
            case R.id.id_start_gateway:
                startGatewayTest(v);
                break;
            case R.id.id_start_ota:
                startOTATest(v);
                break;
            case R.id.id_start_breeze_ota:
                startBreezeOTATest(v);
                break;
            case R.id.id_start_h2_file:
                startH2FileTest(v);
                break;
            case R.id.id_test_init:
                connect();
                break;
            case R.id.id_test_deinit:
                deinit();
                break;
            case R.id.id_mqtt_test:
//                testJniLeakWithCoAP();
                startMqttTest(v);
                break;
            case R.id.id_test_reset:
                startResetTest(v);
                break;
            case R.id.id_log_push:
                startLogPush(v);
                break;
        }
    }

    private static ArrayList<DeviceInfoData> getTestDataList() {
        ArrayList<DeviceInfoData> infoDataArrayList = new ArrayList<DeviceInfoData>();

        DeviceInfoData test6 = new DeviceInfoData();
        test6.productKey = DemoApplication.productKey;
        test6.deviceName = DemoApplication.deviceName;
        test6.deviceSecret = DemoApplication.deviceSecret;
        infoDataArrayList.add(test6);
        return infoDataArrayList;
    }

    /**
     * 初始化
     * 耗时操作，建议放到异步线程
     */
    private void connect() {
        AppLog.d(TAG, "connect() called");
        // SDK初始化
        DeviceInfoData deviceInfoData = getTestDataList().get(testDeviceIndex.getAndIncrement() % getTestDataList().size());
        DemoApplication.productKey = deviceInfoData.productKey;
        DemoApplication.deviceName = deviceInfoData.deviceName;
        DemoApplication.deviceSecret = deviceInfoData.deviceSecret;
        new Thread(new Runnable() {
            @Override
            public void run() {
                InitManager.init(DemoActivity.this, DemoApplication.productKey, DemoApplication.deviceName,
                        DemoApplication.deviceSecret, DemoApplication.productSecret, new IDemoCallback() {

                            @Override
                            public void onError(AError aError) {
                                AppLog.d(TAG, "onError() called with: aError = [" + InitManager.getAErrorString(aError) + "]");
                                // 初始化失败，初始化失败之后需要用户负责重新初始化
                                // 如一开始网络不通导致初始化失败，后续网络恢复之后需要重新初始化

                                if (aError != null) {
//                                    AppLog.d(TAG, "初始化失败，错误信息：" + aError.getCode() + "-" + aError.getSubCode() + ", " + aError.getMsg());
                                    showToast("初始化失败，错误信息：" + aError.getCode() + "-" + aError.getSubCode() + ", " + aError.getMsg());
                                } else {
//                                    AppLog.d(TAG, "初始化失败");
                                    showToast("初始化失败");
                                }
                            }

                            @Override
                            public void onInitDone(Object data) {
                                AppLog.d(TAG, "onInitDone() called with: data = [" + data + "]");
                                DemoApplication.isInitDone = true;
                                showToast("初始化成功");
//                                AppLog.d(TAG, "初始化成功");
                            }
                        });
            }
        }).start();
    }

    /**
     * 耗时操作，建议放到异步线程
     * 反初始化同步接口
     */
    private void deinit() {
        AppLog.d(TAG, "deinit");
        DemoApplication.isInitDone = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 同步接口
                LinkKit.getInstance().deinit();
                DASHelper.getInstance().deinit();
                showToast("反初始化成功");
//                AppLog.d(TAG, "反初始化成功");
            }
        }).start();
    }

    private void publishTest() {
        try {
            AppLog.d(TAG, "publishTest called.");
            MqttPublishRequest request = new MqttPublishRequest();
            // 支持 0 和 1， 默认0
            request.qos = 1;
            request.isRPC = false;
            request.topic = "/" + DemoApplication.productKey + "/" + DemoApplication.deviceName + "/user/update";
            request.msgId = String.valueOf(IDGenerater.generateId());
            // TODO 用户根据实际情况填写 仅做参考
            request.payloadObj = "{\"id\":\"" + request.msgId + "\", \"version\":\"1.0\"" + ",\"params\":{\"state\":\"1\"} }";
            LinkKit.getInstance().publish(request, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
                    showToast("发布成功");
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                    showToast("发布失败 " + (aError != null ? aError.getCode() : "null"));
                }
            });
        } catch (Exception e) {
            showToast("发布异常 ");
        }
    }


    private ScheduledFuture future = null;

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
