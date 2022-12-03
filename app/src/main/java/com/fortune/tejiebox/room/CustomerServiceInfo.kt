package com.fortune.tejiebox.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 客服聊天数据
 */
@Entity(tableName = "customer_service_table")
class CustomerServiceInfo(
    //消息id
    @ColumnInfo(name = "chat_id") val chat_id: Int,

    //内容来源 0-客服 1-用户
    @ColumnInfo(name = "form") val form: Int,

    //消息类型 1-文本信息 2-图片信息
    @ColumnInfo(name = "chat_type") val chat_type: Int,

    //文本信息
    @ColumnInfo(name = "chat_content") var chat_content: String?,

    //图片Url
    @ColumnInfo(name = "chat_img_url") var chat_img_url: String?,

    //图片宽
    @ColumnInfo(name = "imgW") val imgW: Int?,

    //图片高度
    @ColumnInfo(name = "imgH") val imgH: Int?,

    //时间戳
    @ColumnInfo(name = "chat_time") val chat_time: Long,

    //是否是已读消息 0-未读 1-已读
    @ColumnInfo(name = "is_read") var is_read: Int,

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: Long = System.currentTimeMillis()
)