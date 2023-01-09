package com.yfc.cool.umd

import com.yfc.cool.util.ByteUtil
import java.io.File
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.charset.Charset

class UMDFileHelper {
    var randomAccessFile: RandomAccessFile? = null

    fun init(file: File) {
        randomAccessFile = RandomAccessFile(file, "r")
    }

    fun seekAndRead(pos: Long, readLength: Long): ByteArray {
        return if (readLength <= Int.MAX_VALUE) {
            seekAndRead(pos, readLength.toInt())
        } else {
            var position = pos
            var length = readLength
            var result = byteArrayOf()
            while (length >= Int.MAX_VALUE) {
                val read = seekAndRead(position, Int.MAX_VALUE)
                result = result.plus(read)
                length -= Int.MAX_VALUE
                position += Int.MAX_VALUE
            }

            val read = seekAndRead(position, length)
            result = result.plus(read)
            position += length

            seek(pos)

            result
        }
    }

    fun seekAndRead(pos: Long, readLength: Int): ByteArray {
        return if (randomAccessFile != null) {
            seekAndRead(pos, readLength, randomAccessFile!!)
        } else {
            ByteUtil.getEmptyBytes(readLength)
        }
    }

    fun seekAndRead(pos: Long, readLength: Int, file: RandomAccessFile): ByteArray {
        seek(pos, file)
        return readBytes(readLength, file)
    }

    fun seek(pos: Long) {
        randomAccessFile?.apply {
            seek(pos, this)
        }
    }

    fun seek(pos: Long, file: RandomAccessFile) = file.seek(pos)

    fun readBytes(readLength: Int): ByteArray {
        return if (randomAccessFile != null) {
            readBytes(readLength, randomAccessFile!!)
        } else {
            ByteUtil.getEmptyBytes(readLength)
        }
    }

    fun readBytes(readLength: Int, file: RandomAccessFile): ByteArray {
        val bytes = ByteUtil.getEmptyBytes(readLength)
        file.read(bytes, 0, bytes.size)
        return bytes
    }

    fun getString(byteArray: ByteArray?): String? {
        return if (byteArray != null) {
            byteArray.reverse()
            String(byteArray, Charset.forName("UNICODE")).reversed()
        } else {
            null
        }
    }

    fun getLong(byteArray: ByteArray?): Long = getBigInteger(byteArray).toLong()
    fun getInt(byteArray: ByteArray?): Int = getBigInteger(byteArray).toInt()

    private fun getBigInteger(byteArray: ByteArray?): BigInteger {
        return if (byteArray != null) {
            byteArray.reverse()
            BigInteger(byteArray)
        } else {
            BigInteger(ByteUtil.getEmptyBytes(1))
        }
    }

    fun reset() {
        kotlin.runCatching {
            randomAccessFile?.close()
        }
        randomAccessFile = null
    }
}