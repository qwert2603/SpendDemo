package com.qwert2603.spenddemo.records_list

import com.qwert2603.andrlib.base.mvi.PartialChange
import com.qwert2603.spenddemo.model.entity.RecordChange
import com.qwert2603.spenddemo.model.entity.RecordsListItem
import com.qwert2603.spenddemo.model.entity.SyncState
import com.qwert2603.spenddemo.utils.FastDiffUtils

sealed class RecordsListPartialChange : PartialChange {
    data class RecordsListChanged(
            val list: List<RecordsListItem>,
            val diff: FastDiffUtils.FastDiffResult,
            val recordsChanges: HashMap<String, RecordChange>
    ) : RecordsListPartialChange()

    data class ShowInfoChanged(val showInfo: ShowInfo) : RecordsListPartialChange()
    data class SumsInfoChanged(val sumsInfo: SumsInfo) : RecordsListPartialChange()
    data class LongSumPeriodDaysChanged(val days: Int) : RecordsListPartialChange()
    data class ShortSumPeriodMinutesChanged(val minutes: Int) : RecordsListPartialChange()

    data class SyncStateChanged(val syncState: SyncState) : RecordsListPartialChange()
}