package com.v2ray.ang.util

import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.util.Base64
import com.google.zxing.WriterException
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import java.util.*
import kotlin.collections.HashMap

object Utils {

    /**
     * convert string to editalbe for kotlin
     *
     * @param text
     * @return
     */
    fun getEditable(text: String): Editable {
        return Editable.Factory.getInstance().newEditable(text)
    }

    /**
     * find value in array position
     */
    fun arrayFind(array: Array<out String>, value: String): Int {
        for (i in array.indices) {
            if (array[i] == value) {
                return i
            }
        }
        return -1
    }

    /**
     * parseInt
     */
    fun parseInt(str: String): Int {
        try {
            return Integer.parseInt(str)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    /**
     * get text from clipboard
     */
    fun getClipboard(context: Context): String {
        try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return cmb.text.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * set text to clipboard
     */
    fun setClipboard(content: String, context: Context) {
        try {
            val cmb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.text = content
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * base64 decode
     */
    fun decode(text: String): String {
        try {
            return Base64.decode(text, Base64.NO_WRAP).toString(charset("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * base64 encode
     */
    fun encode(text: String): String {
        try {
            return Base64.encodeToString(text.toByteArray(charset("UTF-8")), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * get dns servers
     */
    fun getDnsServers(): Array<out String> {
        val ret = LinkedHashSet<String>()
        ret.add("8.8.8.8")
        ret.add("8.8.4.4")
        return ret.toTypedArray()
    }

    /**
     * create qrcode using zxing
     */
    fun createQRCode(text: String, size: Int = 500): Bitmap? {
        try {
            val hints = HashMap<EncodeHintType, String>()
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8")
            val bitMatrix = QRCodeWriter().encode(text,
                    BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            for (y in 0..size - 1) {
                for (x in 0..size - 1) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000.toInt()
                    } else {
                        pixels[y * size + x] = 0xffffffff.toInt()
                    }

                }
            }
            val bitmap = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * is ip address
     */
    fun isIpAddress(value: String): Boolean {
        try {
            var start = 0
            var end = value.indexOf('.')
            var numBlocks = 0

            while (start < value.length) {

                if (end == -1) {
                    end = value.length
                }

                try {
                    val block = Integer.parseInt(value.substring(start, end))
                    if (block > 255 || block < 0) {
                        return false
                    }
                } catch (e: NumberFormatException) {
                    return false
                }

                numBlocks++
                start = end + 1
                end = value.indexOf('.', start)
            }
            return numBlocks == 4
        } catch (e: WriterException) {
            e.printStackTrace()
            return false
        }
    }
}


