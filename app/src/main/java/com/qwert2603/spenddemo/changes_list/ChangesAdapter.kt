package com.qwert2603.spenddemo.changes_list

import android.view.ViewGroup
import com.qwert2603.spenddemo.base_mvi.load_refresh.list.recyclerview.BaseRecyclerViewAdapter
import com.qwert2603.spenddemo.model.entity.Change

class ChangesAdapter : BaseRecyclerViewAdapter<Change>() {
    override fun onCreateViewHolderModel(parent: ViewGroup, viewType: Int) = ChangeViewHolder(parent)
}