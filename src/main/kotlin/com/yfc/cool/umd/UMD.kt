package com.yfc.cool.umd

import com.yfc.cool.umd.bean.UMDChapters
import com.yfc.cool.umd.bean.UMDCover
import com.yfc.cool.umd.bean.UMDHeader
import com.yfc.cool.umd.bean.UMDProperty
import com.yfc.cool.util.LogUtil
import com.yfc.cool.util.ZLibUtils
import java.io.File
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class UMD {
    companion object {
        private val tag = UMD::class.java.simpleName

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
    var loadDataToMemory = false

    var parseStatus: UMDParseStatus = UMDParseStatus.STATUS_DEFAULT
    var header: UMDHeader? = null
    var property: UMDProperty? = null
    var chapter: UMDChapters? = null
    var cover: UMDCover? = null

    private var chapterDataBlockRandomBytes = byteArrayOf()
    private var chapterDataBlockRandomBytesArray: Array<String> = arrayOf()

    fun parseFile(filePath: String, loadDataToMemory: Boolean = false, listener: ((Boolean) -> Unit)? = null) {
        parseFile(File(filePath), loadDataToMemory, listener)
    }

    @OptIn(ExperimentalTime::class)
    fun parseFile(file: File, loadDataToMemory: Boolean = false, listener: ((Boolean) -> Unit)? = null) {
        thread {
            reset()

            kotlin.runCatching {
                val duration = measureTime {
                    this.filePath = file.absolutePath
                    this.fileLength = file.length()
                    this.loadDataToMemory = loadDataToMemory
                    this.fileHelper = UMDFileHelper().apply { init(file) }

                    var pos = parseHeader()
                    pos = parseProperty(pos)
                    pos = parseChapter(pos)
                    pos = parseCover(pos)
                    parseEnd()

                    parseStatus = UMDParseStatus.STATUS_SUCCESS
                    resetFileHelper()
                }

                logD("parseFile success $duration")
                listener?.invoke(true)
            }.onFailure {
                parseStatus = UMDParseStatus.STATUS_FAILED
                reset()

                logE(it)
                listener?.invoke(false)
            }
        }
    }

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

            property?.fileLength = fileHelper!!.getLong(fileLengthBytes)

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

    private fun parseChapter(pos: Long): Long {
        chapter = UMDChapters()

        var position = pos + UMDChapters.chaptersBytes.size + 1
        val randomBytes1 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
        position += UMDChapters.randomBytes.size + UMDChapters.separator.size
        val randomBytes2 = fileHelper?.seekAndRead(position, UMDChapters.randomBytes.size)
        position += UMDChapters.randomBytes.size

        if (randomBytes1?.contentEquals(randomBytes2) == true) {
            val size = 4

            val chapterLengthBytes = fileHelper?.seekAndRead(position, size)
            val chapterSize = fileHelper!!.getInt(chapterLengthBytes)

            position += size

            chapter?.apply {
                this.chaptersSize = (chapterSize - 9) / 4
                this.chaptersTitle = MutableList(this.chaptersSize) { _ -> UMDChapters.UMDChapterTitle() }

                this.chaptersTitle!!.forEach {
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
            if (randomBytesTitle1?.contentEquals(randomBytesTitle2) == true) {
                val sizeTitle = 4

                val chapterTitleLengthBytes = fileHelper?.seekAndRead(position, sizeTitle)
                val allChapterTitleSize = fileHelper!!.getLong(chapterTitleLengthBytes)

                position += sizeTitle

                val singleChapterTitleSize = ((allChapterTitleSize - 9) / chapter!!.chaptersSize).toInt()

                for (i in 0 until chapter!!.chaptersSize) {
                    val temp = fileHelper?.seekAndRead(position + 1, singleChapterTitleSize - 1)
                    chapter!!.chaptersTitle!![i].title = fileHelper?.getString(temp)

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
                val chapterDataSize = if (chapter?.chaptersData.isNullOrEmpty()) 0 else chapter!!.chaptersData!!.size

                if (chapterDataSizeCheck.toInt() == chapterDataSize) {
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

        var data: String? = null
        if (loadDataToMemory) {
            data = fileHelper?.getString(ZLibUtils.decompress(seekAndRead!!))
        }

        if (chapter?.chaptersData.isNullOrEmpty()) {
            chapter?.chaptersData = mutableListOf()
        }
        chapter?.chaptersData?.add(UMDChapters.UMDChapterData().apply {
            this.filePath = this@UMD.filePath
            this.offset = position
            this.length = chapterDataLength
            this.data = data
        })

        position += chapterDataLength

        return position
    }

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

    fun reset() {
        parseStatus = UMDParseStatus.STATUS_DEFAULT

        filePath = null
        fileLength = 0
        fileLengthUMD = 0
        loadDataToMemory = false

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

    private fun resetFileHelper() {
        fileHelper?.reset()
        fileHelper = null
    }

    private fun logD(log: String) {
        LogUtil.logD(tag, log)
    }

    private fun logE(log: Any?) {
        LogUtil.logE(tag, log)
    }

    private fun logE(log: Throwable) {
        LogUtil.logE(tag, log)
    }
}