package com.lzb.record

/**
 * Created by lzb on 2018/11/21.
 */
interface AudioCallbackListener {
    /**
     * 录音状态变化
     * @param status 当前状态
     * @param filePath 当前录制保存的文件地址
     */
    fun onStatusChange(status: RecordStatus, filePath: String)

    //录音数据，实时从硬件读取
    fun onRecordData(data: ByteArray, volume: Float)

//    //录音结束后保存文件
//    fun onSaveWav(isSaved: Boolean, filePath: String)
}