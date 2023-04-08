package com.fortune.tejiebox.utils

import android.annotation.SuppressLint
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@SuppressLint("NewApi")
object AESUtils {
    // 加密头
    private val HEAD = byteArrayOf(
        'T'.toByte(), 'J'.toByte(), 'B'.toByte(), 'O'.toByte(), 'X'.toByte()
    )

    // 加密key
    private const val DEFAULT_KEY = "sK\$T9%m+2a-]f~*7"

    /**
     * 加密
     */
    fun encrypt(str: String, key: String = DEFAULT_KEY): String? {
        return base64Encode(encryptWithHead(str.toByteArray(), key))?.let { String(it) }
    }

    /**
     * 解密
     */
    fun decrypt(str: String, key: String = DEFAULT_KEY): String? {
        val base64Decode = base64Decode(str.toByteArray())
        if (base64Decode != null) {
            return String(decryptWithHead(base64Decode, key))
        }
        return null
    }

    // 实际的加密解密操作
    @Throws(Exception::class)
    private fun operation(
        src: ByteArray,
        key: String,
        mode: Int
    ): ByteArray {
        val raw = key.toByteArray(charset("utf-8"))
        val keySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(mode, keySpec)
        return cipher.doFinal(src)
    }

    // 加密
    @Throws(Exception::class)
    private fun encryptWithHead(bytes: ByteArray, key: String): ByteArray {
        val encBytes: ByteArray = toEncrypt(bytes, key)
        val resultByte = ByteArray(encBytes.size + HEAD.size)
        System.arraycopy(HEAD, 0, resultByte, 0, HEAD.size)
        System.arraycopy(encBytes, 0, resultByte, HEAD.size, encBytes.size)
        return resultByte
    }

    // 解密
    @Throws(Exception::class)
    private fun decryptWithHead(src: ByteArray, key: String): ByteArray {
        require(isTJBOX(src)) { "Not TJBOX format" }
        val bytes = ByteArray(src.size - HEAD.size)
        System.arraycopy(src, HEAD.size, bytes, 0, bytes.size)
        return toDecrypt(bytes, key)
    }

    // 根据key加密
    @Throws(Exception::class)
    private fun toEncrypt(src: ByteArray, key: String): ByteArray {
        return operation(src, key, Cipher.ENCRYPT_MODE)
    }

    // 根据key解密
    @Throws(Exception::class)
    private fun toDecrypt(src: ByteArray, key: String): ByteArray {
        return operation(src, key, Cipher.DECRYPT_MODE)
    }

    // 校验头
    private fun isTJBOX(src: ByteArray): Boolean {
        for (i in HEAD.indices) {
            if (HEAD[i] != src[i]) {
                return false
            }
        }
        return true
    }

    // Base64解密
    @Throws(Exception::class)
    private fun base64Decode(str: ByteArray?): ByteArray? {
//        val decoder: Base64.Decoder = Base64.getDecoder()
        return try {
            Base64.decode(str, Base64.DEFAULT)
//            decoder.decode(str)
        } catch (e: Exception) {
            null
        }
    }

    // Base64加密
    @Throws(Exception::class)
    private fun base64Encode(str: ByteArray): ByteArray? {
        if (str.isNotEmpty()) {
//            val encoder: Base64.Encoder = Base64.getEncoder()
            return try {
                Base64.encode(str, Base64.DEFAULT)
//                encoder.encode(str)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}