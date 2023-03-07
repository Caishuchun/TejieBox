package com.fortune.tejiebox.bean

data class GameInfo4ClipboardBean(
    val channelId: String,
    val account: String,
    val password: String,
    val version: String,
    val serviceId: String,
    val roleId: String
) {
    companion object {
        private var mData: GameInfo4ClipboardBean? = null
        fun setData(data: GameInfo4ClipboardBean?) {
            this.mData = data
        }

        fun getData() = mData
    }
}