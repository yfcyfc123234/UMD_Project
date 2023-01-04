package com.yfc.cool.umd.bean

class UMDHeader : UMDBean() {
    companion object {
        private const val serialVersionUID: Long = -5156275993859357281L
    }

    var header: ByteArray? = null
    override fun toString(): String {
        return "UMDHeader(header=${header?.contentToString()})"
    }
}