package com.lzb.record.play

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by lzb on 2018/11/27.
 */
class AudioTracker {
    // 音频采样率 44100，官方文档表示这个采样率兼容性最好
    private val AUDIO_SAMPLE_RETE = 44100
    //声道  双声道
    private val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO
    //音频采集精度
    private val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    //缓冲区字节大小
    private var bufferSizeInBytes: Int = 0

    private lateinit var mAudioTrack: AudioTrack
    private var mCurrentStatus = PlayStatus.STATUS_RELEASE

    private var mExecutorService: ExecutorService = Executors.newCachedThreadPool()

    companion object {
        private var mInstance: AudioTracker? = null

        @Synchronized
        fun getInstance(): AudioTracker {
            if (mInstance == null)
                mInstance = AudioTracker()
            return mInstance!!
        }
    }

    fun prepare() {
        bufferSizeInBytes = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RETE, AUDIO_CHANNEL, AUDIO_ENCODING)
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            AUDIO_SAMPLE_RETE,
            AUDIO_CHANNEL,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes, AudioTrack.MODE_STREAM
        )
        updateStatus(PlayStatus.STATUS_READY)
    }

    fun start() {
        if (mCurrentStatus == PlayStatus.STATUS_PLAYING)
            throw IllegalStateException("it's playing now!")
        if (mCurrentStatus == PlayStatus.STATUS_RELEASE)
            throw IllegalStateException("you should call prepare() before start playing")
        mAudioTrack.play()
        updateStatus(PlayStatus.STATUS_PLAYING)
    }

    fun stop() {
        mAudioTrack.stop()
        updateStatus(PlayStatus.STATUS_STOP)
        release()
    }

    fun play(audioBuffer: ByteArray) {
        if (mCurrentStatus == PlayStatus.STATUS_PLAYING)
            mAudioTrack.write(audioBuffer, 0, audioBuffer.size)
    }

    fun playPCMFile(path: String) {
        if (mCurrentStatus == PlayStatus.STATUS_PLAYING) {
            mExecutorService.execute {
                var fis: FileInputStream? = null
                try {
                    fis = FileInputStream(path)
                    val bytes = ByteArray(1024)
                    var readSize = fis.read(bytes)
                    while (readSize > 0) {
                        mAudioTrack.write(bytes, 0, readSize)
                        readSize = fis.read(bytes)
                    }
                    stop()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    fis?.close()
                }
            }
        }
    }

    private fun release() {
        mAudioTrack.release()
        updateStatus(PlayStatus.STATUS_RELEASE)
    }

    private fun updateStatus(status: PlayStatus) {
        if (mCurrentStatus == status)
            return
        mCurrentStatus = status
        //回调

    }
}