package com.qwert2603.spenddemo.records_list

import android.view.ViewGroup
import com.qwert2603.andrlib.base.recyclerview.BaseRecyclerViewAdapter
import com.qwert2603.andrlib.base.recyclerview.BaseRecyclerViewHolder
import com.qwert2603.andrlib.model.IdentifiableLong
import com.qwert2603.spenddemo.records_list.entity.DateSum
import com.qwert2603.spenddemo.records_list.entity.RecordUI
import com.qwert2603.spenddemo.records_list.entity.RecordsListItem

class RecordsAdapter : BaseRecyclerViewAdapter<RecordsListItem>() {
    companion object {
        const val VIEW_TYPE_RECORD = 1
        const val VIEW_TYPE_DATE_SUM = 2
    }

    var showChangeKinds = true
    var showIds = true
    var showDatesInRecords = true

    override fun getItemViewTypeModel(m: RecordsListItem) = when (m) {
        is RecordUI -> VIEW_TYPE_RECORD
        is DateSum -> VIEW_TYPE_DATE_SUM
        else -> null!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolderModel(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_RECORD -> RecordViewHolder(parent)
        VIEW_TYPE_DATE_SUM -> DateSumViewHolder(parent)
        else -> null!!
    } as BaseRecyclerViewHolder<RecordsListItem>

    override fun onBindViewHolder(holder: BaseRecyclerViewHolder<IdentifiableLong>, position: Int, payloads: MutableList<Any>) {
        if (RecordsListAnimator.PAYLOAD_HIGHLIGHT in payloads) return
        super.onBindViewHolder(holder, position, payloads)
    }
}