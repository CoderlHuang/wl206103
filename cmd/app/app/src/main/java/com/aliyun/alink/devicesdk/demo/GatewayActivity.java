package com.aliyun.alink.devicesdk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.devicesdk.adapter.SubDeviceListAdapter;
import com.aliyun.alink.devicesdk.app.AppLog;
import com.aliyun.alink.devicesdk.app.DemoApplication;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.api.SignUtils;
import com.aliyun.alink.dm.model.RequestModel;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.channel.gateway.api.GatewayChannel;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceActionListener;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceChannel;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceConnectListener;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceRemoveListener;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.util.RandomStringUtil;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *
 *  Copyright (c) 2014-2016 Alibaba Group. All rights reserved.
 *  License-Identifier: Apache-2.0
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


public class GatewayActivity extends BaseActivity {
    private static final String TAG = "GatewayActivity";

    private List<DeviceInfo> subDeviceTripleInfoList = new ArrayList<>();

    private String testPublishTopic = "/sys/{productKey}/{deviceName}/thing/event/property/post";
    private String testSubscribePropertyService = "/sys/{productKey}/{deviceName}/thing/service/property/set";
    private String testSubscribeService = "/sys/{productKey}/{deviceName}/thing/service/+";
    private String testSubscribeSyncService = "/sys/{productKey}/{deviceName}/rrpc/request/+";
    private String[] subscribeServiceList = {testSubscribePropertyService, testSubscribeService, testSubscribeSyncService};


