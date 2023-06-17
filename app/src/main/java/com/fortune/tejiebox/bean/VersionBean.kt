package com.fortune.tejiebox.bean

class VersionBean {

    companion object {
        private var mDate: DataBean? = null

        fun getData() = mDate
        fun setData(data: DataBean) {
            this.mDate = data
        }

        fun clear() {
            this.mDate = null
        }
    }

    /**
     * code : 1
     * msg : success
     * data : {"version_name":"1.0.0","version_number":20210119,"update_type":2,"update_msg":"更新信息\r\n更新信息","update_url":"http://www.baidu,com"}
     */
    private var code: Int? = null
    private var msg: String? = null
    private var data: DataBean? = null

    fun getCode(): Int? {
        return code
    }

    fun setCode(code: Int?) {
        this.code = code
    }

    fun getMsg(): String? {
        return msg
    }

    fun setMsg(msg: String?) {
        this.msg = msg
    }

    fun getData(): DataBean? {
        return data
    }

    fun setData(data: DataBean?) {
        this.data = data
    }

    /**
     * version_name : 1.0.0
     * version_number : 20210119
     * update_type : 2
     * update_msg : 更新信息
     * update_url : 正常普通下载地址
     * update_url2 : 百度推广下载地址,专属名称"特戒"
     * default_page : 默认页
     * channel : 渠道号
     * activity_is_open : 活动是否开启 1开启 0关闭
     * notice : 公告
     * gm_user_fee_duration : 0
     * isShowStartGameBtn: 是否显示开始游戏按钮 1显示 0不显示
     * isCanUseShare: 是否可以使用分享 1可以 0不可以
     */
    class DataBean {
        var version_name: String? = null
        var version_number: Int? = null
        var update_type: Int? = null
        var update_msg: String? = null
        var update_url: String? = null
        var update_url2: String? = null
        var default_page: String? = null
        var channel: Int? = null
        var activity_is_open: Int? = null
        var notice: String? = null
        var gm_user_fee_duration: Int? = null
        var isShowStartGameBtn: Int = 1
        var isCanUseShare: Int = 1
    }
}