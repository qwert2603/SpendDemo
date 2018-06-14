package com.qwert2603.spenddemo.records_list_mvvm

import android.support.v4.widget.TextViewCompat
import android.util.TypedValue
import android.view.ViewGroup
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.records_list_mvvm.entity.ProfitUI
import com.qwert2603.spenddemo.utils.toFormattedString
import com.qwert2603.spenddemo.utils.toPointedString
import com.qwert2603.spenddemo.utils.zeroToEmpty
import kotlinx.android.synthetic.main.item_profit.view.*
import java.text.SimpleDateFormat
import java.util.*

class ProfitViewHolder(parent: ViewGroup) : BaseViewHolder<ProfitUI>(parent, R.layout.item_profit) {

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("H:mm", Locale.getDefault())
    }

    init {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                itemView.id_TextView,
                12,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP
        )
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                itemView.time_TextView,
                14,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP
        )
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                itemView.date_TextView,
                14,
                16,
                1,
                TypedValue.COMPLEX_UNIT_SP
        )
    }

    override fun bind(t: ProfitUI, adapter: RecordsListAdapter) = with(itemView) {
        super.bind(t, adapter)
        val showIds = adapter.showIds
        val showChangeKinds = adapter.showChangeKinds
        val showDatesInRecords = adapter.showDatesInRecords
        val showTimesInRecords = adapter.showTimesInRecords

        local_ImageView.setVisible(showChangeKinds)
        id_TextView.setVisible(showIds)
        date_TextView.setVisible(showDatesInRecords)
        time_TextView.setVisible(showTimesInRecords)

        id_TextView.text = t.id.toString()
        date_TextView.text = t.date.toFormattedString(resources)
        time_TextView.text = TIME_FORMAT.format(t.date)
        kind_TextView.text = t.kind
        value_TextView.text = t.value.toLong().toPointedString().zeroToEmpty()
    }
}