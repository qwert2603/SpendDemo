package com.qwert2603.spenddemo.records_list.vhs

import android.view.ViewGroup
import com.qwert2603.andrlib.base.recyclerview.BaseRecyclerViewHolder
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.records_list.entity.DateSumUI
import com.qwert2603.spenddemo.utils.toFormattedString
import com.qwert2603.spenddemo.utils.toPointedString
import com.qwert2603.spenddemo.utils.zeroToEmpty
import kotlinx.android.synthetic.main.item_date_sum.view.*

class DateSumViewHolder(parent: ViewGroup) : BaseRecyclerViewHolder<DateSumUI>(parent, R.layout.item_date_sum) {
    override fun bind(m: DateSumUI) = with(itemView) {
        super.bind(m)
        date_TextView.text = m.date.toFormattedString(resources)
        profitsSum_TextView.setVisible(m.showProfits)
        profitsSum_TextView.text = m.profits.toPointedString().zeroToEmpty()
        spendsSum_TextView.setVisible(m.showSpends)
        spendsSum_TextView.text = m.spends.toPointedString().zeroToEmpty()
    }
}