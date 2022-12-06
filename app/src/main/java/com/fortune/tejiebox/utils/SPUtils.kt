package com.fortune.tejiebox.utils

import android.content.Context
import android.content.SharedPreferences
import com.fortune.tejiebox.constants.SPArgument
import com.fortune.tejiebox.myapp.MyApp

/**
 * Sp存储工具类
 */

object SPUtils {

    private const val SPFileName = "tejie_sp_config"

    private val sp: SharedPreferences by lazy {
        MyApp.getInstance().getSharedPreferences(SPFileName, Context.MODE_PRIVATE)
    }

    /**
     * 存值
     */
    fun putValue(key: String, value: Any?) = with(sp.edit()) {
        when (value) {
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            is String, null -> putString(key, value?.toString())
            else -> throw IllegalArgumentException("SPUtils putValue is error")
        }
    }.apply()

    private fun getValue(key: String, default: Any?): Any? = with(sp) {
        return@with when (default) {
            is Int -> getInt(key, default)
            is Long -> getLong(key, default)
            is Float -> getFloat(key, default)
            is Boolean -> getBoolean(key, default)
            is String, null -> getString(key, default?.toString())
            else -> throw java.lang.IllegalArgumentException("SPUtils getValue is error")
        }
    }

    /**
     * 取值Int
     */
    fun getInt(key: String, default: Int = 0) = getValue(key, default) as Int

    /**
     * 取值Long
     */
    fun getLong(key: String, default: Long = 0L) = getValue(key, default) as Long

    /**
     * 取值Float
     */
    fun getFloat(key: String, default: Float = 0f) = getValue(key, default) as Float

    /**
     * 取值Boolean
     */
    fun getBoolean(key: String, default: Boolean = false) = getValue(key, default) as Boolean

    /**
     * 取值String?
     */
    fun getString(key: String, default: String? = null) = getValue(key, default) as String?

    /**
     * 移除当前key
     */
    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    /**
     * 清空sp
     */
    fun clear() {
        val isLogined = getBoolean(SPArgument.IS_LOGIN_ED, false)
        val account = getString(SPArgument.LOGIN_ACCOUNT, null)
        val pass = getString(SPArgument.LOGIN_ACCOUNT_PASS, null)
        val isAgree = getBoolean(SPArgument.IS_CHECK_AGREEMENT, false)
        sp.edit().clear().apply()
        if (isLogined) {
            putValue(SPArgument.IS_LOGIN_ED, true)
        }
        if (!account.isNullOrBlank()) {
            putValue(SPArgument.LOGIN_ACCOUNT, account)
        }
        if (!pass.isNullOrBlank()) {
            putValue(SPArgument.LOGIN_ACCOUNT_PASS, pass)
        }
        if (isAgree) {
            putValue(SPArgument.IS_CHECK_AGREEMENT, true)
        }
    }

}