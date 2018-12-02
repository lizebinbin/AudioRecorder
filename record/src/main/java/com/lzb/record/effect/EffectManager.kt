package com.lzb.record.effect

import android.content.Context
import org.fmod.FMOD
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by lzb on 2018/11/30.
 */
class EffectManager private constructor() {
    //是否初始化
    private var isInited = false
    var currentType = EffectUtils.MODE_NORMAL
    private var fixedThreadPool: ExecutorService? = null
    private lateinit var playerThread: PlayerThread
    private var path = "file:///android_asset/bin.wav"

    companion object {
        private var mInstance: EffectManager? = null
        @Synchronized
        fun getInstance(): EffectManager {
            if (mInstance == null) {
                mInstance = EffectManager()
            }
            return mInstance!!
        }
    }

    fun init(context: Context) {
        FMOD.init(context)
        fixedThreadPool = Executors.newFixedThreadPool(1)
        isInited = true
    }

    fun play(path: String, type: Int) {
        if (!isInited)
            throw IllegalStateException("FMOD not inited")
        this.path = path
        this.currentType = type
        playerThread = PlayerThread()
        fixedThreadPool?.execute(playerThread)
    }

    fun stop() {
        EffectUtils.stop()
    }

    fun close() {
        FMOD.close()
    }

    inner class PlayerThread : Runnable {
        override fun run() {
            EffectUtils.fix(path, currentType, 0)
        }
    }
}