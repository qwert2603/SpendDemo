package com.qwert2603.spenddemo.records_list

import com.qwert2603.spenddemo.records_list.entity.RecordsListItem

data class RecordsListViewState(
        val records: List<RecordsListItem>,
        val changesCount: Int,
        val showChangeKinds: Boolean,
        val showIds: Boolean,
        val showDateSums: Boolean,
        val balance30Days: Int
)