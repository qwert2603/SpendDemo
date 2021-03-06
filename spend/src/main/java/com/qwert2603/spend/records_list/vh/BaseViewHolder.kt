package com.qwert2603.spend.records_list.vh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.qwert2603.spend.model.entity.RecordsListItem
import com.qwert2603.spend.records_list.RecordsListAdapter
import kotlinx.android.extensions.LayoutContainer

abstract class BaseViewHolder<T : RecordsListItem>(
        parent: ViewGroup,
        @LayoutRes layoutRes: Int
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)), LayoutContainer {

    override val containerView: View = itemView

    var t: T? = null
        protected set

    protected var adapter: RecordsListAdapter? = null
        private set

    init {
        itemView.setOnClickListener {
            val t = t ?: return@setOnClickListener
            val itemClicks = adapter?.itemClicks ?: return@setOnClickListener
            itemClicks.onNext(t)
        }
        itemView.setOnLongClickListener {
            val t = t ?: return@setOnLongClickListener false
            val itemLongClicks = adapter?.itemLongClicks ?: return@setOnLongClickListener false
            itemLongClicks.onNext(t)
            return@setOnLongClickListener true
        }
    }

    @CallSuper
    open fun bind(t: T, adapter: RecordsListAdapter) {
        this.t = t
        this.adapter = adapter
    }

    @CallSuper
    open fun unbind() {
        this.t = null
        this.adapter = null
    }
}