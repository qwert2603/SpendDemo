package com.qwert2603.spenddemo.records_list

import com.qwert2603.andrlib.base.mvi.BaseView
import com.qwert2603.spenddemo.model.entity.CreatingProfit
import com.qwert2603.spenddemo.model.entity.Profit
import com.qwert2603.spenddemo.model.entity.Spend
import com.qwert2603.spenddemo.records_list.entity.ProfitUI
import com.qwert2603.spenddemo.records_list.entity.SpendUI
import io.reactivex.Observable

interface RecordsListView : BaseView<RecordsListViewState> {
    fun viewCreated(): Observable<Any>

    fun editSpendClicks(): Observable<SpendUI>
    fun deleteSpendClicks(): Observable<SpendUI>

    fun showChangesClicks(): Observable<Any>

    fun deleteSpendConfirmed(): Observable<Long>
    fun editSpendConfirmed(): Observable<Spend>

    fun sendRecordsClicks(): Observable<Any>
    fun showAboutClicks(): Observable<Any>

    fun showIdsChanges(): Observable<Boolean>
    fun showChangeKindsChanges(): Observable<Boolean>
    fun showDateSumsChanges(): Observable<Boolean>
    fun showMonthSumsChanges(): Observable<Boolean>
    fun showSpendsChanges(): Observable<Boolean>
    fun showProfitsChanges(): Observable<Boolean>

    fun addProfitClicks(): Observable<Any>
    fun editProfitClicks(): Observable<ProfitUI>
    fun deleteProfitClicks(): Observable<ProfitUI>

    fun addProfitConfirmed(): Observable<CreatingProfit>
    fun editProfitConfirmed(): Observable<Profit>
    fun deleteProfitConfirmed(): Observable<Long>

    fun addStubSpendsClicks(): Observable<Any>
    fun addStubProfitsClicks(): Observable<Any>
    fun clearAllClicks(): Observable<Any>
}