package com.lzb.record

import android.media.AudioFormat
import android.util.Log
import java.io.*


/**
 * Created by lzb on 2018/11/21.
 */
class PcmToWaveUtil {
    private val mBufferSize: Int = 1024 //缓存的音频大小
    private val mSampleRate = 44100
    private val mChannel = AudioFormat.CHANNEL_IN_STEREO //立体声
    private val mEncoding = AudioFormat.ENCODING_PCM_16BIT


    fun addHeader2wav(filePath: String, fileOutputStream: FileOutputStream) {
        val longSampleRate = mSampleRate
        val totalAudioLen = fileOutputStream.channel.size()
        val totalDataLen = totalAudioLen + 36
        val channels = 2
        val byteRate = 16 * mSampleRate * channels / 8

        var randomAccessFile: RandomAccessFile? = null
        var tmpFis: FileInputStream? = null
        var tmpFos: FileOutputStream? = null
        try {
            randomAccessFile = RandomAccessFile(filePath, "rw")
            val tmp = File.createTempFile("tmp", null)
            tmp.deleteOnExit()
            tmpFis = FileInputStream(tmp)
            tmpFos = FileOutputStream(tmp)
            randomAccessFile.seek(0)
            val tmpByte = ByteArray(1024)
            var hasRead = randomAccessFile.read(tmpByte)
            while (hasRead > 0) {
                tmpFos.write(tmpByte, 0, hasRead)
                hasRead = randomAccessFile.read(tmpByte)
            }
            randomAccessFile.seek(0)
            //插入头
            val header = getWavHeader(totalAudioLen, totalDataLen, longSampleRate.toLong(), channels, byteRate.toLong())
            randomAccessFile.write(header, 0, 44)
            //插入原内容
            hasRead = tmpFis.read(tmpByte)
            while (hasRead > 0) {
                randomAccessFile.write(tmpByte, 0, hasRead)
                hasRead = tmpFis.read(tmpByte)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            tmpFis?.close()
            tmpFos?.close()
            randomAccessFile?.close()
            fileOutputStream.close()
        }
    }


    fun pcm2wav(
        sourceFileName: String,
        dstFileName: String,
        isDeletePcm: Boolean,
        isRealTime: Boolean,
        lastLength: Long,
        isStoped: Boolean
    ): Boolean {
        var readPcm: FileInputStream
        var writeWav: FileOutputStream

        val byteRate = 16 * mSampleRate * mChannel / 8

        var bytes = ByteArray(mBufferSize)
        try {
            if (!isStoped) {
                readPcm = FileInputStream(sourceFileName)
                writeWav = FileOutputStream(dstFileName, isRealTime)
                val totalAudioLen = readPcm.channel.size()
                if (totalAudioLen <= 0) {
                    return false
                }
                //循环读出写入
                if (isRealTime) {
                    readPcm.skip(lastLength)
                }
                var size = readPcm.read(bytes)
                while (size != -1) {
                    writeWav.write(bytes)
                    size = readPcm.read(bytes)
                }
                readPcm.close()
                writeWav.close()
                //转换完成之后删除源文件
                if (isDeletePcm) {
                    val file = File(sourceFileName)
                    file.delete()
                }
                return true
            } else {

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun pcm2wav(sourceFileName: String, dstFileName: String, isDeletePcm: Boolean): Boolean {
        var readPcm: FileInputStream
        var writeWav: FileOutputStream

        val channels = 2
        val longSampleRate = mSampleRate
        val byteRate = 16 * mSampleRate * channels / 8

        var bytes = ByteArray(mBufferSize)
        try {
            readPcm = FileInputStream(sourceFileName)
            writeWav = FileOutputStream(dstFileName)
            val totalAudioLen = readPcm.channel.size()
            if (totalAudioLen <= 0) {
                return false
            }
            val totalDataLen = totalAudioLen + 36
            //添加头
            writeWaveFileHeader(
                writeWav,
                totalAudioLen,
                totalDataLen,
                longSampleRate.toLong(),
                channels,
                byteRate.toLong()
            )
            //循环读出写入
            var size = readPcm.read(bytes)
            while (size != -1) {
                writeWav.write(bytes)
                size = readPcm.read(bytes)
            }
            readPcm.close()
            writeWav.close()
            //转换完成之后删除源文件
            if (isDeletePcm) {
                val file = File(sourceFileName)
                file.delete()
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    private fun updateHeaderLength(dstFileName: String, totalAudioLen: Long, totalDataLen: Long) {
//        var writeWav = FileOutputStream(dstFileName)
//        val changeLength = ByteArray(4)
//        //修改data长度
//        changeLength[0] = (totalDataLen and 0xff).toByte()
//        changeLength[1] = (totalDataLen shr 8 and 0xff).toByte()
//        changeLength[2] = (totalDataLen shr 16 and 0xff).toByte()
//        changeLength[3] = (totalDataLen shr 24 and 0xff).toByte()
//        writeWav.write(changeLength, 4, 4)
//
//        //修改audio长度
//        changeLength[0] = (totalAudioLen and 0xff).toByte()
//        changeLength[1] = (totalAudioLen shr 8 and 0xff).toByte()
//        changeLength[2] = (totalAudioLen shr 16 and 0xff).toByte()
//        changeLength[3] = (totalAudioLen shr 24 and 0xff).toByte()
//        writeWav.write(changeLength, 40, 4)
//
//        //关闭
//        writeWav.close()
    }

    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ) {
        val header = getWavHeader(totalAudioLen, totalDataLen, longSampleRate, channels, byteRate)
        out.write(header, 0, 44)
    }

    fun getWavHeader(
        totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte() //WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (2 * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte() //data
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }
}