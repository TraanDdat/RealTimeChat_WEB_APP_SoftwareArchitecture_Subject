package com.example.zaloclone

object NativeProcessor {
    init {
        System.loadLibrary("zaloclone")
    }

    external fun stringFromJNI(): String
    external fun processImageSecurely(imageData: ByteArray): ByteArray
}
