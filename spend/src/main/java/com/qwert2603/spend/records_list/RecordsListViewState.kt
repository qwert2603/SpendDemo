package com.qwert2603.spend.records_list

import com.qwert2603.spend.model.entity.*
import com.qwert2603.spend.utils.Const
import com.qwert2603.spend.utils.FastDiffUtils
import com.qwert2603.spend.utils.sumByLong

data class RecordsListViewState(
        val showInfo: ShowInfo,
        val longSumPeriod: Days,
        val shortSumPeriod: Minutes,
        val sumsInfo: SumsInfo,
        val sortByValue: Boolean,
        val showFilters: Boolean,
        val searchQuery: String,
        val startDate: SDate?,
        val endDate: SDate?,
        val records: List<RecordsListItem>?,
        val diff: FastDiffUtils.FastDiffResult,
        val recordsChanges: HashMap<String, RecordChange>, // key is Record::uuid
        val syncState: SyncState,
        private val _selectedRecordsUuids: HashSet<String>,
        val oldRecordsLock: Boolean
) {
    private val recordsByUuid: HashMap<String, Record>? by lazy {
        records
                ?.mapNotNull { it as? Record }
                ?.let { records ->
                    records.associateByTo(HashMap(records.size)) { it.uuid }
                }
    }

    val selectedRecordsUuids: HashSet<String> = _selectedRecordsUuids.filterTo(HashSet()) { recordsByUuid?.get(it)?.isDeleted() == false }

    val selectMode by lazy { selectedRecordsUuids.isNotEmpty() }

    val selectedSum by lazy {
        val recordsByUuid = recordsByUuid
        return@lazy if (recordsByUuid == null) {
            0L
        } else {
            selectedRecordsUuids.sumByLong { uuid ->
                val record = recordsByUuid[uuid]!!
                when {
                    record.recordCategory.recordTypeId == Const.RECORD_TYPE_ID_SPEND -> -1 * record.value
                    record.recordCategory.recordTypeId == Const.RECORD_TYPE_ID_PROFIT -> record.value
                    else -> null!!
                }.toLong()
            }
        }
    }

    private val selectedRecords: List<Record> by lazy {
        val recordsByUuid = recordsByUuid ?: return@lazy emptyList<Record>()
        selectedRecordsUuids.map { recordsByUuid[it]!! }
    }

    val canCombineSelected: Boolean by lazy {
        selectedRecords.size > 1
                && selectedRecords.all { it.isChangeable(oldRecordsLock) }
                && selectedRecords.distinctBy { it.recordCategory to it.kind }.size == 1
                && selectedRecords.sumByLong { it.value.toLong() } <= Int.MAX_VALUE
    }

    val canDeleteSelected by lazy { selectedRecords.all { it.isChangeable(oldRecordsLock) } }

    val canChangeSelected by lazy { selectedRecords.all { it.isChangeable(oldRecordsLock) } }

    fun createCombineAction(): RecordsListViewAction.AskToCombineRecords? {
        val recordsByUuid = recordsByUuid
        if (!canCombineSelected || recordsByUuid == null) return null
        val someSelectedRecord = recordsByUuid[selectedRecordsUuids.first()]!!
        return RecordsListViewAction.AskToCombineRecords(
                recordUuids = selectedRecordsUuids.toList(),
                categoryUuid = someSelectedRecord.recordCategory.uuid,
                kind = someSelectedRecord.kind
        )
    }
}