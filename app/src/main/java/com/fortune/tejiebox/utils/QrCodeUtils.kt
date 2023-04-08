package com.fortune.tejiebox.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

object QrCodeUtils {
    /**
     * 生成简单二维码
     *
     * @param content                字符串内容
     * @param width                  二维码宽度
     * @param height                 二维码高度
     * @param characterSet           编码方式（一般使用UTF-8）
     * @param errorCorrectionLevel   容错率 L：7% M：15% Q：25% H：35%
     * @param margin                 空白边距（二维码与边框的空白区域）
     * @param colorBlack             黑色色块
     * @param colorWhite             白色色块
     * @return BitMap
     */
    fun createQRCode(
        content: String?,
        width: Int,
        height: Int,
        characterSet: String?,
        errorCorrectionLevel: String?,
        margin: String?,
        colorBlack: Int,
        colorWhite: Int
    ): Bitmap? {
        // 字符串内容判空
        if (TextUtils.isEmpty(content)) {
            return null
        }
        // 宽和高>=0
        if (width < 0 || height < 0) {
            return null
        }
        return try {
            /** 1.设置二维码相关配置  */
            val hints =
                Hashtable<EncodeHintType, String?>()
            // 字符转码格式设置
            if (!TextUtils.isEmpty(characterSet)) {
                hints[EncodeHintType.CHARACTER_SET] = characterSet
            }
            // 容错率设置
            if (!TextUtils.isEmpty(errorCorrectionLevel)) {
                hints[EncodeHintType.ERROR_CORRECTION] = errorCorrectionLevel
            }
            // 空白边距设置
            if (!TextUtils.isEmpty(margin)) {
                hints[EncodeHintType.MARGIN] = margin
            }
            /** 2.将配置参数传入到QRCodeWriter的encode方法生成BitMatrix(位矩阵)对象  */
            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值  */
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    //bitMatrix.get(x,y)方法返回true是黑色色块，false是白色色块
                    if (bitMatrix[x, y]) {
                        //黑色色块像素设置，可以通过传入不同的颜色实现彩色二维码，例如Color.argb(1,55,206,141)等设置不同的颜色。
                        pixels[y * width + x] = colorBlack
                    } else {
                        // 白色色块像素设置
                        pixels[y * width + x] = colorWhite
                    }
                }
            }
            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,并返回Bitmap对象  */
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    /***
     * 为二维码添加logo
     *
     * @param srcBitmap 二维码图片
     * @param logoBitmap logo图片
     * @param percent logo比例
     * @return 生成的最终的图片
     */
    fun createQRCodeWithLogo(srcBitmap: Bitmap?, logoBitmap: Bitmap?, percent: Float): Bitmap? {
        //判断参数是否正确
        if (srcBitmap == null)
            return null
        if (logoBitmap == null)
            return srcBitmap
        //输入logo图片比例错误自动纠正为默认的0.2f
        var logoPercent = percent
        if (percent < 0 || percent > 1)
            logoPercent = 0.2f

        //分别获取bitmap图片的大小
        val sHeight = srcBitmap.height
        val sWidth = srcBitmap.width
        val lHeight = logoBitmap.height
        val lWidth = logoBitmap.width

        //获取缩放比例
        val scareWidth = sHeight * logoPercent / lWidth
        val scareHeight = sWidth * logoPercent / lHeight

        //使用canvas重新绘制bitmap
        val bitmap = Bitmap.createBitmap(sWidth, sHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(srcBitmap, 0f, 0f, null)
        canvas.scale(
            scareWidth,
            scareHeight,
            (sWidth / 2).toFloat(),
            (sHeight / 2).toFloat()
        )   //设置缩放中心基点
        canvas.drawBitmap(
            logoBitmap,
            (sWidth / 2 - lWidth / 2).toFloat(),
            (sHeight / 2 - lHeight / 2).toFloat(),
            null
        )
        return bitmap
    }
}