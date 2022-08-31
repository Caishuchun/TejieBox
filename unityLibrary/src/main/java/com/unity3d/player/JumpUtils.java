package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;

import com.xinyu.deviceutils.DevicesUtils;

public class JumpUtils {
    public static void jump2Game(Activity context, String channel) {
        Intent intent = new Intent(context, DevicesUtils.class);
        intent.putExtra("hfdd", "fromBox=true|channel=" + channel);
        context.startActivity(intent);
    }
}
