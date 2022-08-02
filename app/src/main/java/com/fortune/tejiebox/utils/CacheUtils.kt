package com.fortune.tejiebox.utils

import android.app.Activity
import com.fortune.tejiebox.myapp.MyApp
import java.io.File
import java.text.DecimalFormat

/**
 * 缓存工具类
 */
object CacheUtils {
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): String {
        return formatFileSize(
            getFileSize(MyApp.getInstance().externalCacheDir!!)
                    + getFileSize(MyApp.getInstance().cacheDir)
                    + getFileSize(MyApp.getInstance().codeCacheDir)
        )
    }

    /**
     * 清理缓存
     */
    fun clearCache(activity: Activity): Boolean {
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)

        try {
            val externalCacheDir = MyApp.getInstance().externalCacheDir
            val success1 =
                if (externalCacheDir != null && externalCacheDir.isDirectory && !externalCacheDir.length()
                        .equals(0)
                ) {
                    deleteDir(externalCacheDir)
                } else {
                    false
                }
            val cacheDir = MyApp.getInstance().cacheDir
            val success2 =
                if (cacheDir != null && cacheDir.isDirectory && !cacheDir.length().equals(0)
                ) {
                    deleteDir(cacheDir)
                } else {
                    false
                }
            val codeCacheDir = MyApp.getInstance().codeCacheDir
            val success3 =
                if (codeCacheDir != null && codeCacheDir.isDirectory && !codeCacheDir.length()
                        .equals(0)
                ) {
                    deleteDir(codeCacheDir)
                } else {
                    false
                }
            return success1 && success2 && success3
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 删除文件夹
     */
    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val listFiles = dir.list()
            for (childFile in listFiles) {
                val success = deleteDir(File(dir, childFile))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }


    /**
     * 格式文件大小
     */
    private fun formatFileSize(size: Long): String {
        if (size == 0L) {
            return "0KB"
        }
        val decimalFormat = DecimalFormat("#.00")
        return when {
            size < 1024L -> "${decimalFormat.format(size.toDouble())} B"
            size < 1024L * 1024L -> "${decimalFormat.format(size.toDouble() / 1024)} KB"
            size < 1024L * 1024L * 1024L -> "${decimalFormat.format(size.toDouble() / 1024 / 1024)} MB"
            size < 1024L * 1024L * 1024L * 1024L -> "${decimalFormat.format(size.toDouble() / 1024 / 1024 / 1024)} GB"
            else -> "未知"
        }
    }

    /**
     * 获取文件大小
     */
    private fun getFileSize(file: File): Long {
        var size = 0L
        val listFiles = file.listFiles()
        for (childFile in listFiles) {
            size += if (childFile.isDirectory) {
                getFileSize(childFile)
            } else {
                childFile.length()
            }
        }
        return size
    }
}