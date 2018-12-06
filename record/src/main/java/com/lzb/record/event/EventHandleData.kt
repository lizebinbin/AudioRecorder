package com.lzb.record.event

import java.util.*

/**
 * Created by lzb on 2018/12/5.
 */
data class EventHandleData(
    var dataArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventHandleData

        if (!Arrays.equals(dataArray, other.dataArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(dataArray)
    }
}