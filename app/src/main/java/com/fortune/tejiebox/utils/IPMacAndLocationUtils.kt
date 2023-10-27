package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.Settings
import com.fortune.tejiebox.bean.IPMacAndLocationBean
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


/**
 * ip地址和位置信息
 */
object IPMacAndLocationUtils {

    /**
     * 获取最终结果
     */
    fun getResult(activity: Activity, listener: OnIpMacAndLocationListener) {
        val data = ShelfDataUtils.getShelfData4Local()
        if(data == null){
            listener.success()
            return
        }
        if(data.ipInfo == 0){
            listener.success()
            return
        }
        Thread {
            val url = URL("http://ip-api.com/json/")
            try {
                val inputStream = url.openStream()
                val reader = BufferedReader(InputStreamReader(inputStream, "gbk"))
                var line: String?
                val sb = StringBuffer()
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                val html = sb.toString()
                inputStream.close()
                reader.close()


                val ipMacAndLocation = Gson().fromJson(html, IPMacAndLocationBean::class.java)
                activity.runOnUiThread {
                    if (ipMacAndLocation != null) {
                        if (ipMacAndLocation.countryCode.isEmpty()) {
                            listener.fail(3, "CountryCode is invalid")
                        } else if (!ipMacAndLocation.lat.isFinite() || !ipMacAndLocation.lon.isFinite()) {
                            listener.fail(3, "Lat or lon is invalid")
                        } else {
                            val country = ipMacAndLocation.countryCode
                            if (country == "CN" || country == "TW" || country == "HK" || country == "MO") {
                                val location = getLocation(activity)
                                if (location == null) {
                                    listener.fail(0, "Can get location!")
                                } else {
                                    val distance = getDistance(
                                        location.longitude,
                                        location.latitude,
                                        ipMacAndLocation.lon,
                                        ipMacAndLocation.lat
                                    )
                                    LogUtils.d("==============ipMacAndLocation:$ipMacAndLocation")
                                    LogUtils.d("==============location:$location")
                                    LogUtils.d("==============distance:$distance")
                                    if (distance > 1000 * 2) {
                                        listener.fail(4, "Inconsistent IP and location")
                                    } else {
                                        listener.success()
                                    }
                                }
                            } else {
                                listener.fail(3, "CountryCode is not in China")
                            }
                        }
                    } else {
                        listener.fail(1, "FormatInfo is invalid")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    listener.fail(2, "Exception: ${e.message}")
                }
            }
        }.start()
    }


    /**
     * 根据坐标返回单位是米
     */
    private fun getDistance(
        longitude1: Double, latitude1: Double,
        longitude2: Double, latitude2: Double
    ): Double {
        val Lat1 = rad(latitude1)
        val Lat2 = rad(latitude2)
        val a = Lat1 - Lat2
        val b = rad(longitude1) - rad(longitude2)
        var s = 2 * Math.asin(
            Math.sqrt(
                Math.pow(Math.sin(a / 2), 2.0)
                        + (Math.cos(Lat1) * Math.cos(Lat2)
                        * Math.pow(Math.sin(b / 2), 2.0))
            )
        )
        s = s * 6378137.0
        s = (Math.round(s * 10000) / 10000).toDouble()
        return s
    }

    private fun rad(d: Double): Double {
        return d * Math.PI / 180.0
    }


    /**
     * ip和位置最终结果的回调
     */
    interface OnIpMacAndLocationListener {
        fun success()
        fun fail(code: Int, errorMessage: String)
    }


    private var mLocationManager: LocationManager? = null
    private var mProvider: String? = null

    /**
     * 初始化位置信息
     */
    @SuppressLint("MissingPermission")
    fun initLocation(activity: Activity, listener: OnIpMacAndLocationListener?) {
        try {
            mLocationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!canGetLocation()) {
                listener?.fail(0, "Can get location!")
                return
            }
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_FINE //高精度
            criteria.isAltitudeRequired = false //不要求海拔
            criteria.isBearingRequired = false //不要求方位
            criteria.isCostAllowed = true //允许有花费
            criteria.powerRequirement = Criteria.POWER_LOW //低功耗
            // 从可用的位置提供器中，匹配以上标准的最佳提供器
            mProvider = mLocationManager?.getBestProvider(criteria, true)

            LogUtils.d("==============mLocationListener=>mProvider:$mProvider")
            mLocationManager?.requestLocationUpdates(
                mProvider!!, 1000L, 0f, mLocationListener
            )
        } catch (e: Exception) {
            listener?.fail(0, "Can get location! e:${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    /**
     * 清理监听
     */
    fun clearListener(){
        mLocationManager?.removeUpdates(mLocationListener)
    }

    private fun canGetLocation(): Boolean {
        var canGetLocation = false
        if (mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || mLocationManager?.isProviderEnabled
                (LocationManager.NETWORK_PROVIDER) ==true
        ) {
            canGetLocation = true
        }
        return canGetLocation
    }

    /**
     * 位置变化的监听
     */
    private val mLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            val latitude = location.latitude
            val longitude = location.longitude

            LogUtils.d("==============mLocationListener=>latitude:$latitude,longitude:$longitude")
        }


    }

    /**
     * 获取最新位置信息
     */
    @SuppressLint("MissingPermission")
    fun getLocation(activity: Activity): Location? {
        var location: Location? = null
        if (!canGetLocation()) {
            openLocationService(activity)
            return null
        }
        try {
            location = mLocationManager?.getLastKnownLocation(mProvider!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    /**
     * 获取维度
     */
    fun getLatitude(activity: Activity) = getLocation(activity)?.latitude

    /**
     * 获取经度
     */
    fun getLongitude(activity: Activity) = getLocation(activity)?.longitude

    /**
     * 开启位置服务
     */
    fun openLocationService(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        activity.startActivity(intent)
    }
}