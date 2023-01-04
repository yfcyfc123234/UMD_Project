package com.yfc.cool.umd.bean

import com.yfc.cool.umd.UMDFileHelper
import com.yfc.cool.util.ByteUtil
import com.yfc.cool.util.ZLibUtils
import java.io.File

class UMDChapters : UMDBean() {
    companion object {
        private const val serialVersionUID: Long = 6721233348990574756L

        val chaptersBytes = byteArrayOf(0x83.toByte(), 0x00, 0x01, 0x09)
        val randomBytes = ByteUtil.getEmptyBytes(4)
        val separator = byteArrayOf(0x24)
        val chaptersTitleBytes = byteArrayOf(0x84.toByte(), 0x00, 0x01, 0x09)
        val chunkF = byteArrayOf(0XF1.toByte(), 0x00, 0x00, 0x15)
        val chunkA = byteArrayOf(0X0A.toByte(), 0x00, 0x00, 0x09)
        val contentEnd = byteArrayOf(0X81.toByte(), 0x00, 0x01, 0x09)
    }

    var chaptersSize: Int = 0
    var chaptersData: MutableList<UMDChapterData>? = null
    var chaptersTitle: MutableList<UMDChapterTitle>? = null

    override fun toString(): String {
        return "UMDChapters(" +
                "chaptersSize=$chaptersSize, " +
                "chaptersData=${if (chaptersData.isNullOrEmpty()) "null" else chaptersData!!.size.toString()}, " +
                "\nchaptersTitle=$chaptersTitle" +
                ")"
    }

    class UMDChapterTitle : UMDBean() {
        companion object {
            private const val serialVersionUID: Long = -220330632556807410L
        }

        var offset: Long = 0L
        var title: String? = null

        override fun toString(): String {
            return "UMDChapterTitle(offset=$offset, title=$title)\n"
        }
    }

    class UMDChapterData : UMDBean() {
        companion object {
            private const val serialVersionUID: Long = 6982003890271432585L
        }

        var filePath: String? = null
        var offset: Long = 0L
        var length: Long = 0L
        var data: String? = null
            get() {
                kotlin.runCatching {
                    if (field.isNullOrEmpty()) {
                        val helper = UMDFileHelper()
                        helper.init(File(filePath ?: ""))
                        field = helper.getString(ZLibUtils.decompress(helper.seekAndRead(offset, length)))
                        helper.reset()
                    }
                }
                return field
            }

        override fun toString(): String {
            return "UMDChapterData(filePath=$filePath, offset=$offset, length=$length)"
        }
    }
}