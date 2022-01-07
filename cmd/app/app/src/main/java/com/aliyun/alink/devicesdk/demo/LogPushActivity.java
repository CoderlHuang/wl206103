package com.aliyun.alink.devicesdk.demo;


import android.util.Log;
import android.view.View;

import com.aliyun.alink.devicesdk.app.AppLog;
import com.aliyun.alink.dm.api.LogManager;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.tools.AError;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogPushActivity extends BaseTemplateActivity {
    private static final String TAG = LogPushActivity.class.getSimpleName();
    private String sendLog = "{" + "\"id\" : 123," + "\"version\":\"1.0\"," + "\"params\" :[{" + "\"utcTime\": \"" + getStringDate() +
            "\"," + "\"logLevel\": \"ERROR\"," + "\"module\": \"ALK-LK\"," +
            "\"code\" :\"\"," + "\"traceContext\": \"123456\"," + "\"logContent\" : \"some log content\"" +
            "}]," + "\"method\" : \"thing.log.post\"" + "}";
    private static final String SUCCESS = "成功";
    private static final String FAILED = "失败";

    @Override
    protected void initViewData() {
        AppLog.e(TAG, "test");
        funcTV1.setText("上传消息");
        funcET1.setText("test");
        funcBT1.setText("上传");

        funcRL2.setVisibility(View.GONE);

//        funcTV2.setText("上传消息");
//        funcET2.setText("test");
//        funcBT2.setText("上传");
    }

    @Override
    protected void onFunc1Click() {
        String getData = funcET1.getText().toString();
        LogManager.RecLog recLog = new LogManager.RecLog();
        recLog.setMsg(getData);
        recLog.setLogLevel(Log.ERROR);
        LinkKit.getInstance().postLog(recLog, new IConnectSendListener() {
            @Override
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                showToast(SUCCESS);
            }

            @Override
            public void onFailure(ARequest aRequest, AError aError) {
                showToast(FAILED);
            }
        });
    }

    @Override
    protected void onFunc2Click() {

    }

    private static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }
}
