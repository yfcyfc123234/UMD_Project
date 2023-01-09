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
    var chaptersOriginalData: MutableList<UMDChapterOriginalData>? = null
    var chaptersTitleAndContent: MutableList<UMDChapterTitleAndContent>? = null

    override fun toString(): String {
        return "UMDChapters(" +
                "chaptersSize=$chaptersSize, " +
                "chaptersOriginalData=${if (chaptersOriginalData.isNullOrEmpty()) "null" else chaptersOriginalData!!.size.toString()}, " +
                "\nchaptersTitleAndContent=$chaptersTitleAndContent" +
                ")"
    }

    class UMDChapterTitleAndContent : UMDBean() {
        companion object {
            private const val serialVersionUID: Long = -220330632556807410L
        }

        var offset: Long = 0L
        var length: Long = 0L
        var title: String? = null
        var content: String? = null

        override fun toString(): String {
            return "UMDChapterTitleAndContent(offset=$offset, length=$length, title=$title, content=${if (content.isNullOrEmpty()) "null" else content?.length.toString()})\n"
        }
    }

    class UMDChapterOriginalData : UMDBean() {
        companion object {
            private const val serialVersionUID: Long = 6982003890271432585L
        }

        var filePath: String? = null
        var offset: Long = 0L
        var length: Long = 0L
        var data: String? = null

        override fun toString(): String {
            return "UMDChapterOriginalData(filePath=$filePath, offset=$offset, length=$length, data=${if (data.isNullOrEmpty()) "null" else data?.length.toString()})"
        }
    }
}