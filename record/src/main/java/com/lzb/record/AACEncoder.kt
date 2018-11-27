package com.lzb.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Created by lzb on 2018/11/25.
 */
class AACEncoder {
    private val MIME_TYPE = "audio/mp4a-latm"
    private val SAMPLE_RATE = 44100
    private val CHANNEL_COUNT = 2
    private val BIT_RATE = 96000
    private val AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    private lateinit var muBufferInfo: MediaCodec.BufferInfo
    private var mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)

    private var inputBuffers: Array<ByteBuffer>? = null
    private var outputBuffers: Array<ByteBuffer>? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null


    fun prepare() {
        muBufferInfo = MediaCodec.BufferInfo()
        val mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AAC_PROFILE)
        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mEncoder.start()

        inputBuffers = mEncoder.inputBuffers
        outputBuffers = mEncoder.outputBuffers
        bufferInfo = MediaCodec.BufferInfo()
    }

    fun encode(readSize: Int, readBuffer: ByteArray, fileOutputStream: FileOutputStream) {
        if (inputBuffers != null) {
            var overCount = 0
            var lastSize = 0
            val lastReadBuffer = ByteArray(readBuffer.size)
            var inputBufferIndex = mEncoder.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers!![inputBufferIndex]
                inputBuffer.clear()
                val remainSize = inputBuffer.remaining()
                /**
                 * 如果一次塞不完的话，处理后再循环处理剩下的数据
                 */
                if (remainSize < readBuffer.size) {
                    inputBuffer.put(readBuffer, 0, remainSize)
                    inputBuffer.limit(remainSize)
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, remainSize, System.nanoTime(), 0)
                    overCount = readBuffer.size / remainSize
                    lastSize = readBuffer.size - remainSize
                    System.arraycopy(readBuffer, remainSize, lastReadBuffer, 0, lastSize)
                } else {
                    inputBuffer.put(readBuffer)
                    inputBuffer.limit(readBuffer.size)
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, readBuffer.size, System.nanoTime(), 0)
                }

            }
            getOutputBufferAndWrite(fileOutputStream)
            fileOutputStream.flush()
            while (overCount > 0 && lastSize > 0) {
                inputBufferIndex = mEncoder.dequeueInputBuffer(-1)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = inputBuffers!![inputBufferIndex]
                    inputBuffer.clear()
                    val remainSize = inputBuffer.remaining()
                    if (remainSize < lastSize) {
                        inputBuffer.put(lastReadBuffer, 0, remainSize)
                        inputBuffer.limit(remainSize)
                        mEncoder.queueInputBuffer(inputBufferIndex, 0, remainSize, System.nanoTime(), 0)

                        lastSize -= remainSize
                        System.arraycopy(readBuffer, (readBuffer.size - lastSize), lastReadBuffer, 0, lastSize)
                    } else {
                        inputBuffer.put(lastReadBuffer, 0, lastSize)
                        inputBuffer.limit(lastSize)
                        mEncoder.queueInputBuffer(inputBufferIndex, 0, lastSize, System.nanoTime(), 0)
                        //剩余长度置0
                        lastSize = 0
                    }
                }
                getOutputBufferAndWrite(fileOutputStream)
                fileOutputStream.flush()
                overCount--
            }
        }
    }

    fun getOutputBufferAndWrite(fileOutputStream: FileOutputStream) {
        var outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0)
        while (outputBufferIndex >= 0) {
            val outBitsSize = bufferInfo!!.size
            val outPacketSize = outBitsSize + 7
            val outputBuffer = outputBuffers!![outputBufferIndex]

            outputBuffer.position(bufferInfo!!.offset)
            outputBuffer.limit(bufferInfo!!.size + outBitsSize)

            //添加ADTS头
            val outData = ByteArray(outPacketSize)
            addADTStoPacket(outData, outPacketSize)

            outputBuffer.get(outData, 7, outBitsSize)
            outputBuffer.position(bufferInfo!!.offset)
            try {
                fileOutputStream.write(outData)
            } catch (e: Exception) {
            }
            mEncoder.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
        val profile = 2 // AAC LC
        // 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        val freqIdx = 4 // 16000 采样率
        val chanCfg = 1 // 1 单声道
        // fill in ADTS data
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
        Log.e("AudioEncoder", packetLen.toString() + "packetlen")
    }

}