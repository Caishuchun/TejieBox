package com.fortune.tejiebox.utils

import android.content.Context
import android.os.Build
import android.os.Process
import com.fortune.tejiebox.bean.ShelfDataBean
import com.snail.antifake.jni.EmulatorDetectUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 用于判断是否多开
 */
object IsMultipleOpenAppUtils {

    /**
     * 是否算是多开
     */
    fun isMultipleOpenApp(context: Context): Boolean {
        ShelfDataBean.getData()?.let {
            if (it.multipleOpenAppType == 1) {
                return isDualApp4System() || isDualApp4App(context) || isDualApp4Ex(context)
            }
        }
        //一棒子打死系列, 只要安装有多开应用的设备都不行
        return checkByOriginApkPackageName(context)
    }

    /**
     * 反系统级应用多开
     */
    private fun isDualApp4System(): Boolean {
        val isDualApp4System = 0 != Process.myUid() / 100000
        LogUtils.d("isMultipleOpenApp==>isDualApp4System=$isDualApp4System")
        return isDualApp4System
    }

    /**
     * 反应用级应用多开
     */
    private fun isDualApp4App(context: Context): Boolean {
        val isDualApp4App = try {
            val dataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.applicationContext.dataDir.toString()
            } else {
                val manager = context.packageManager
                manager.getPackageInfo(context.packageName, 0).applicationInfo.dataDir.toString()
            }
            val file = File(dataDir + File.separator + "..")
            file.canRead()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        LogUtils.d("isMultipleOpenApp==>isDualApp4App=$isDualApp4App")
        return isDualApp4App
    }

    /**
     * 反应用级应用多开加强版
     */
    private fun isDualApp4Ex(context: Context): Boolean {
        val isDualApp4Ex = try {
            val simpleName = "DualApp"
            val dataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.applicationContext.dataDir.toString()
            } else {
                val manager = context.packageManager
                manager.getPackageInfo(context.packageName, 0).applicationInfo.dataDir.toString()
            }
            val testPath = dataDir + File.separator + simpleName
            val fos = FileOutputStream(testPath)
            val fileDescriptor = fos.fd
            val fid_decriptor = fileDescriptor.javaClass.getDeclaredField("descriptor")
            fid_decriptor.isAccessible = true
            //获取fd
            val fd = fid_decriptor.get(fileDescriptor) as Int
            //fd 反查真实路径
            val fdPath = String.format("/proc/self/fd/%d", fd)
            val realPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.readSymbolicLink(Paths.get(fdPath)).toString()
            } else {
                //但版本模拟器都不支持多开应用，所以这里不做兼容设计，直接返回null
                null
            }
            LogUtils.d("isMultipleOpenApp==>realPath=$realPath")
            if (realPath == null) {
                return false
            }
            if (!realPath.startsWith("/data/user/0/") && !EmulatorDetectUtil.isEmulator(context)) {
                //不是正常的data目录
                true
            } else if (realPath.substring(realPath.lastIndexOf(File.separator)) != File.separator + simpleName) {
                // 文件名被修改
                true
            } else {
                //尝试访问真实路径的父目录
                val fatherDirPath = realPath.replace(simpleName, "..")
                LogUtils.d("isMultipleOpenApp==>fatherDirPath=$fatherDirPath")
                File(fatherDirPath).canRead()
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        LogUtils.d("isMultipleOpenApp==>isDualApp4Ex=$isDualApp4Ex")
        return isDualApp4Ex
    }


    /**
     * 维护一份市面多开应用的包名列表
     */
    private val virtualPkgs = arrayOf(
        "com.bly.dkplat",  //多开分身本身的包名
        "dkplugin.pke.nnp",//多开分身克隆应用的包名会随机变换
        "com.by.chaos",  //chaos引擎
        "com.lbe.parallel",  //平行空间
        "com.excelliance.dualaid",  //双开助手
        "com.lody.virtual",  //VirtualXposed，VirtualApp
        "com.qihoo.magic", //360分身大师
        "com.qihoo.magicmutiple", //360分身大师
        "com.qihoo.magic_mutiple", //360分身大师
        "com.qihoo.magic.xposed", //分身大师xposed版
        "com.qihoo.magic.dg3biVWaqVGduUmb1RncvZmLt92Yk_101", //360分身大师
        "cn.chuci.and.wkfenshen",
        "com.boly.wxmultopen",
        "com.excelliance.dualaid",  //双开助手
        "com.excelliance.dualaid.b32", //双开助手32位包
        "com.hy.clone", //幻影分身
        "com.hy.clone.plugin", //幻影分身32位插件
        "com.hy.multiapp.master.wxfs", //无限分身
        "dkplugin.ckb.mrn",
        "info.red.virtual", //悟空分身定位软件
        "com.smallyin.moreopen", //应用分身定位软件
        "com.xuanmutech.fenkai", //应用分身多开软件
        "com.ifreetalk.ftalk", //派派多开分身虚拟定位
        "cn.com.vapp.nxfs", //牛x分身手机版
        "com.sheep2.xyfs", //应用多开分身app
        "com.yizhi.ftd", //双开分身软件
        "com.ziyi18.virtualapp_6", //分身双开软件
        "com.godinsec.godinsec_private_space", //x分身手机版
        "com.parallelspace.multipleaccounts.appclone", //es应用分身
        "com.changyou.helper", //喵分身虚拟定位
        "com.meta.app.fenshen", //超级分身宝
        "com.tyzhzxl.fsqq", //分身qq
        "com.juying.Jixiaomi.fenshen", //机小秘分身
        "com.nox.mopen.app", //夜神多开
        "com.liuniantech.fenshen123", //应用分身
        "com.ludashi.dualspace.cn", //柯柯框架
        "com.boly.jyelves", //机友精灵(软件分身多开)
        "com.droi.adocker", //分身有术
        "com.droi.adocker.pro", //分身有术Pro
        "com.zb.xapp", //分身有力
        "com.cw.super", //微分身
        "com.mtt.douyincompanion", //多开分身助手
        "com.vbooster.vbooster_privace_z_space", //Z分身
        "com.zsl.shadow", //影子分身
        "info.xiangyin.virtua", //哪吒多开分身
        "com.zxqy.vbox", //双开分身助手
        "free.game.video.box.fuo", //时空助手
    )

    /**
     * 检测原始的包名，多开应用会hook处理getPackageName方法
     * 顺着这个思路，如果在应用列表里出现了同样的包，那么认为该应用被多开了
     * @param context
     * @return
     */
    private fun checkByOriginApkPackageName(context: Context): Boolean {
        try {
            val pm = context.packageManager
            val pkgs = pm.getInstalledPackages(0)
            for (info in pkgs) {
                if (virtualPkgs.contains(info.packageName)) {
                    return true
                }
            }
        } catch (ignore: Exception) {
        }
        return false
    }
}

