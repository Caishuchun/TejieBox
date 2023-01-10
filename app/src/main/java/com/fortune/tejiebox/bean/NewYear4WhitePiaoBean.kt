package com.fortune.tejiebox.bean

data class NewYear4WhitePiaoBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        val end_time: Long,
        val start_time: Long,
        val status_list: List<Status>
    ) {
        data class Status(
            val activity_msg: String,
            val activity_state: Int,
            val activity_title: String,
            val current_time: Int,
            val date: String,
            val give_num: Int,
            val total_time: Int,
            var activity_sort: Int
        )
    }
}