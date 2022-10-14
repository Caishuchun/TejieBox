package com.fortune.tejiebox.utils

import android.content.Context
import com.fortune.tejiebox.base.BaseAppUpdateSetting
import com.fortune.tejiebox.constants.SPArgument
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.UMCrash

/**
 * 友盟初始化工具类
 */
object UMUtils {

    /**
     * 初始化
     */
    fun init(context: Context) {
        //获取本地是否有存储渠道号,没有存储直接退出,有存储进行下一步
        val uMChannelId = SPUtils.getString(SPArgument.UM_CHANNEL_ID, null) ?: return

        //友盟统计的channel为了和之前保持一致,当后台返回的渠道为100则默认是之前的,否则就是新渠道
        val channel = if (uMChannelId == "100") null else "tejie_$uMChannelId"

        UMConfigure.init(
            context, "62bd575605844627b5d180c2", channel,
            UMConfigure.DEVICE_TYPE_PHONE, ""
        )
        UMConfigure.setLogEnabled(BaseAppUpdateSetting.isDebug)
        UMCrash.registerUMCrashCallback {
            return@registerUMCrashCallback "UMCrash_TejieBox"
        }
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
        UMConfigure.setProcessEvent(true)
    }
}