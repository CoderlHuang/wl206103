package com.aliyun.alink.devicesdk.manager;

import android.content.Context;

import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.tools.ALog;
import com.aliyun.isoc.aps.DASClient;
import com.aliyun.isoc.aps.DASConnectionStatus;
import com.aliyun.isoc.aps.LKConnection;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author : Jeeking
 * date   : 2020/8/10 9:19 PM
 * desc   :
 */
public class DASHelper {
    private static final String TAG = "DASHelper";

    private DASClient client = null;
    private LKConnection connection = null;
    private AtomicBoolean hasInitedAB = new AtomicBoolean(false);

    private DASHelper() {
    }

    private static class SingletonHolder {
        private final static DASHelper INSTANCE = new DASHelper();
    }

    public static DASHelper getInstance() {
        return DASHelper.SingletonHolder.INSTANCE;
    }

    public void init(Context context, String pk, String dn) {
        ALog.i(TAG, "init() called with: context = [" + context + "], pk = [" + pk + "], dn = [" + dn + "]");
        try {
            if (hasInitedAB.compareAndSet(false, true)) {
                client = DASClient.create(context, pk, dn);
                connection = new LKConnection(LinkKit.getInstance(), client);
                client.setConnection(connection);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ALog.w(TAG, "init das "+ e);
        }
    }

    public void notifyConnectionStatus(ConnectState connectState) {
        ALog.i(TAG, "notifyConnectionStatus() called with: connectState = [" + connectState +
                "], client = [" + client + "]" + ", hasInitedAB = [" + hasInitedAB + "]");
        try {
            if (client != null && hasInitedAB.get()) {
                if (connectState == ConnectState.CONNECTED) {
                    client.onConnectionStatus(DASConnectionStatus.DAS_CONNECTION_CONNECTED);
                } else if (connectState == ConnectState.DISCONNECTED || connectState == ConnectState.CONNECTFAIL) {
                    client.onConnectionStatus(DASConnectionStatus.DAS_CONNECTION_DISCONNECTED);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ALog.w(TAG, "onConnectionStatus das "+ e);
        }
    }

    public void deinit() {
        ALog.i(TAG, "deinit() called");
        if (hasInitedAB.compareAndSet(true, false)) {
            try {
                if (connection != null) {
                    connection.deinit();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                ALog.w(TAG, "deinit das " + e);
            }
            try {
                if (client != null) {
                    client.destroy();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                ALog.w(TAG, "destroy das " + e);
            }
        }
    }
}
