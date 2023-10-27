package com.fortune.tejiebox.bean

data class ShelfDataBean(
    val update_time: Long,
    val huawei_market: Int,
    val isCanUseShare: Int,
    val oppo_market: Int,
    val tencet_market: Int,
    val vivo_market: Int,
    val xiaomi_market: Int,
    val s360_market: Int,
    val baidu_market: Int,
    val s91_market: Int,
    val samsung_market: Int,
    val multipleOpenAppType: Int,//多开应用软件样式
    val ipInfo: Int,//ip信息管理
    val isShowIntegralBtn:Int,//大图标是否展示免费充值
)