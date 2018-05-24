package com.qwert2603.spenddemo.records_list_mvvm

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.qwert2603.andrlib.util.Const
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.spenddemo.records_list.entity.*
import com.qwert2603.spenddemo.utils.FastDiffUtils

class RecordsListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var list: List<RecordsListItem> = emptyList()
        set(value) {
            val oldList = field
            field = value

            // todo: background thread.
            FastDiffUtils.fastCalculateDiff(
                    oldList = oldList,
                    newList = field,
                    id = { this.id() },
                    compareOrder = { r1, r2 ->
                        r2.time().compareTo(r1.time())
                                .takeIf { it != 0 }
                                ?: r1.priority().compareTo(r2.priority())
                    },
                    isEqual = { r1, r2 -> r1 == r2 }
            )
                    .also { LogUtils.d("RecordsListAdapter fastCalculateDiff $it") }
                    .dispatchToAdapter(this)
        }

    override fun getItemCount() = list.size

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_SPEND -> SpendViewHolder(parent)
        VIEW_TYPE_DATE_SUM -> DateSumViewHolder(parent)
        VIEW_TYPE_PROFIT -> ProfitViewHolder(parent)
        VIEW_TYPE_TOTALS -> TotalsViewHolder(parent)
        VIEW_TYPE_MONTH_SUM -> MonthSumViewHolder(parent)
        else -> null!!
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? SpendViewHolder)?.bind(list[position] as? SpendUI)
        (holder as? ProfitViewHolder)?.bind(list[position] as? ProfitUI)
        (holder as? TotalsViewHolder)?.bind(list[position] as? TotalsUI)
        (holder as? DateSumViewHolder)?.bind(list[position] as? DateSumUI)
        (holder as? MonthSumViewHolder)?.bind(list[position] as? MonthSumUI)
    }

    override fun getItemViewType(position: Int) = when (list[position]) {
        is SpendUI -> VIEW_TYPE_SPEND
        is DateSumUI -> VIEW_TYPE_DATE_SUM
        is ProfitUI -> VIEW_TYPE_PROFIT
        is TotalsUI -> VIEW_TYPE_TOTALS
        is MonthSumUI -> VIEW_TYPE_MONTH_SUM
        else -> null!!
    }

    companion object {
        const val VIEW_TYPE_SPEND = 1
        const val VIEW_TYPE_DATE_SUM = 2
        const val VIEW_TYPE_PROFIT = 3
        const val VIEW_TYPE_TOTALS = 4
        const val VIEW_TYPE_MONTH_SUM = 5

        private fun RecordsListItem.id() = when (this) {
            is SpendUI -> this.id + 1_000_000_000L
            is DateSumUI -> this.date.time / Const.MILLIS_PER_DAY + 3_000_000_000L
            is ProfitUI -> this.id + 2_000_000_000L
            is TotalsUI -> 1918L
            is MonthSumUI -> this.month.time / Const.MILLIS_PER_DAY + 4_000_000_000L
            else -> null!!
        }

        private fun RecordsListItem.time() = when (this) {
            is SpendUI -> this.date.time
            is DateSumUI -> this.date.time
            is ProfitUI -> this.date.time
            is TotalsUI -> Long.MIN_VALUE
            is MonthSumUI -> this.month.time
            else -> null!!
        }

        private fun RecordsListItem.priority() = when (this) {
            is SpendUI -> 1
            is ProfitUI -> 2
            is DateSumUI -> 3
            is MonthSumUI -> 4
            is TotalsUI -> 5
            else -> null!!
        }

    }
}