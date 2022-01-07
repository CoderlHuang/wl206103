package com.aliyun.alink.devicesdk.demo;

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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.aliyun.alink.devicesdk.app.AppLog;
import com.aliyun.alink.devicesdk.manager.ToastUtils;
import com.aliyun.alink.dm.api.IOta;
import com.aliyun.alink.dm.api.OtaInfo;
import com.aliyun.alink.dm.api.ResultCallback;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.tools.ALog;

import java.io.File;
import java.util.Map;

public class OTAActivity extends BaseActivity implements IOta.OtaListener {
    private static final String TAG = "OTAActivity";

    private IOta mOta;
    private EditText mText;
    private OtaInfo mInfo;
    private int mProgress;
    private IOta.OtaConfig mConfig;
    private OtaInfo otaInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ota);

        mText = findViewById(R.id.text);

        init();
    }

    void init() {
        mOta = (IOta) LinkKit.getInstance().getOta();
    }

    public void startOta(View view) {
        String version = "";
        if (!TextUtils.isEmpty(mText.getText())) {
            version = mText.getText().toString();
        }

        if (TextUtils.isEmpty(version)) {
            showToast("版本 为空");
            return;
        }

        log(TAG, "reportVersion:" + version);
        final String finalVersion = version;

        File apkDir = new File(getCacheDir(), "apk");
        apkDir.mkdirs();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            apkDir = Environment.getExternalStorageDirectory();
        }
        final String filePath = new File(apkDir, finalVersion + ".apk").getPath();
        mConfig = new IOta.OtaConfig();
        mConfig.otaFile = new File(filePath);
        mConfig.deviceVersion = finalVersion;
        mOta.tryStartOta(mConfig, this);
    }

    private static ResultCallback<String> textCallback = new ResultCallback<String>() {
        @Override
        public void onRusult(int error, String s) {
            String text = "上报" + ((error == ResultCallback.SUCCESS) ? "成功" : "失败");
//            showToast(text);
            AppLog.d(TAG, text);
        }
    };

    @Override
    public boolean onOtaProgress(int step, IOta.OtaResult otaResult) {
        int code = otaResult.getErrorCode();
        Object data = otaResult.getData();
        Map extra = otaResult.getExtData();
        if (data instanceof Integer && mProgress != (int)data) {
            AppLog.d(TAG, "code:" + code + " data:" + data + " extra:" + extra);
        }
        if (code != IOta.NO_ERROR) {
            AppLog.e(TAG, "onOtaProgress error:" + code);
            // show tip for uses.
            return false;
        }

        switch (step) {
            case IOta.STEP_SUBSCRIBE:
                AppLog.d(TAG, "STEP_SUBSCRIBE");
                break;

            case IOta.STEP_RCVD_OTA:
                AppLog.d(TAG, "STEP_RCVD_OTA");
                otaInfo = (OtaInfo) otaResult.getData();
                break;
            case IOta.STEP_DOWNLOAD:
                AppLog.d(TAG, "STEP_DOWNLOAD");
                if (data instanceof Integer) {
                    int progress = (int) data;
                    if (mProgress != progress) {
                        mProgress = progress;
                        if (mProgress % 10 == 0) {
                            mOta.reportProgress(progress, "desc", textCallback);
                        }
                    }

                    if (100 == progress) {
                        if (otaInfo != null && otaInfo.isDiff == 1) {
                            ToastUtils.showToast("差分包，不执行安装");
                            // 测试代码，apk才要走以下逻辑

                        } else {
                            try {
                                installApk(mConfig.otaFile.getPath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;

            case IOta.STEP_REPORT_VERSION:
                AppLog.d(TAG, "STEP_REPORT_VERSION");
                break;
        }
        return true;
    }


    /**
     * 如果需要定制化上报的进度，比如下载占50%，其它流程占50%，可自行调用上报进度接口
     * 在本demo示例，已下载的进度作为升级的进度
     *
     * @param view
     */
    public void reportOtaProgress(View view) {
        int progress = 77;
        try {
            if (!TextUtils.isEmpty(mText.getText())) {
                progress = Integer.parseInt(mText.getText().toString());
            }
        } catch (Exception e) {
            showToast("进度不合法");
            return;
        }
        mOta.reportProgress(progress, "desc", textCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mOta.tryStopOta();
    }

    void installApk(String apkPath) {
        File apkFile = new File(apkPath);

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri contentUri = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            apkFile.setReadable(true);
            contentUri = Uri.fromFile(apkFile);
        } else {
            contentUri = FileProvider.getUriForFile(OTAActivity.this, "com.aliyun.alink.devicesdk.demo.auth_fileprovider", apkFile);
        }

        install.setDataAndType(contentUri, "application/vnd.android.package-archive");
        install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(install);
    }
}
