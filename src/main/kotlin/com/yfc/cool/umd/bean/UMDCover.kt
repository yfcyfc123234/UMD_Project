package com.yfc.cool.umd.bean

class UMDCover : UMDBean() {
    companion object {
        private const val serialVersionUID: Long = -1262186795623718041L

        val coverBytes = byteArrayOf(0x82.toByte(), 0x00, 0x01, 0x0A, 0x01)
    }

    var coverPath: String? = null

    override fun toString(): String {
        return "UMDCover(coverPath=$coverPath)"
    }
}