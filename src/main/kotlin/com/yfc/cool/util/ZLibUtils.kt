package com.yfc.cool.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * ZLib压缩工具
 */
object ZLibUtils {
    /**
     * 压缩
     *
     * @param data 待压缩数据
     * @return byte[] 压缩后的数据
     */
    fun compress(data: ByteArray): ByteArray {
        var output: ByteArray
        val deflater = Deflater()
        deflater.reset()
        deflater.setInput(data)
        deflater.finish()
        val bos = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!deflater.finished()) {
                val i = deflater.deflate(buf)
                bos.write(buf, 0, i)
            }
            output = bos.toByteArray()
        } catch (e: Exception) {
            output = byteArrayOf()
            e.printStackTrace()
        } finally {
            try {
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        deflater.end()
        return output
    }

    /**
     * 解压缩
     *
     * @param data 待压缩的数据
     * @return byte[] 解压缩后的数据
     */
    fun decompress(data: ByteArray): ByteArray {
        var output: ByteArray
        val inflater = Inflater()
        inflater.reset()
        inflater.setInput(data)
        val o = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!inflater.finished()) {
                val i = inflater.inflate(buf)
                o.write(buf, 0, i)
            }
            output = o.toByteArray()
        } catch (e: Exception) {
            output = byteArrayOf()
            e.printStackTrace()
        } finally {
            try {
                o.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        inflater.end()
        return output
    }
}