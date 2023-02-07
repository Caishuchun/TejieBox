package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;

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
    }
}
