package com.fortune.tejiebox.room

import androidx.room.*

/**
 * 客服聊天数据库Dao
 */
@Dao
interface CustomerServiceInfoDao {
    /**
     * 查询全部数据
     */
    @get:Query("SELECT * FROM customer_service_table")
    val all: List<CustomerServiceInfo>

    /**
     * 查询最新数据
     */
    @Query("SELECT * FROM (SELECT * FROM customer_service_table ORDER BY id DESC LIMIT :maxNum) tempTable ORDER BY id")
    fun getInfo(maxNum: Int = 15): List<CustomerServiceInfo>

    /**
     * 添加数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addInfo(info: CustomerServiceInfo)

    /**
     * 更新数据
     */
    @Update
    fun update(info: CustomerServiceInfo)

    /**
     * 删除数据
     */
    @Delete
    fun deleteInfo(info: CustomerServiceInfo)

    /**
     * 清空数据库
     */
    @Query("DELETE FROM customer_service_table")
    fun clear()
}