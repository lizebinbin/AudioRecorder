package com.lzb.record

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.text.TextUtils
import android.util.Log
import com.lzb.record.event.EventHandleData
import com.lzb.record.play.AudioTracker
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Created by lzb on 2018/11/21.
 */
class AudioRecorder private constructor() {
    private val TAG = this::class.java.simpleName
    //当前状态
    private var currentStatus = RecordStatus.STATE_RELEASE
    //默认存储目录
    private var recordCacheDirectory: File? = null
    //音频来源-麦克风
    private var audioSource = MediaRecorder.AudioSource.MIC
    // 音频采样率 44100，官方文档表示这个采样率兼容性最好
    private val AUDIO_SAMPLE_RETE = 44100
    //声道  双声道
    private val AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO
    //音频采集精度
    private val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    //缓冲区字节大小
    private var bufferSizeInBytes: Int = 0

    private lateinit var mAudioRecord: AudioRecord
    //文件名
    private var fileName: String? = null
    //格式化文件名用
    private lateinit var dateFormat: SimpleDateFormat

    private var mExecutorService: ExecutorService = Executors.newCachedThreadPool()

    private lateinit var pcmToWaveUtil: PcmToWaveUtil

    private var callbackListener: AudioCallbackListener? = null
    private var isPaused = false

    private var fosWritePCM: FileOutputStream? = null
    private var fosWriteAAC: FileOutputStream? = null
    private var fosWriteWAV: FileOutputStream? = null

    private var currentSaveType = SAVE_TYPE_AAC

    private var mAacEncoder: AACEncoder? = null

    private var isNeedPlayRealTime = false

    init {
        EventBus.getDefault().register(this)
    }

    companion object {
        const val SAVE_TYPE_AAC = 1
        const val SAVE_TYPE_WAV = 2
        const val SAVE_TYPE_PCM = 3

        private var mInstance: AudioRecorder? = null
        @Synchronized
        fun getInstance(): AudioRecorder {
            if (mInstance == null) {
                mInstance = AudioRecorder()
            }
            return mInstance!!
        }
    }

