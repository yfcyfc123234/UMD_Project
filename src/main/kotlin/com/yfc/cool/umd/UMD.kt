package com.yfc.cool.umd

import com.yfc.cool.umd.bean.UMDChapters
import com.yfc.cool.umd.bean.UMDCover
import com.yfc.cool.umd.bean.UMDHeader
import com.yfc.cool.umd.bean.UMDProperty
import com.yfc.cool.util.LogUtil
import com.yfc.cool.util.LogUtil.logD
import com.yfc.cool.util.LogUtil.logE
import com.yfc.cool.util.ZLibUtils
import java.io.File
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class UMD {
    companion object {
        private val tag = UMD::class.java.simpleName

        private const val DEBUG = true

        private val SEPARATOR = byteArrayOf(0X23)
        private val UMD_HEADER = byteArrayOf(0x89.toByte(), 0X9B.toByte(), 0X9A.toByte(), 0XDE.toByte())
        private val unknown1 = byteArrayOf(0x01, 0X00, 0X00, 0X08)
        private val unknown2 = byteArrayOf(0x01, 0X00)
        private val UMD_END = byteArrayOf(0x0C.toByte(), 0X00, 0X01, 0X09)
    }

    private var fileHelper: UMDFileHelper? = null

    var filePath: String? = null
    var fileLength: Long = 0
    var fileLengthUMD: Long = 0

    var parseStatus: UMDParseStatus = UMDParseStatus.STATUS_DEFAULT
    var header: UMDHeader? = null
    var property: UMDProperty? = null
    var chapter: UMDChapters? = null
    var cover: UMDCover? = null

    private var chapterDataBlockRandomBytes = byteArrayOf()
    private var chapterDataBlockRandomBytesArray: Array<String> = arrayOf()

    fun parseFile(
        filePath: String,
        listener: ((umd: UMD, success: Boolean) -> Unit)? = null,
    ) = parseFile(File(filePath), listener)

    @OptIn(ExperimentalTime::class)
    fun parseFile(
        file: File,
        listener: ((umd: UMD, success: Boolean) -> Unit)? = null,
    ) {
        thread {
            reset()

            kotlin.runCatching {
                val duration = measureTime {
                    this.filePath = file.absolutePath
                    this.fileLength = file.length()
                    this.fileHelper = UMDFileHelper().apply { init(file) }

                    var pos = parseHeader()
                    pos = parseProperty(pos)
                    pos = parseChapter(pos)
                    parseCover(pos)
                    parseEnd()

                    parseStatus = UMDParseStatus.STATUS_SUCCESS
                    resetFileHelper()
                }

                logD("parseFile success $duration")
                listener?.invoke(this, true)
            }.onFailure {
                parseStatus = UMDParseStatus.STATUS_FAILED
                reset()

                logE(it)
                listener?.invoke(this, false)
            }
        }
    }

    /**
     * 解析文件长度
     */
    private fun parseEnd() {
        val bytes = SEPARATOR.plus(UMD_END)
        val size = bytes.size + 4
        fileHelper?.apply {
            val read = seekAndRead((randomAccessFile?.length() ?: 0) - size, size)
            if (read.take(size - 4).toByteArray().contentEquals(bytes)) {
                fileLengthUMD = getLong(read.takeLast(4).toByteArray())
            } else {
                throw UMDException("解析失败，无法获取整个文件长度")
            }
        }
    }

    /**
     * 判断是否是UMD格式的电子书
     */
    private fun parseHeader(): Long {
        val headerLength = UMD_HEADER.size.toLong()
        val umdFormat = fileHelper?.seekAndRead(0, headerLength.toInt())
        if (umdFormat.contentEquals(UMD_HEADER)) {
            header = UMDHeader().apply {
                header = umdFormat?.copyOf()
            }
            return headerLength
        } else {
            throw UMDException("解析失败，该文件不是UMD类型文件")
        }
    }

    /**
     * 解析一些可能存在的属性
     */
    private fun parseProperty(pos: Long): Long {
        property = UMDProperty()

        var position = pos + SEPARATOR.size + unknown1.size
        val type = fileHelper?.seekAndRead(position, 1)
        if (type != null && type[0] == 0x01.toByte()) {
            property?.type = UMDType.TYPE_TXT

            position += unknown2.size + 1

            position = parseProperty(position, UMDProperty.titleBytes)
            position = parseProperty(position, UMDProperty.authorBytes)
            position = parseProperty(position, UMDProperty.yearBytes)
            position = parseProperty(position, UMDProperty.monthBytes)
            position = parseProperty(position, UMDProperty.dayBytes)
            position = parseProperty(position, UMDProperty.genderBytes)
            position = parseProperty(position, UMDProperty.publisherBytes)
            position = parseProperty(position, UMDProperty.vendorBytes)
            position = parseProperty(position, UMDProperty.fileLengthBytes)

            return position
        } else {
            throw UMDException("解析失败，暂不支持UMD漫画类型文件")
        }
    }

    /**
     * 按类型解析一些可能存在的属性
     */
    private fun parseProperty(pos: Long, bytes: ByteArray): Long {
        var position = pos

        val temp = SEPARATOR.plus(bytes)
        val read = fileHelper?.seekAndRead(position, temp.size)

        if (!read.contentEquals(temp)) {
            return position
        }

        position += temp.size

        val lengthBytes = fileHelper?.seekAndRead(position, 2)

        if (bytes.contentEquals(UMDProperty.fileLengthBytes)) {
            val size = UMDProperty.fileLengthBytes.size
            val fileLengthBytes = fileHelper?.seekAndRead(position, size)

            property?.fileLengthUncompressed = fileHelper!!.getLong(fileLengthBytes)

            position += size
        } else {
            if (lengthBytes == null) {
                return position
            }

            val length = lengthBytes[1].toInt() - 5
            position += lengthBytes.size
            val dataBytes = fileHelper?.seekAndRead(position, length)

            val data = fileHelper?.getString(dataBytes)
            property?.apply {
                when {
                    bytes.contentEquals(UMDProperty.titleBytes) -> title = data
                    bytes.contentEquals(UMDProperty.authorBytes) -> author = data
                    bytes.contentEquals(UMDProperty.yearBytes) -> year = data
                    bytes.contentEquals(UMDProperty.monthBytes) -> month = data
                    bytes.contentEquals(UMDProperty.dayBytes) -> day = data
                    bytes.contentEquals(UMDProperty.genderBytes) -> gender = data
                    bytes.contentEquals(UMDProperty.publisherBytes) -> publisher = data
                    bytes.contentEquals(UMDProperty.vendorBytes) -> vendor = data
                }
            }

            position += length
        }

        return position
    }

    /**
     * 解析目录和正文内容
     */
    private fun parseChapter(pos: Long): Long {
        chapter = UMDChapters()

        var position = pos + UMDChapters.chaptersBytes.size + 1
        val randomBytes1 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
        position += UMDChapters.randomBytes.size + UMDChapters.separator.size
        val randomBytes2 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
        position += UMDChapters.randomBytes.size

        if (randomBytes1.contentEquals(randomBytes2)) {
            val size = 4

            val chapterLengthBytes = fileHelper?.seekAndRead(position, size)
            val chapterSize = fileHelper!!.getInt(chapterLengthBytes)

            position += size

            chapter?.apply {
                this.chaptersSize = (chapterSize - 9) / 4
                this.chaptersTitleAndContent = MutableList(this.chaptersSize) { _ -> UMDChapters.UMDChapterTitleAndContent() }

                this.chaptersTitleAndContent!!.forEach {
                    val sizeChapterSingle = 4
                    val chapterSingleBytes = fileHelper?.seekAndRead(position, sizeChapterSingle)
                    val chapterSingleOffset = fileHelper!!.getLong(chapterSingleBytes)
                    it.offset = chapterSingleOffset
                    position += sizeChapterSingle
                }
            }

            position += UMDChapters.chaptersTitleBytes.size + 1
            val randomBytesTitle1 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
            position += UMDChapters.randomBytes.size + UMDChapters.separator.size
            val randomBytesTitle2 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
            position += UMDChapters.randomBytes.size
            if (randomBytesTitle1.contentEquals(randomBytesTitle2)) {
                val sizeTitle = 4

                val chapterTitleLengthBytes = fileHelper?.seekAndRead(position, sizeTitle)
                val allChapterTitleSize = fileHelper!!.getLong(chapterTitleLengthBytes)

                position += sizeTitle

                val singleChapterTitleSize = ((allChapterTitleSize - 9) / chapter!!.chaptersSize).toInt()

                for (i in 0 until chapter!!.chaptersSize) {
                    val temp = fileHelper?.seekAndRead(position + 1, singleChapterTitleSize - 1)
                    chapter!!.chaptersTitleAndContent!![i].title = fileHelper?.getString(temp)

                    position += singleChapterTitleSize
                }

                var tempPosition: Long
                do {
                    tempPosition = parseChapterDataBlock(position)
                    if (tempPosition > 0) {
                        position = tempPosition
                    }
                } while (tempPosition > 0)

                position += SEPARATOR.plus(UMDChapters.contentEnd).size + 9

                val chapterDataSizeCheck = ((fileHelper?.getLong(fileHelper?.seekAndRead(position, 4)) ?: 0) - 9) / 4
                position += 4
                val chapterDataSize = if (chapter?.chaptersOriginalData.isNullOrEmpty()) 0 else chapter!!.chaptersOriginalData!!.size

                if (chapterDataSizeCheck.toInt() == chapterDataSize) {
                    relevanceChapterAndContent()

                    position += chapterDataBlockRandomBytesArray.size
                    return position
                } else {
                    throw UMDException("解析失败，章节校验未通过")
                }
            } else {
                throw UMDException("解析失败，章节校验未通过")
            }
        } else {
            throw UMDException("解析失败，章节校验未通过")
        }
    }

    /**
     * 关联标题和内容
     */
    private fun relevanceChapterAndContent() {
        chapter?.apply {
            var allContentBytes = byteArrayOf()
            chaptersOriginalData?.forEach {
                val bytes = ZLibUtils.decompress(fileHelper?.seekAndRead(it.offset, it.length) ?: byteArrayOf())
                allContentBytes = allContentBytes.plus(bytes)
            }

            chaptersTitleAndContent?.let {
                if (it.isNotEmpty()) {
                    it.forEachIndexed { index, bean ->
                        if (index < it.size - 1) {
                            bean.length = it[index + 1].offset - bean.offset
                        } else {
                            bean.length = allContentBytes.size - bean.offset
                        }

                        val byteArray = allContentBytes.copyOfRange(bean.offset.toInt(), (bean.offset + bean.length).toInt())
                        byteArray.reverse()
                        bean.content = String(byteArray, Charset.forName("UNICODE"))
                            .reversed()
                            .replace("\u2029", "\r\n")
                            .replace("\u0000", "")
                    }
                }
            }
        }
    }

    /**
     * 解析正文(循环)
     */
    private fun parseChapterDataBlock(pos: Long): Long {
        var position = pos

        position = parseChapterDataBlockJump(position)
        if (position == 0L) {
            return 0L
        }
        position += UMDChapters.separator.size

        val randomBytes = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
        chapterDataBlockRandomBytes = chapterDataBlockRandomBytes.plus(randomBytes!!)
        val list = chapterDataBlockRandomBytes.map {
            var result = it.toUByte().toString(16)
            if (result.length == 1) {
                result = "0${result}"
            }
            "0x${result}".uppercase()
        }
        chapterDataBlockRandomBytesArray = list.toTypedArray()

        position += UMDChapters.randomBytes.size
        val chapterDataLengthBytes = fileHelper?.seekAndRead(position, 4)
        val chapterDataLength = fileHelper!!.getLong(chapterDataLengthBytes) - 9

        position += chapterDataLengthBytes!!.size
        val seekAndRead = fileHelper?.seekAndRead(position, chapterDataLength)

        val data = fileHelper?.getString(ZLibUtils.decompress(seekAndRead!!))

        if (chapter?.chaptersOriginalData.isNullOrEmpty()) {
            chapter?.chaptersOriginalData = mutableListOf()
        }
        chapter?.chaptersOriginalData?.add(UMDChapters.UMDChapterOriginalData().apply {
            this.filePath = this@UMD.filePath
            this.offset = position
            this.length = chapterDataLength
            this.data = data
        })

        position += chapterDataLength

        return position
    }

    /**
     * 解析正文时
     * 判断是否需要跳过某些字节 或者 是否解析正文完成
     */
    private fun parseChapterDataBlockJump(pos: Long): Long {
        var position = pos

        val chunkATemp = SEPARATOR.plus(UMDChapters.chunkA)
        val bytesA = fileHelper?.seekAndRead(position, chunkATemp.size)
        if (bytesA.contentEquals(chunkATemp)) {
            position += chunkATemp.size + 4
            return position
        }

        val chunkFTemp = SEPARATOR.plus(UMDChapters.chunkF)
        val bytesF = fileHelper?.seekAndRead(position, chunkFTemp.size)
        if (bytesF.contentEquals(chunkFTemp)) {
            position += chunkFTemp.size + 16
            return position
        }

        val contentEndTemp = SEPARATOR.plus(UMDChapters.contentEnd)
        val bytesContentEnd = fileHelper?.seekAndRead(position, contentEndTemp.size)
        if (bytesContentEnd.contentEquals(contentEndTemp)) {
            return 0
        }

        return position
    }

    /**
     * 解析封面图片
     */
    private fun parseCover(pos: Long): Long {
        var position = pos

        val coverBytes = SEPARATOR.plus(UMDCover.coverBytes)
        val bytes = fileHelper?.seekAndRead(position, coverBytes.size)
        if (bytes.contentEquals(coverBytes)) {
            position += coverBytes.size + 9

            val coverLengthBytes = fileHelper?.seekAndRead(position, 4)
            val coverLength = (fileHelper?.getLong(coverLengthBytes) ?: 0) - 9
            position += 4

            val coverDataBytes = fileHelper?.seekAndRead(position, coverLength)
            val file = File(filePath ?: "")
            val coverFile = File(file.parentFile.absolutePath, "${file.nameWithoutExtension}.jpg")
            coverFile.writeBytes(coverDataBytes!!)

            cover = UMDCover().apply {
                this.coverPath = coverFile.absolutePath
            }

            position += coverLength

            return position
        } else {
            throw UMDException("解析失败，封面校验未通过")
        }
    }

    /**
     * 重置属性
     */
    fun reset() {
        parseStatus = UMDParseStatus.STATUS_DEFAULT

        filePath = null
        fileLength = 0
        fileLengthUMD = 0

        header = null
        property = null
        chapter = null
        cover?.apply {
            val file = File(this.coverPath ?: "")
            if (file.exists() && file.isFile) {
                file.delete()
            }
        }
        cover = null

        resetFileHelper()
    }

    /**
     * 重置文件帮助类
     */
    private fun resetFileHelper() {
        fileHelper?.reset()
        fileHelper = null
    }

    private fun logD(log: String) {
        if (DEBUG) {
            LogUtil.logD(tag, log)
        }
    }

    private fun logE(log: Any?) {
        if (DEBUG) {
            LogUtil.logE(tag, log)
        }
    }

    private fun logE(log: Throwable) {
        if (DEBUG) {
            LogUtil.logE(tag, log)
        }
    }
}