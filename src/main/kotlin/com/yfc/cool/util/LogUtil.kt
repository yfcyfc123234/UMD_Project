package com.yfc.cool.util

object LogUtil {
    fun logD(tag: String, log: String) {
        println("$tag:$log")
    }

    fun logE(tag: String, log: Any?) {
        System.err.println("$tag:$log")
    }

    fun logE(tag: String, log: Throwable?) {
        System.err.println("$tag: error")
        log?.printStackTrace()
    }
}