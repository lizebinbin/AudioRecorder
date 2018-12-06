package com.lzb.audiooperation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.lzb.record.AudioCallbackListener
import com.lzb.record.AudioRecorder
import com.lzb.record.RecordFileUtil
import com.lzb.record.RecordStatus
import com.lzb.record.effect.EffectManager
import com.lzb.record.effect.EffectUtils
import com.lzb.record.play.AudioTracker
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener, AudioCallbackListener {

    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
    private val PERMISSION_REQUEST_CODE = 1001
    private var currentStatus = RecordStatus.STATE_RELEASE
    private lateinit var testPCMPath: String
    private lateinit var testDstPCMPath: String
    private var saveFileType = AudioRecorder.SAVE_TYPE_PCM
    private var recordFilePath: String? = null
    private var isOpenPlayRealTime = false
    private var mToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        //初始化
        AudioRecorder.getInstance().init(this, this)
        AudioRecorder.getInstance().setSaveType(AudioRecorder.SAVE_TYPE_PCM)

        EffectManager.getInstance().init(this)

        testPCMPath = RecordFileUtil.getDefaultRecordDirectory(this).absolutePath + File.separator +
                "20181205-144755.pcm"
        testDstPCMPath = RecordFileUtil.getDefaultRecordDirectory(this).absolutePath + File.separator +
                "20181205-144755-faster.pcm"

        rg_SelectFileType.setOnCheckedChangeListener { group, checkId ->
            when (checkId) {
                R.id.rb_savePCM -> saveFileType = AudioRecorder.SAVE_TYPE_PCM
                R.id.rb_saveWAV -> saveFileType = AudioRecorder.SAVE_TYPE_WAV
                R.id.rb_saveAAC -> saveFileType = AudioRecorder.SAVE_TYPE_AAC
            }
            AudioRecorder.getInstance().setSaveType(saveFileType)
        }
        playSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            isOpenPlayRealTime = isChecked
            if (isChecked) {
                Toast.makeText(this@MainActivity, "开启实时播放", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "关闭实时播放", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermission() {
        val checkSelfPermission = ContextCompat.checkSelfPermission(this, permissions[0])
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onClick(v: View?) {
        if (v == null)
            return
        when (v.id) {
            R.id.startRecord -> {
                if (currentStatus == RecordStatus.STATE_RELEASE) {
                    AudioRecorder.getInstance().prepareRecord(null, isOpenPlayRealTime)
                    AudioRecorder.getInstance().start()
                } else {
                    showToast("请检查当前状态！")
                }
            }
            R.id.pauseRecord -> {
                if (currentStatus == RecordStatus.STATE_PAUSE) {
                    AudioRecorder.getInstance().resumeRecord()
                } else {
                    AudioRecorder.getInstance().pause()
                }
            }
            R.id.stopRecord -> AudioRecorder.getInstance().stop()
            R.id.change_luoli -> playChangeVoice(EffectUtils.MODE_LUOLI)
            R.id.change_dashu -> playChangeVoice(EffectUtils.MODE_DASHU)
            R.id.change_gaoguai -> playChangeVoice(EffectUtils.MODE_GAOGUAI)
            R.id.change_normal -> playChangeVoice(EffectUtils.MODE_NORMAL)
            R.id.change_jingsong -> playChangeVoice(EffectUtils.MODE_JINGSONG)
            R.id.change_kongling -> playChangeVoice(EffectUtils.MODE_KONGLING)
            R.id.change_hechang -> playChangeVoice(EffectUtils.MODE_HECHANG)


            R.id.changeVoice -> {
                val path = "file:///android_asset/bin.wav"
                EffectManager.getInstance().play(path, EffectUtils.MODE_DASHU)
            }
            R.id.stopChangeVoice -> {
                EffectManager.getInstance().stop()
            }
            R.id.downVolume -> {
                EffectManager.getInstance().downVolume(testPCMPath, testDstPCMPath)
            }
            R.id.fasterPCM -> EffectManager.getInstance().fasterPCM(testPCMPath, testDstPCMPath)
            R.id.slowerPCM -> EffectManager.getInstance().slowerPCM(testPCMPath, testDstPCMPath)
            R.id.playPCM -> {
                AudioTracker.getInstance().prepare()
                AudioTracker.getInstance().start()
                AudioTracker.getInstance().playPCMFile(testPCMPath)
            }
            R.id.playPCMChange -> {
                AudioTracker.getInstance().prepare()
                AudioTracker.getInstance().start()
                AudioTracker.getInstance().playPCMFile(testDstPCMPath)
            }
        }
    }

    override fun onStatusChange(status: RecordStatus, filePath: String) {
        currentStatus = status
        when (status) {
            RecordStatus.STATE_RECORDING -> {
                startRecord.text = "正在录音..."
                pauseRecord.text = "暂停录音"
//                EffectManager.getInstance().play(filePath, EffectUtils.MODE_DASHU)
            }
            RecordStatus.STATE_PAUSE -> {
                startRecord.text = "录音暂停..."
                pauseRecord.text = "继续录音"
            }
            RecordStatus.STATE_STOP -> {
                startRecord.text = "正在保存..."
                pauseRecord.text = "暂停录音"
            }
            RecordStatus.STATE_RELEASE -> {
                startRecord.text = "开始录音"
                pauseRecord.text = "暂停录音"
                recordFilePath = filePath
            }
        }
    }

    override fun onRecordData(data: ByteArray, volume: Float) {
//        EffectManager.getInstance().sendRealTimePCM(data)
    }

    override fun onDestroy() {
        super.onDestroy()
        EffectManager.getInstance().close()
    }

    private fun playChangeVoice(type: Int) {
        if (currentStatus != RecordStatus.STATE_RELEASE) {
            showToast("请先结束录音！")
            return
        }
        if (saveFileType == AudioRecorder.SAVE_TYPE_AAC) {
            showToast("暂不支持aac类型，请选择其他类型")
            return
        }
        if (recordFilePath != null) {
            EffectManager.getInstance().stop()
            val path = "file:///android_asset/bin.wav"
            EffectManager.getInstance().play(recordFilePath!!, type)
        } else {
            showToast("文件地址错误")
        }
    }

    private fun showToast(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }
//    override fun onSaveWav(isSaved: Boolean, filePath: String) {
//        Log.e("Main", "onSaveWav isSaved:$isSaved  filePath:$filePath")
//    }
}
