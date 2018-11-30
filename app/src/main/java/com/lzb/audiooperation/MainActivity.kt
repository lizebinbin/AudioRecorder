package com.lzb.audiooperation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.lzb.record.AudioCallbackListener
import com.lzb.record.AudioRecorder
import com.lzb.record.RecordStatus
import com.lzb.record.effect.EffectManager
import com.lzb.record.effect.EffectUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, AudioCallbackListener {

    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
    private val PERMISSION_REQUEST_CODE = 1001
    private var currentStatus = RecordStatus.STATE_RELEASE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        //初始化
        AudioRecorder.getInstance().init(this, this)
        AudioRecorder.getInstance().setSaveType(AudioRecorder.SAVE_TYPE_PCM)

        EffectManager.getInstance().init(this)

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
                AudioRecorder.getInstance().prepareRecord(null)
                AudioRecorder.getInstance().start()
            }
            R.id.pauseRecord -> {
                if (currentStatus == RecordStatus.STATE_PAUSE) {
                    AudioRecorder.getInstance().resumeRecord()
                } else {
                    AudioRecorder.getInstance().pause()
                }
            }
            R.id.stopRecord -> AudioRecorder.getInstance().stop()
            R.id.changeVoice -> {
                val path = "file:///android_asset/bin.wav"
                EffectManager.getInstance().play(path, EffectUtils.MODE_DASHU)
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
            }
        }
    }

    override fun onRecordData(data: ByteArray, volume: Float) {

    }

    override fun onDestroy() {
        super.onDestroy()
        EffectManager.getInstance().close()
    }

//    override fun onSaveWav(isSaved: Boolean, filePath: String) {
//        Log.e("Main", "onSaveWav isSaved:$isSaved  filePath:$filePath")
//    }
}
