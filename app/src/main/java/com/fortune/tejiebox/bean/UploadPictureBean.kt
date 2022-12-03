package com.fortune.tejiebox.bean

data class UploadPictureBean(
    val code: Int,
    val msg: String,
    var data: Data,
) {
    data class Data(val path: String, val url: String)
}