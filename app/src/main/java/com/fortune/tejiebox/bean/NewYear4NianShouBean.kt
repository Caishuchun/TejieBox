package com.fortune.tejiebox.bean

data class NewYear4NianShouBean(
    val code: Int,
    val `data`: Data,
    val msg: String
) {
    data class Data(
        var current_blood: Int,
        var current_energy: Int,
        val end_time: Long,
        val start_time: Long,
        val total_blood: Int,
        val treasure_list: List<Treasure>
    ) {
        data class Treasure(
            val blood: Int,
            val give_num: Int,
            var receive_state: Int,
            val treasure_id: Int
        )
    }
}