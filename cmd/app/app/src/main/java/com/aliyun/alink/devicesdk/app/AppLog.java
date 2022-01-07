package com.aliyun.alink.devicesdk.app;

import android.util.Log;

import com.aliyun.alink.linksdk.tools.log.HLoggerFactory;
import com.aliyun.alink.linksdk.tools.log.ILogger;


public class AppLog {
    private static final String PRE_TAG = "LK--";
    private static ILogger logger = new HLoggerFactory().getInstance(PRE_TAG);

    public static final int ASSERT = Log.ASSERT;
    public static final int DEBUG = Log.DEBUG;
    public static final int ERROR = Log.ERROR;
    public static final int INFO = Log.INFO;
    public static final int VERBOSE = Log.VERBOSE;
    public static final int WARN = Log.WARN;

    static public void d(String tag, String msg) {
        logger.d(tag, getFilterString(msg));
    }

    static public void i(String tag, String msg) {
        logger.i(tag, getFilterString(msg));
    }

    static public void w(String tag, String msg) {
        logger.w(tag, getFilterString(msg));
    }

    static public void e(String tag, String msg) {
        logger.e(tag, getFilterString(msg));
    }

    static public void e(String tag, String where, Exception ex) {
        if (null != ex) {
            logger.e(tag, getFilterString(where) + " EXCEPTION: " + ex.getMessage());
            ex.printStackTrace();
        } else {
            logger.e(tag, getFilterString(where) + " EXCEPTION: unknown");
        }
    }

    public static void llog(byte priority, String tag, String msg) {
        com.aliyun.alink.linksdk.tools.ALog.llog(priority, PRE_TAG + tag, getFilterString(msg));
    }

    public static void setLevel(byte level) {
        com.aliyun.alink.linksdk.tools.ALog.setLevel(level);
    }

    private static String getFilterString(String msg) {
        return msg;
    }
}
