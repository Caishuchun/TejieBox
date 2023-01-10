package com.fortune.tejiebox.bean

data class NewYear4InviteBean(
    val code: Int,
    val msg: String,
    val data: Data
) {
    data class Data(
        val start_time: Long,
        val end_time: Long,
        val share_link: String,
        val total_time: Int,
        val treasure_list: List<Treasure>
    ) {
        data class Treasure(
            val treasure_id: Int,
            val invitation_num: Int,
            var receive_state: Int,
            val give_num: Int,
        )
    }
}