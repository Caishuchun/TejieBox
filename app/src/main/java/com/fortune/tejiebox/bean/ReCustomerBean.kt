package com.fortune.tejiebox.bean

data class ReCustomerBean(
    val code: Int,
    val msg: String,
    val data: List<Data>
) {
    data class Data(
        val id: Int,
        val content: String,
        val type: Int,
        val img_height: Int?,
        val img_width: Int?
    )
}
