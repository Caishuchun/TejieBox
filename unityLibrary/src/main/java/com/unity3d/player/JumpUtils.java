package com.unity3d.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.xinyu.deviceutils.DevicesUtils;

/**
 * 跳转游戏工具类
 */
public class JumpUtils {
    public static void jump2Game(Activity context, String channel, String UIId) {
        String uiId = "";
        if (null != UIId && !UIId.equals("")) {
            uiId = "|loggerres=" + UIId;
        }
//        uiId = "|loggerres=0/_sys/logger4";
        Intent intent = new Intent(context, DevicesUtils.class);
        intent.putExtra("hfdd", "fromBox=true|channel=" + channel + uiId);
        context.startActivity(intent);

        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        System.out.println("==================手机" + manufacturer + ",型号" + model);
        if (manufacturer != null && manufacturer.toLowerCase().equals("samsung")) {
            //三星手机
            Message message = new Message();
            message.obj = context;
            mHandler.sendMessageDelayed(message, 500);
        }
    }

    @SuppressLint("HandlerLeak")
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Activity context = (Activity) msg.obj;
            Intent intent2 = new Intent(context, UnitySplashActivity.class);
            context.startActivity(intent2);
        }
    };
}
