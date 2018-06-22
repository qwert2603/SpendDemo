package com.qwert2603.spenddemo.records_list_mvvm

import android.support.v4.widget.TextViewCompat
import android.util.TypedValue
import android.view.ViewGroup
import com.qwert2603.andrlib.util.color
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.model.entity.ChangeKind
import com.qwert2603.spenddemo.model.entity.SyncStatus
import com.qwert2603.spenddemo.records_list_mvvm.entity.SpendUI
import com.qwert2603.spenddemo.utils.setStrike
import com.qwert2603.spenddemo.utils.toFormattedString
import com.qwert2603.spenddemo.utils.toPointedString
import com.qwert2603.spenddemo.utils.zeroToEmpty
import kotlinx.android.synthetic.main.item_spend.view.*
import java.text.SimpleDateFormat
import java.util.*

class SpendViewHolder(parent: ViewGroup) : BaseViewHolder<SpendUI>(parent, R.layout.item_spend) {

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

    override fun bind(t: SpendUI, adapter: RecordsListAdapter) = with(itemView) {
        super.bind(t, adapter)
        val showIds = adapter.showIds
        val showChangeKinds = adapter.showChangeKinds
        val showDatesInRecords = adapter.showDatesInRecords
        val showTimesInRecords = adapter.showTimesInRecords

        local_ImageView.setVisible(showChangeKinds)
        id_TextView.setVisible(showIds)
        date_TextView.setVisible(showDatesInRecords)
        time_TextView.setVisible(showTimesInRecords)

        local_ImageView.setImageResource(when (t.syncStatus()) {
            SyncStatus.LOCAL -> R.drawable.ic_local
            SyncStatus.SYNCING -> R.drawable.ic_syncing
            SyncStatus.REMOTE -> R.drawable.ic_done_24dp
        })
        if (t.changeKind != null) {
            local_ImageView.setColorFilter(resources.color(when (t.changeKind) {
                ChangeKind.INSERT -> R.color.local_change_create
                ChangeKind.UPDATE -> R.color.local_change_edit
                ChangeKind.DELETE -> R.color.local_change_delete
            }))
        } else {
            local_ImageView.setColorFilter(resources.color(R.color.anth))
        }
        id_TextView.text = t.id.toString()
        date_TextView.text = t.date.toFormattedString(resources)
        time_TextView.text = TIME_FORMAT.format(t.date)
        kind_TextView.text = t.kind
        value_TextView.text = t.value.toLong().toPointedString().zeroToEmpty()

        isClickable = t.canEdit
        isLongClickable = t.canDelete

        val strike = showChangeKinds && t.changeKind == ChangeKind.DELETE
        listOf(id_TextView, date_TextView, time_TextView, kind_TextView, value_TextView)
                .forEach { it.setStrike(strike) }
    }

    private fun SpendUI.syncStatus(): SyncStatus = when {
        this.idInList() in (adapter?.syncingItemIdsInList ?: emptySet()) -> SyncStatus.SYNCING
        this.changeKind != null -> SyncStatus.LOCAL
        else -> SyncStatus.REMOTE
    }
}