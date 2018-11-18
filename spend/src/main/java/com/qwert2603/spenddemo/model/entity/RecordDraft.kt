package com.qwert2603.spenddemo.model.entity

import com.qwert2603.spenddemo.model.rest.entity.RecordServer
import com.qwert2603.spenddemo.utils.Const
import com.qwert2603.spenddemo.utils.DateUtils
import java.util.*

data class RecordDraft(
        val uuid: String,
        val recordTypeId: Long,
        val date: Int?, // null means "now".
        val time: Int?,
        val kind: String,
        val value: Int
) {
    init {
        require(recordTypeId in Const.RECORD_TYPE_IDS)
        if (date == null) require(time == null)
    }

    fun isValid() = value > 0 && kind.length in 1..Const.MAX_KIND_LENGTH

    companion object {
        fun new(recordTypeId: Long) = RecordDraft(
                uuid = UUID.randomUUID().toString(),
                recordTypeId = recordTypeId,
                date = null,
                time = null,
                kind = "",
                value = 0
        )
    }
}

fun RecordDraft.toRecordServer(): RecordServer {
    val (nowDate, nowTime) = DateUtils.getNow()
    return RecordServer(
            uuid = uuid,
            recordTypeId = recordTypeId,
            date = date ?: nowDate,
            time = if (date != null) time else nowTime,
            kind = kind,
            value = value
    )
}

fun Record.toRecordDraft() = RecordDraft(
        uuid = uuid,
        recordTypeId = recordTypeId,
        date = date,
        time = time,
        kind = kind,
        value = value
)