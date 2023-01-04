package com.yfc.cool.umd.bean

import com.yfc.cool.umd.UMDType
import java.text.SimpleDateFormat

class UMDProperty : UMDBean() {
    companion object {
        private const val serialVersionUID: Long = 1637941732661918571L

        val titleBytes = byteArrayOf(0x02, 0x00)
        val authorBytes = byteArrayOf(0x03, 0x00)
        val yearBytes = byteArrayOf(0x04, 0x00)
        val monthBytes = byteArrayOf(0x05, 0x00)
        val dayBytes = byteArrayOf(0x06, 0x00)
        val genderBytes = byteArrayOf(0x07, 0x00)
        val publisherBytes = byteArrayOf(0x08, 0x00)
        val vendorBytes = byteArrayOf(0x09, 0x00)
        val fileLengthBytes = byteArrayOf(0x0B, 0x00, 0x00, 0x09)
    }

    var type: UMDType? = null

    var title: String? = null // 0x02 0x00 标题
    var author: String? = null // 0x03 0x00 作者
    var year: String? = null // 0x04 0x00 出版年份
    var month: String? = null // 0x05 0x00 出版月份
    var day: String? = null // 0x06 0x00 出版日
    var gender: String? = null // 0x07 0x00 小说类型
    var publisher: String? = null // 0x08 0x00 出版商
    var vendor: String? = null // 0x09 0x00 零售商
    var fileLengthUncompressed: Long = 0 // 0x0B, 0x00, 0x00, 0x09 小说未压缩时的内容总长度（字节）

    fun getDate(): String = "${year}-${month}-${day}"

    fun getDateLong(): Long {
        kotlin.runCatching {
            return SimpleDateFormat("yyyy-MM-dd").parse(getDate()).time
        }
        return 0L
    }

    override fun toString(): String {
        return "UMDProperty(type=$type, title=$title, author=$author, year=$year, month=$month, day=$day, gender=$gender, publisher=$publisher, vendor=$vendor, fileLengthUncompressed=$fileLengthUncompressed)"
    }
}