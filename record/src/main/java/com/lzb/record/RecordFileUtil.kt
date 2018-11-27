package com.lzb.record

import android.content.Context
import android.media.AudioFormat
import android.support.v4.content.ContextCompat
import java.io.File

/**
 * Created by lzb on 2018/11/21.
 */
class RecordFileUtil {
    companion object {
        private const val PCM_PATH = "cache_pcm"

        fun getDefaultRecordDirectory(context: Context): File {
            val dataDirectory = ContextCompat.getExternalCacheDirs(context)[0]
            val directory = File(dataDirectory, PCM_PATH)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return directory
        }

        fun parsePcm2Wav(sourceFile: String, dstFile: String) {
            val sampleRate = 16000
            val sampleSizeInBits = 16
            val channels = 1

//            val af:AudioFormat = AudioFormat()
        }
    }
}