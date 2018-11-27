package com.lzb.record

import android.media.MediaPlayer

/**
 * Created by lzb on 2018/11/22.
 */
class AudioPlayer {
    private lateinit var mMediaPlayer: MediaPlayer

    fun init() {
        mMediaPlayer = MediaPlayer()

    }

    fun play(dataSource: String) {
        mMediaPlayer.prepare()
        mMediaPlayer.setDataSource(dataSource)
        mMediaPlayer.start()
    }

    fun stop() {
        if (mMediaPlayer.isPlaying) {
            mMediaPlayer.stop()
        }
    }

    fun release() {
        mMediaPlayer.release()
    }
}