    fun init(context: Context?, callbackListener: AudioCallbackListener? = null) {
        if (context == null)
            throw NullPointerException("context is null when init AudioRecorder")
        this.callbackListener = callbackListener
        recordCacheDirectory = RecordFileUtil.getDefaultRecordDirectory(context.applicationContext)
        dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA)

    }

    /**
     * 准备录音
     */
    fun prepareRecord(fileName: String?, isNeedPlayRealTime: Boolean) {
        if (currentStatus != RecordStatus.STATE_RELEASE) {
            throw IllegalStateException("当前状态为$currentStatus")
        }
        if (recordCacheDirectory == null)
            throw IllegalStateException("call the method init() before createDefaultAudio()")
        this.isNeedPlayRealTime = isNeedPlayRealTime
        if (TextUtils.isEmpty(fileName)) {
            this.fileName = dateFormat.format(System.currentTimeMillis()) + ".pcm"
        } else {
            this.fileName = fileName
        }
        //获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RETE, AUDIO_CHANNEL, AUDIO_ENCODING)
        mAudioRecord = AudioRecord(audioSource, AUDIO_SAMPLE_RETE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes)
        updateStatus(RecordStatus.STATE_READY)
        if (isNeedPlayRealTime)
            AudioTracker.getInstance().prepare()
    }

    fun start() {
        if (currentStatus == RecordStatus.STATE_RELEASE) {
            throw IllegalStateException("录音尚未初始化，调用createDefaultAudio()方法初始化")
        }
        if (currentStatus == RecordStatus.STATE_RECORDING) {
            throw IllegalStateException("正在录音...")
        }
        mAudioRecord.startRecording()
        updateStatus(RecordStatus.STATE_RECORDING)
        if (isNeedPlayRealTime)
            AudioTracker.getInstance().start()
        //保存到文件
        mExecutorService.execute {
            Log.e("AudioRecorder", "start record")
            setSaveConfig()
            handleAudioBuffer()
        }
    }

    fun pause() {
        if (currentStatus == RecordStatus.STATE_RECORDING) {
            mAudioRecord.stop()
            updateStatus(RecordStatus.STATE_PAUSE)
            isPaused = true
        }
    }

    fun resumeRecord() {
        if (currentStatus != RecordStatus.STATE_PAUSE)
            throw IllegalStateException("不在暂停状态，不能恢复")
        mAudioRecord.startRecording()
        updateStatus(RecordStatus.STATE_RECORDING)
        //保存到文件
        mExecutorService.execute {
            handleAudioBuffer()
        }
    }

    fun stop() {
        if (currentStatus == RecordStatus.STATE_RECORDING || currentStatus == RecordStatus.STATE_PAUSE) {
            mAudioRecord.stop()
            Log.e("AudioRecorder", "stop record")
            updateStatus(RecordStatus.STATE_STOP)
            release()

            if (isNeedPlayRealTime)
                AudioTracker.getInstance().stop()
        }
    }

    private fun release() {
        mAudioRecord.release()
        updateStatus(RecordStatus.STATE_RELEASE)
        //结束录音后再次调用处理方法用于关闭流、添加头等操作
        handleAudioBuffer()

//        pcmToWaveUtil = PcmToWaveUtil()
//        mExecutorService.execute {
//            val audioFile = File(recordCacheDirectory, fileName)
//            val desFileName = fileName?.substring(0, fileName?.indexOf(".pcm")!!) + "-release.wav"
//            val dstFile = File(recordCacheDirectory, desFileName)
//            val changeResult = pcmToWaveUtil.pcm2wav(audioFile.absolutePath, dstFile.absolutePath, false)
//        }
    }

    fun setSaveType(saveType: Int) {
        if (saveType != SAVE_TYPE_AAC && saveType != SAVE_TYPE_PCM && saveType != SAVE_TYPE_WAV) {
            throw IllegalArgumentException("save type can only be AudioRecorder.SAVE_TYPE_AAC,AudioRecorder.SAVE_TYPE_AAC and AudioRecorder.SAVE_TYPE_AAC")
        }
        currentSaveType = saveType
    }

    //更新状态，回调
    private fun updateStatus(status: RecordStatus) {
        if (currentStatus == status)
            return
        currentStatus = status
        val filePath = recordCacheDirectory!!.absolutePath + File.separator + fileName
        if (callbackListener != null)
            callbackListener!!.onStatusChange(currentStatus, filePath)
    }

    private fun handleAudioBuffer() {
        //创建一个接受单元
        val readBuffer = ByteArray(bufferSizeInBytes)
        //每次读取到的长度
        var readSize = 0
        while (currentStatus == RecordStatus.STATE_RECORDING) {
            readSize = mAudioRecord.read(readBuffer, 0, bufferSizeInBytes)

            if (callbackListener != null)
                callbackListener!!.onRecordData(readBuffer, 3f);

            //处理readBuffer，
            saveBuffer2Pcm(readSize, readBuffer)
            saveBuffer2WAV(readSize, readBuffer)
            saveBuffer2AAC(readSize, readBuffer)
            if (isNeedPlayRealTime)
                AudioTracker.getInstance().play(readBuffer)
        }
        if (currentStatus == RecordStatus.STATE_RELEASE) {
            try {
                if (currentSaveType == SAVE_TYPE_WAV && fosWriteWAV != null) {
                    mExecutorService.execute {
                        val saveFileName = fileName?.substring(0, fileName?.lastIndexOf(".")!!)
                        val wavFilePath = recordCacheDirectory!!.absolutePath + File.separator + "$saveFileName.wav"
                        pcmToWaveUtil = PcmToWaveUtil()
                        pcmToWaveUtil.addHeader2wav(wavFilePath, fosWriteWAV!!)
                    }
                } else {
                    fosWriteWAV?.close()
                }

                fosWritePCM?.close()
                fosWriteAAC?.close()
            } catch (e: IOException) {
            }
        }

    }

    private fun setSaveConfig() {
        fosWriteAAC = null
        fosWritePCM = null
        fosWriteWAV = null
        val saveFileName = fileName?.substring(0, fileName?.lastIndexOf(".")!!)
        when (currentSaveType) {
            SAVE_TYPE_PCM -> {
                val pcmFilePath = recordCacheDirectory!!.absolutePath + File.separator + "$saveFileName.pcm"
                fosWritePCM = FileOutputStream(pcmFilePath)
            }
            SAVE_TYPE_WAV -> {
                val wavFilePath = recordCacheDirectory!!.absolutePath + File.separator + "$saveFileName.wav"
                fosWriteWAV = FileOutputStream(wavFilePath)
            }
            SAVE_TYPE_AAC -> {
                val aacFilePath = recordCacheDirectory!!.absolutePath + File.separator + "$saveFileName.aac"
                fosWriteAAC = FileOutputStream(aacFilePath)
                mAacEncoder = AACEncoder()
                mAacEncoder!!.prepare()
            }
        }
    }

    /**
     * 将读取到的音频存储到pcm文件
     */
    private fun saveBuffer2Pcm(readSize: Int, readBuffer: ByteArray) {
        //将读取到的数据写到.pcm文件中
        if (readSize != AudioRecord.ERROR_INVALID_OPERATION && fosWritePCM != null) {
            fosWritePCM?.write(readBuffer, 0, readBuffer.size)
        }
    }

    /**
     * 将读取到的音频存储到aac文件
     */
    private fun saveBuffer2AAC(readSize: Int, readBuffer: ByteArray) {
        if (mAacEncoder != null && fosWriteAAC != null && readSize != AudioRecord.ERROR_INVALID_OPERATION) {
            mAacEncoder!!.encode(readSize, readBuffer, fosWriteAAC!!)
        }
    }

    /**
     * 将读取到的音频存储到wav文件
     */
    private fun saveBuffer2WAV(readSize: Int, readBuffer: ByteArray) {
        if (readSize != AudioRecord.ERROR_INVALID_OPERATION) {
            fosWriteWAV?.write(readBuffer, 0, readBuffer.size)
        }
    }


    @Subscribe
    fun onEvent(handleData: EventHandleData) {
        if (handleData.dataArray.isNotEmpty()) {
            AudioTracker.getInstance().play(handleData.dataArray)
        }
    }

}