package com.fortune.tejiebox.bean

data class ShelfDataBean(
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
    val multipleOpenAppType: Int
) {
    companion object {
        private var shelfDataBean: ShelfDataBean? = null

        fun setData(shelfDataBean: ShelfDataBean) {
            this.shelfDataBean = shelfDataBean
        }

        fun getData(): ShelfDataBean? = shelfDataBean
    }

}