    private Spinner subDeviceListSpinner = null;
    private DeviceInfo selectedSubdeviceInfo = null;
    private SubDeviceListAdapter subDeviceListAdapter = null;
    private EditText publishPayloadET = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway);
        initViews();
        addPermitJoinSupport();
    }

    private void addPermitJoinSupport() {
        LinkKit.getInstance().getGateway().permitJoin(BaseTemplateActivity.mConnectRrpcListener);
    }

    private void initViews() {
//        publishPayloadET = findViewById(R.id.id_publish_payload);
        subDeviceListSpinner = findViewById(R.id.id_sub_dev_list);
        subDeviceListAdapter = new SubDeviceListAdapter(this);
        subDeviceListSpinner.setAdapter(subDeviceListAdapter);
        subDeviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppLog.d(TAG, "onItemSelected() called with: parent = [" + parent + "], view = [" + view + "], position = [" + position + "], id = [" + id + "]");
                if (subDeviceListAdapter == null) {
                    return;
                }
                if (subDeviceListAdapter.getCount() < 1 || position < 0 || position > subDeviceListAdapter.getCount() - 1) {
                    AppLog.w(TAG, "position invalid, position=" + position + ",count=" + subDeviceListAdapter.getCount());
                    return;
                }
                selectedSubdeviceInfo = (DeviceInfo) subDeviceListAdapter.getItem(position);
                if (selectedSubdeviceInfo == null) {
                    AppLog.w(TAG, "selected device info is null.");
                    return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                AppLog.d(TAG, "onNothingSelected() called with: parent = [" + parent + "]");

            }
        });
    }


    private void updateSpinnerList(final List<DeviceInfo> infoList) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (subDeviceListAdapter != null) {
                    subDeviceListAdapter.setListData(infoList);
                    subDeviceListAdapter.notifyDataSetChanged();
                    if (selectedSubdeviceInfo == null && infoList.size() > 0) {
                        selectedSubdeviceInfo = (DeviceInfo) subDeviceListAdapter.getItem(0);
                    }
                }
            }
        });
    }

    /**
     * ?????????????????????
     *
     * @param view ????????????????????????  ???????????????????????? ?????????
     */
    public void subDevRegister(View view) {
        List<DeviceInfo> toRegisterDeviceList = getSubDevList();
        if (toRegisterDeviceList == null || toRegisterDeviceList.size() < 1) {
            showToast("??????????????????????????????");
            return;
        }
        List<DeviceInfo> presetSubdevList = new ArrayList<>();
        List<DeviceInfo> normalSubDevList = new ArrayList<>();
        DeviceInfo item = null;
        for (int i = 0; i < toRegisterDeviceList.size(); i++) {
            item = toRegisterDeviceList.get(i);
            if (item == null) {
                continue;
            }
            if (!TextUtils.isEmpty(item.productSecret)) {
                presetSubdevList.add(toRegisterDeviceList.get(i));
            } else {
                normalSubDevList.add(toRegisterDeviceList.get(i));
            }
        }

        if (presetSubdevList.size() > 0) {
            // ???????????????????????????????????????????????????productSecret????????????????????????????????????????????????????????????
            // ???????????????????????????????????????COTA-????????????????????????????????????????????????????????????pk???dn???ps??????????????????
            // ??????????????????
            // ???????????????????????????????????????????????????????????????????????????????????????
            MqttPublishRequest request = new MqttPublishRequest();
            final RequestModel requestModel = new RequestModel();
            requestModel.id = String.valueOf(IDGenerater.generateId());
            requestModel.version = "1.0";
            requestModel.method = GatewayChannel.METHOD_PRESET_SUBDEV_REGITER;
            request.isRPC = true;
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < presetSubdevList.size(); i++) {
                DeviceInfo itemDev = presetSubdevList.get(i);
                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("productKey", itemDev.productKey);
                itemMap.put("deviceName", itemDev.deviceName);
                itemMap.put("random", RandomStringUtil.getRandomString(10));
                String sign = SignUtils.hmacSign(itemMap, itemDev.productSecret);
                itemMap.put("sign", sign);
                itemMap.put("signMethod", "hmacsha1");
                jsonArray.add(itemMap);
            }

            jsonObject.put("proxieds", jsonArray);
            requestModel.params = jsonObject;
            request.payloadObj = requestModel.toString();
            LinkKit.getInstance().getGateway().subDevicRegister(request, enhanceSendListener);
        }

        if (normalSubDevList.size() > 0) {
            // ??????????????????????????????pk???dn?????????????????????????????????
            // ??????????????? ???????????????????????????????????????????????????????????????????????????????????????
            LinkKit.getInstance().getGateway().gatewaySubDevicRegister(normalSubDevList, normalSendListener);
        }
    }

    private IConnectSendListener enhanceRegister = new IConnectSendListener() {
        @Override
        public void onResponse(ARequest aRequest, AResponse aResponse) {
            AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
            try {
                showToast("???????????????????????????");
                ResponseModel<Map<String, List<DeviceInfo>>> responseModel = JSONObject.parseObject(aResponse.data.toString(),
                        new TypeReference<ResponseModel<Map<String, List<DeviceInfo>>>>() {
                        }.getType());
                // TODO ?????????????????????????????????
                AppLog.d(TAG, "onResponse responseModel=" + JSONObject.toJSONString(responseModel));
                // {"code":200,"data":{"failures":[],"successes":[{"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}]},"id":"1","message":"success","method":"thing.proxy.provisioning.product_register","version":"1.0"}
                List<DeviceInfo> successList = responseModel.data.get("successes");
                List<DeviceInfo> failList = responseModel.data.get("failures");
                selectedSubdeviceInfo = null;
                updateSpinnerList(successList);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onFailure(ARequest aRequest, AError aError) {
            AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
        }
    };

    private IConnectSendListener normalRegister = new IConnectSendListener() {
        @Override
        public void onResponse(ARequest aRequest, AResponse aResponse) {
            AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? "null" : aResponse.data) + "]");
            try {
                showToast("???????????????????????????");
                ResponseModel<List<DeviceInfo>> response = JSONObject.parseObject(aResponse.data.toString(), new TypeReference<ResponseModel<List<DeviceInfo>>>() {
                }.getType());
                //TODO ?????????????????????????????????
                // for test
                selectedSubdeviceInfo = null;
                updateSpinnerList(response.data);
                log(TAG, "??????????????????????????? " + response.data.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(ARequest aRequest, AError aError) {
            AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
            showToast("???????????????????????????");
            log(TAG, "???????????????????????????");
        }
    };

    private SendListener normalSendListener = new SendListener(normalRegister);
    private SendListener enhanceSendListener = new SendListener(enhanceRegister);

    static class SendListener implements IConnectSendListener{
        WeakReference<IConnectSendListener> handlerWakRef = null;

        public SendListener(IConnectSendListener handler) {
            handlerWakRef = new WeakReference<>(handler);
        }
        @Override
        public void onResponse(ARequest aRequest, AResponse aResponse) {
            if (handlerWakRef != null && handlerWakRef.get() != null) {
                handlerWakRef.get().onResponse(aRequest, aResponse);
            }
        }

        @Override
        public void onFailure(ARequest aRequest, AError aError) {
            if (handlerWakRef != null && handlerWakRef.get() != null) {
                handlerWakRef.get().onFailure(aRequest, aError);
            }
        }
    }

    /**
     * ????????????????????????????????????
     * ?????????????????????????????????
     *
     * @param view
     */
    public void getSubDevices(View view) {
        LinkKit.getInstance().getGateway().gatewayGetSubDevices(getSubDevSendListener);
    }

    private IConnectSendListener getSubDev = new IConnectSendListener() {
        @Override
        public void onResponse(ARequest aRequest, AResponse aResponse) {
            AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? "null" : aResponse.data) + "]");
            showToast("???????????????????????????");
            log(TAG, "??????????????????????????? aResponse=" + (aResponse == null ? "null" : aResponse.data));
            try {
                ResponseModel<List<DeviceInfo>> response = JSONObject.parseObject(aResponse.data.toString(), new TypeReference<ResponseModel<List<DeviceInfo>>>() {
                }.getType());

                subDeviceTripleInfoList.addAll(response.data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(ARequest aRequest, AError aError) {
            AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
            showToast("???????????????????????????");
            log(TAG, "???????????????????????????");
        }
    };

    private SendListener getSubDevSendListener = new SendListener(getSubDev);

    /**
     * ????????????????????????
     * ?????????????????????????????????????????????????????? deviceSecret ???????????????????????????????????????
     * ???????????? sign = hmac_md5(deviceSecret, clientId123deviceNametestproductKey123timestamp1524448722000)
     *
     * @param view
     */
    public void addSubDevice(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewayAddSubDevice(info, subDevConnectListener);
    }

    static class SubDevConnectListener implements ISubDeviceConnectListener{
        ISubDeviceConnectListener handlerConnectListener = null;

        public SubDevConnectListener(ISubDeviceConnectListener handler) {
            handlerConnectListener = handler;
        }

        @Override
        public String getSignMethod() {
            if (handlerConnectListener != null) {
                return handlerConnectListener.getSignMethod();
            }
            return null;
        }

        @Override
        public String getSignValue() {
            if (handlerConnectListener != null) {
                return handlerConnectListener.getSignValue();
            }
            return null;
        }

        @Override
        public String getClientId() {
            if (handlerConnectListener != null) {
                return handlerConnectListener.getClientId();
            }
            return null;
        }

        @Override
        public Map<String, Object> getSignExtraData() {
            if (handlerConnectListener != null) {
                return handlerConnectListener.getSignExtraData();
            }
            return null;
        }

        @Override
        public void onConnectResult(boolean b, ISubDeviceChannel iSubDeviceChannel, AError aError) {
            if (handlerConnectListener != null) {
                handlerConnectListener.onConnectResult(b, iSubDeviceChannel, aError);
            }
        }

        @Override
        public void onDataPush(String s, AMessage aMessage) {
            if (handlerConnectListener != null) {
                handlerConnectListener.onDataPush(s, aMessage);
            }
        }
    }

    /**
     * ???????????????
     *
     * @param view
     */
    private void deleteSubDevice(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewayDeleteSubDevice(info, removeListener);
    }

    private static ISubDeviceRemoveListener removeListener = new ISubDeviceRemoveListener() {
        @Override
        public void onSuceess() {
            AppLog.d(TAG, "onSuceess() called");
            showToast("????????????????????? ");
        }

        @Override
        public void onFailed(AError aError) {
            AppLog.d(TAG, "onFailed() called with: aError = [" + aError + "]");
            showToast("?????????????????????");
        }
    };

    private ISubDeviceConnectListener connectListener = new ISubDeviceConnectListener() {
        @Override
        public String getSignMethod() {
            AppLog.d(TAG, "getSignMethod() called");
            return "hmacsha1";
        }

        @Override
        public String getSignValue() {
            AppLog.d(TAG, "getSignValue() called");
            if (selectedSubdeviceInfo == null) {
                return null;
            }
            Map<String, String> signMap = new HashMap<>();
            signMap.put("productKey", selectedSubdeviceInfo.productKey);
            signMap.put("deviceName", selectedSubdeviceInfo.deviceName);
//                signMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
            signMap.put("clientId", getClientId());
            return SignUtils.hmacSign(signMap, selectedSubdeviceInfo.deviceSecret);
        }

        @Override
        public String getClientId() {
            AppLog.d(TAG, "getClientId() called");
            return "id";
        }

        @Override
        public Map<String, Object> getSignExtraData() {
            return null;
        }

        @Override
        public void onConnectResult(boolean isSuccess, ISubDeviceChannel iSubDeviceChannel, AError aError) {
            AppLog.d(TAG, "onConnectResult() called with: isSuccess = [" + isSuccess + "], iSubDeviceChannel = [" + iSubDeviceChannel + "], aError = [" + aError + "]");
            if (isSuccess) {
                showToast("?????????????????????");
                log(TAG, "????????????????????? " + getPkDn(selectedSubdeviceInfo));
                subDevOnline(null);
            }
        }

        @Override
        public void onDataPush(String s, AMessage message) {
            // new String((byte[]) message.getData())
            // {"method":"thing.service.property.set","id":"184220091","params":{"test":2},"version":"1.0.0"} ??????
            AppLog.d(TAG, "???????????????????????????  onDataPush() called with: s = [" + s + "], s1 = [" + message + "]");
        }
    };

    private SubDevConnectListener subDevConnectListener = new SubDevConnectListener(connectListener);

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param view
     */
    private void subDevOnline(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewaySubDeviceLogin(info, subDeviceActionListener);
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param view
     */
    public void subDevOffline(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("?????????????????????F???????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewaySubDeviceLogout(info, new ISubDeviceActionListener() {
            @Override
            public void onSuccess() {
                showToast("?????????????????????");
                log(TAG, "?????????????????????");
                deleteSubDevice(null);
            }

            @Override
            public void onFailed(AError aError) {
                showToast("?????????????????????");
                log(TAG, "?????????????????????");
                deleteSubDevice(null);
            }
        });
    }

    /**
     * ?????????????????????
     *
     * @param view
     */
    public void subDevSubscribe(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("???????????????????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        String topic = null;
//        for (int i = 0; i < subscribeServiceList.length; i++) {
            topic = subscribeServiceList[0];

            final String tempTopic = topic.replace("{deviceName}", selectedSubdeviceInfo.deviceName)
                    .replace("{productKey}", selectedSubdeviceInfo.productKey);

            LinkKit.getInstance().getGateway().gatewaySubDeviceSubscribe(tempTopic, info, subDeviceActionListener);
//        }
    }

    /**
     * ?????????????????????
     *
     * @param view
     */
    public void subDevPublish(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("???????????????????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        String topic = testPublishTopic.replace("{deviceName}", selectedSubdeviceInfo.deviceName)
                .replace("{productKey}", selectedSubdeviceInfo.productKey);
        String data = publishPayloadET.getText().toString();
        LinkKit.getInstance().getGateway().gatewaySubDevicePublish(topic, data, info, subDeviceActionListener);
    }

    /**
     * ???????????????????????????
     *
     * @param view
     */
    public void subDevUnsubscribe(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("?????????????????????????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        String topic = testSubscribePropertyService.replace("{deviceName}", selectedSubdeviceInfo.deviceName)
                .replace("{productKey}", selectedSubdeviceInfo.productKey);
        LinkKit.getInstance().getGateway().gatewaySubDeviceUnsubscribe(topic, info, subDeviceActionListener);
    }

    private static ISubDeviceActionListener subDeviceActionListener = new ISubDeviceActionListener() {
        @Override
        public void onSuccess() {
            AppLog.d(TAG, "onSuccess() called");
            showToast("???????????????????????????");
        }

        @Override
        public void onFailed(AError aError) {
            AppLog.d(TAG, "onFailed() called with: aError = [" + aError + "]");
            showToast("???????????????????????????");
        }
    };

    private List<DeviceInfo> getSubDevList() {
        return DemoApplication.mDeviceInfoData.subDevice;
    }

    private void subDevDisable(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("????????????????????????????????????????????????????????????");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewaySetSubDeviceDisableListener(info, BaseTemplateActivity.mConnectRrpcListener);
    }

    private String getPkDn(DeviceInfo info) {
        if (info == null) {
            return null;
        }
        return "[pk=" + info.productKey + ",dn=" + info.deviceName + "]";
    }

    public void startSubDeviceControl(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("??????????????????????????????????????????????????????????????????");
            return;
        }
        showToast("?????????????????????????????????????????????????????????");

        final DeviceInfo info = selectedSubdeviceInfo;
        Intent intent = new Intent(this, TSLActivity.class);
        intent.putExtra("pk", info.productKey);
        intent.putExtra("dn", info.deviceName);
        intent.putExtra("sub", true);
        startActivity(intent);
    }
}
