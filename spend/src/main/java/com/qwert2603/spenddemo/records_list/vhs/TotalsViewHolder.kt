package com.qwert2603.spenddemo.records_list.vhs

import android.view.ViewGroup
import com.qwert2603.andrlib.base.recyclerview.BaseRecyclerViewHolder
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.records_list.entity.TotalsUI
import com.qwert2603.spenddemo.utils.toPointedString
import kotlinx.android.synthetic.main.item_totals.view.*

class TotalsViewHolder(parent: ViewGroup) : BaseRecyclerViewHolder<TotalsUI>(parent, R.layout.item_totals) {
    override fun bind(m: TotalsUI) = with(itemView) {
        super.bind(m)

        profits_TextView.setVisible(m.showProfits)
        spends_TextView.setVisible(m.showSpends)
        profits_TextView.text = resources.getString(
                R.string.text_total_items_format,
                resources.getQuantityString(R.plurals.profits, m.profitsCount, m.profitsCount),
                m.profitsSum.toPointedString()
        )
        spends_TextView.text = resources.getString(
                R.string.text_total_items_format,
                resources.getQuantityString(R.plurals.spends, m.spendsCount, m.spendsCount),
                m.spendsSum.toPointedString()
        )
        total_TextView.text = resources.getString(R.string.text_total_balance_format, m.totalBalance.toPointedString())
    }
}