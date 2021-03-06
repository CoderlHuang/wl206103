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

    //????????????
    TextView tv1 = null, tv2 = null, tv3 = null;//????????????????????????
    SensorManager sm;//??????????????????
    Sensor sensorLight;//???????????????
    Sensor sensorProximity;//???????????????
    Sensor sensorHumidity;//???????????????
    MyListener sensorListener = new MyListener();//??????????????????
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

        //???????????????????????????????????????
        tv1 = (TextView) findViewById(R.id.edt1);
        tv2 = (TextView) findViewById(R.id.edt2);
        tv3 = (TextView) findViewById(R.id.edt3);

        //???????????????
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//?????????????????????
        sensorLight = sm.getDefaultSensor(Sensor.TYPE_LIGHT);//?????????????????????
        sensorProximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);//?????????????????????
        sensorHumidity = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);//?????????????????????
        //??????????????????????????????
        context = this;
        //??????
        LinkKit.getInstance().registerOnPushListener(notifyListener);
        Log.e(TAG, "?????????");

    }

    //??????????????????
    private void setLight(Activity context, int brightness) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.screenBrightness = Float.valueOf(brightness) * (1f / 255f);
        context.getWindow().setAttributes(lp);
    }

    //?????????,??????????????????????????????
    public class MyListener implements SensorEventListener {
        //?????????????????????????????????????????????
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                //?????????????????????????????????
                //???????????????????????????event.values
                int value = (int) event.values[0];
                tv1.setText(String.valueOf(value));//??????????????????????????????????????????
                sendData("LightLux", String.valueOf(value));//???????????????????????????????????????
                //??????????????????
                if (value > 11000.0f)
                    setLight(context, 255);
                else
                    setLight(context, (int) (value * 255 / 11000.0));
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                //?????????????????????????????????
                int value = (int) event.values[0];
                tv2.setText(String.valueOf(value));//??????????????????????????????????????????
                sendData("Distance", String.valueOf(value));//???????????????????????????????????????
            } else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                //?????????????????????????????????
                float value = event.values[0];
                tv3.setText(String.valueOf(value));//??????????????????????????????????????????
                sendData("Humidity", String.valueOf(value));//???????????????????????????????????????
            }
        }

        //?????????????????????????????????
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    /**
     * ???????????????????????? MQTT ????????????????????????????????????
     */
    //??????????????????
    private static IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        /**
         * onNotify ????????????????????? shouldHandle ???????????????????????????topic
         * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param topic ?????????topic
         * @param aMessage ?????????????????????
         */
        @Override
        public void onNotify(String connectId, String topic, AMessage aMessage) {
            String data = new String((byte[]) aMessage.data);
            // ???????????????????????????  data = {"method":"thing.service.test_service","id":"123374967","params":{"vv":60},"version":"1.0.0"}
            Log.d("*****************", data);
        }

        /**
         * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param topic ??????topic
         * @return ?????????????????????topic????????????true??????????????????onNotify????????????false???onNotify??????????????????topic?????????????????????????????????true???
         */
        @Override
        public boolean shouldHandle(String connectId, String topic) {
            return true;
        }

        /**
         * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
         * @param connectState {@link ConnectState}
         *     CONNECTED, ????????????
         *     DISCONNECTED, ?????????
         *     CONNECTING, ?????????
         *     CONNECTFAIL; ????????????
         */
        @Override
        public void onConnectStateChange(String connectId, ConnectState connectState) {
            Log.d(TAG, "onConnectStateChange() called with: connectId = [" + connectId + "], connectState = [" + connectState + "]");
        }
    };

    /**
     * ????????????
     *
     * @param deviceId ?????????????????????????????????????????????????????????????????????
     * @param value    ????????????
     */
    void sendData(String deviceId, String value) {
        // ??????
        MqttPublishRequest request = new MqttPublishRequest();
        // ???????????????????????????
        request.isRPC = false;
        // ??????topic??????????????????Topic?????????????????????????????????
        request.topic = "/sys/gr6pt8ESbuO/bracelet/thing/event/property/post";
        // ?????? qos????????????0
        request.qos = 0;

        // data ??????????????????????????? json String?????????id???????????????????????????
        //?????? ???????????????json????????????
        // {"id":"160865432","method":"thing.event.property.post","params":{"LightSwitch":1},"version":"1.0"}
        request.payloadObj = "{\"id\":" + id + ",\"params\":{\"" + deviceId + "\":" + value + "},\"method\":\"thing.event.property.post\"}";
        id += 100;
        LinkKit.getInstance().publish(request, new IConnectSendListener() {
            @Override
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                Log.e(TAG, "???????????????");
                //????????????????????????????????????????????????
                subScript();
            }

            @Override
            public void onFailure(ARequest aRequest, AError aError) {
                // ????????????
                Log.e(TAG, "???????????????");
            }
        });
    }

    // ??????
    void subScript() {
        // ??????
        MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
        // subTopic ??????????????????????????? topic
        subscribeRequest.topic = "/sys/gr6pt8ESbuO/bracelet/thing/event/property/post_reply";
        subscribeRequest.isSubscribe = true;
        LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                // ????????????
                Log.e(TAG, "???????????????");
            }

            @Override
            public void onFailure(AError aError) {
                // ????????????
                Log.e(TAG, "???????????????");
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        //??????????????????????????????
        if (sensorLight != null) {
            //????????????????????????????????????????????????
            sm.registerListener(sensorListener, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("?????????????????????????????????");
        }
        //??????????????????????????????
        if (sensorProximity != null) {
            //????????????????????????????????????????????????
            sm.registerListener(sensorListener, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("?????????????????????????????????");
        }
        //??????????????????????????????
        if (sensorHumidity != null) {
            //????????????????????????????????????????????????
            sm.registerListener(sensorListener, sensorHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            tv1.setText("?????????????????????????????????");
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
            showToast("????????????????????????");
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
            showToast("?????????????????????");
            return;
        }
        Intent intent = new Intent(this, GatewayActivity.class);
        startActivity(intent);
    }

    private boolean checkReady() {
        if (DemoApplication.userDevInfoError) {
            showToast("?????????????????????res/raw/deviceinfo????????????");
            return false;
        }
        if (!DemoApplication.isInitDone) {
            showToast("???????????????????????????????????????");
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

    //Mqtt??????
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
     * ?????????
     * ???????????????????????????????????????
     */
    private void connect() {
        AppLog.d(TAG, "connect() called");
        // SDK?????????
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
                                // ????????????????????????????????????????????????????????????????????????
                                // ?????????????????????????????????????????????????????????????????????????????????????????????

                                if (aError != null) {
//                                    AppLog.d(TAG, "?????????????????????????????????" + aError.getCode() + "-" + aError.getSubCode() + ", " + aError.getMsg());
                                    showToast("?????????????????????????????????" + aError.getCode() + "-" + aError.getSubCode() + ", " + aError.getMsg());
                                } else {
//                                    AppLog.d(TAG, "???????????????");
                                    showToast("???????????????");
                                }
                            }

                            @Override
                            public void onInitDone(Object data) {
                                AppLog.d(TAG, "onInitDone() called with: data = [" + data + "]");
                                DemoApplication.isInitDone = true;
                                showToast("???????????????");
//                                AppLog.d(TAG, "???????????????");
                            }
                        });
            }
        }).start();
    }

    /**
     * ???????????????????????????????????????
     * ????????????????????????
     */
    private void deinit() {
        AppLog.d(TAG, "deinit");
        DemoApplication.isInitDone = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // ????????????
                LinkKit.getInstance().deinit();
                DASHelper.getInstance().deinit();
                showToast("??????????????????");
//                AppLog.d(TAG, "??????????????????");
            }
        }).start();
    }

    private void publishTest() {
        try {
            AppLog.d(TAG, "publishTest called.");
            MqttPublishRequest request = new MqttPublishRequest();
            // ?????? 0 ??? 1??? ??????0
            request.qos = 1;
            request.isRPC = false;
            request.topic = "/" + DemoApplication.productKey + "/" + DemoApplication.deviceName + "/user/update";
            request.msgId = String.valueOf(IDGenerater.generateId());
            // TODO ?????????????????????????????? ????????????
            request.payloadObj = "{\"id\":\"" + request.msgId + "\", \"version\":\"1.0\"" + ",\"params\":{\"state\":\"1\"} }";
            LinkKit.getInstance().publish(request, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
                    showToast("????????????");
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                    showToast("???????????? " + (aError != null ? aError.getCode() : "null"));
                }
            });
        } catch (Exception e) {
            showToast("???????????? ");
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
