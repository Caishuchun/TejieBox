package com.fortune.tejiebox.base

/**
 * 配置文件
 */

object BaseAppUpdateSetting {
    /**
     * 是否打印日志
     * @param true 打印debug的日志(平常测试的时候)
     * @param false 不打印(上正式区发布的时候)
     */
    const val isDebug = true

    /**
     * 测试区或者正式区
     * @param true 正式区
     * @param false 测试区
     */
    const val appType = true

    /**
     * 上架市场渠道
     * @param 0 代表默认
     * @param 1 代表应用宝
     * @param 2 代表华为
     * @param 3 代表小米
     * @param 4 代表vivo
     * @param 5 代表oppo
     */
    const val marketChannel = 0

    /**
     * 网络请求标志, 热更包版本
     * @param patch ""表示基础版本,".1"/".11"表示热更版本
     */
    const val patch = ""

    /**
     * 是否是推广版本
     * @suppress 记得修改string.xml中的应用名
     */
    const val isToPromoteVersion = false

    /**
     * 是否是审核版本
     */
    const val isToAuditVersion = false
}