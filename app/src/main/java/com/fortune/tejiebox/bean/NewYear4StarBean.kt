package com.fortune.tejiebox.bean

data class NewYear4StarBean(
    val code: Int,
    val `data`: List<Data>,
    val msg: String
) {
    data class Data(
        val task_dis: String,
        val task_speed: Int,
        val task_title: String,
        val task_total: Int
    )
}