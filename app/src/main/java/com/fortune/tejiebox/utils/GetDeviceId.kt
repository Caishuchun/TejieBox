package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.telephony.TelephonyManager
import com.fortune.tejiebox.constants.SPArgument
import java.io.*
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.*

object GetDeviceId {

    /**
     * 获取设备唯一标识符
     * @param context
     * @return
     */
    fun getDeviceId(context: Context): String {
        //先读Sp
        var deviceId = SPUtils.getString(SPArgument.ONLY_DEVICE_ID_NEW, null)
        if (deviceId != null) {
            return deviceId
        }
        //读取保存的在sd卡中的唯一标识符
        deviceId = readDeviceID4File()
        //用于生成最终的唯一标识符
        val s = StringBuffer()
        //判断是否已经生成过,
        if (deviceId != null && "" != deviceId) {
            return deviceId
        }
        try {
            //获取IMES(也就是常说的DeviceId)
            deviceId = getIMIEStatus(context)
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            //获取设备的MACAddress地址 去掉中间相隔的冒号
            deviceId = getLocalMac(context).replace(":", "")
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //如果以上搜没有获取相应的则自己生成相应的UUID作为相应设备唯一标识符
        if (s.isEmpty()) {
            val uuid = UUID.randomUUID()
            deviceId = uuid.toString().replace("-", "")
            s.append(deviceId)
        }
        s.append(System.currentTimeMillis().toString())
        //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        val md5 = getMD5(s.toString(), false)
        val onlyDeviceId = "android-$md5"
        if (s.isNotEmpty()) {
            //持久化操作, 进行保存到SD卡中
            writeDeviceID2File(onlyDeviceId)
            SPUtils.putValue(SPArgument.ONLY_DEVICE_ID_NEW, onlyDeviceId)
        }
        return onlyDeviceId
    }

    /**
     * 从文件中获取设备Id字符串
     */
    private fun readDeviceID4File(): String? {
        return try {
            val path = "${Environment.getExternalStorageDirectory()}/.tjDeviceId.txt"
            LogUtils.d("+++++++++++++++++readDeviceID4File=>path:$path")
            val deviceIdFile = File(path)
            if (deviceIdFile.exists()) {
                val bufferedReader = BufferedReader(FileReader(path))
                val deviceId = bufferedReader.readLine()
                LogUtils.d("+++++++++++++++++readDeviceID4File=>deviceId:$deviceId")
                bufferedReader.close()
                deviceId
            } else {
                LogUtils.d("+++++++++++++++++readDeviceID4File=>no file")
                null
            }
        } catch (e: Exception) {
            LogUtils.d("+++++++++++++++++readDeviceID4File=>exception:${e.message}")
            null
        }
    }

    /**
     * 将deviceId存入本地
     */
    private fun writeDeviceID2File(deviceId: String) {
        try {
            val path = "${Environment.getExternalStorageDirectory()}/.tjDeviceId.txt"
            LogUtils.d("+++++++++++++++++writeDeviceID2File=>path:$path")
            val deviceIdFile = File(path)
            if (!deviceIdFile.exists()) {
                deviceIdFile.createNewFile()
            }
            val bufferedWriter = BufferedWriter(FileWriter(path))
            bufferedWriter.write(deviceId)
            bufferedWriter.close()
            LogUtils.d("+++++++++++++++++writeDeviceID2File=>over")
        } catch (e: Exception) {
            LogUtils.d("+++++++++++++++++writeDeviceID2File=>exception:${e.message}")
        }
    }

    /**
     * 获取设备的DeviceId(IMES) 这里需要相应的权限
     * 需要 READ_PHONE_STATE 权限
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    private fun getIMIEStatus(context: Context): String {
        val tm = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.deviceId
    }

    /**
     * 获取设备MAC 地址 由于 6.0 以后 WifiManager 得到的 MacAddress得到都是 相同的没有意义的内容
     * 所以采用以下方法获取Mac地址
     * @param context
     * @return
     */
    private fun getLocalMac(context: Context): String {
        var macAddress: String? = null
        val buf = StringBuffer()
        var networkInterface: NetworkInterface? = null
        try {
            networkInterface = NetworkInterface.getByName("eth1")
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0")
            }
            if (networkInterface == null) {
                return ""
            }
            val addr = networkInterface.hardwareAddress
            for (b in addr) {
                buf.append(String.format("%02X:", b))
            }
            if (buf.isNotEmpty()) {
                buf.deleteCharAt(buf.length - 1)
            }
            macAddress = buf.toString()
        } catch (e: SocketException) {
            e.printStackTrace()
            return ""
        }
        return macAddress
    }

    /**
     * 对挺特定的 内容进行 md5 加密
     * @param message 加密明文
     * @param upperCase 加密以后的字符串是是大写还是小写 true 大写 false 小写
     * @return
     */
    fun getMD5(message: String, upperCase: Boolean): String {
        var md5str = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val input = message.toByteArray()
            val buff = md.digest(input)
            md5str = bytesToHex(buff, upperCase)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return md5str
    }

    /**
     * byte转16进制
     * @param bytes     byte数组
     * @param upperCase 大小写
     */
    private fun bytesToHex(bytes: ByteArray, upperCase: Boolean): String {
        val md5str = StringBuffer()
        var digital: Int
        for (i in bytes.indices) {
            digital = bytes[i].toInt()
            if (digital < 0) {
                digital += 256
            }
            if (digital < 16) {
                md5str.append("0")
            }
            md5str.append(Integer.toHexString(digital))
        }
        return if (upperCase) {
            md5str.toString().toUpperCase(Locale.ROOT)
        } else md5str.toString().toLowerCase(Locale.ROOT)
    }
}