package com.qwert2603.spenddemo.records_list_mvvm

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.qwert2603.spenddemo.model.entity.CreatingProfit
import com.qwert2603.spenddemo.model.entity.CreatingSpend
import com.qwert2603.spenddemo.model.entity.Profit
import com.qwert2603.spenddemo.model.entity.Spend
import com.qwert2603.spenddemo.model.repo.ProfitsRepo
import com.qwert2603.spenddemo.model.repo.SpendsRepo
import com.qwert2603.spenddemo.model.repo.UserSettingsRepo
import com.qwert2603.spenddemo.records_list_mvvm.entity.RecordsListItem
import com.qwert2603.spenddemo.utils.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.*

class RecordsListViewModel(
        private val spendsRepo: SpendsRepo,
        private val profitsRepo: ProfitsRepo,
        private val userSettingsRepo: UserSettingsRepo
) : ViewModel() {

    val showSpends = MutableLiveData<Boolean>()
    val showProfits = MutableLiveData<Boolean>()
    val showDateSums = MutableLiveData<Boolean>()
    val showMonthSums = MutableLiveData<Boolean>()
    val showIds = MutableLiveData<Boolean>()
    val showChangeKinds = MutableLiveData<Boolean>()
    val showBalance = MutableLiveData<Boolean>()
    val showTimes = MutableLiveData<Boolean>()

    val sendRecords = SingleLiveEvent<String>()

    init {
        showSpends.value = userSettingsRepo.showSpends
        showProfits.value = userSettingsRepo.showProfits
        showDateSums.value = userSettingsRepo.showDateSums
        showMonthSums.value = userSettingsRepo.showMonthSums
        showIds.value = userSettingsRepo.showIds
        showChangeKinds.value = userSettingsRepo.showChangeKinds
        showBalance.value = userSettingsRepo.showBalance
        showTimes.value = userSettingsRepo.showTimes

        spendsRepo.locallyEditedSpends().observeForever { pendingMovedSpendId = it?.id }
        profitsRepo.locallyEditedProfits().observeForever { pendingMovedProfitId = it?.id }
    }

    data class ShowInfo(
            val showSpends: Boolean,
            val showProfits: Boolean,
            val showDateSums: Boolean,
            val showMonthSums: Boolean
    ) {
        fun showSpendSum() = showSpends || !showProfits
        fun showProfitSum() = showProfits || !showSpends
        fun showSpendsEnable() = showProfits || showDateSums || showMonthSums
        fun showProfitsEnable() = showSpends || showDateSums || showMonthSums
        fun showDateSumsEnable() = showSpends || showProfits || showMonthSums
        fun showMonthSumsEnable() = showSpends || showProfits || showDateSums
        fun newProfitEnable() = showProfits
        fun newSpendVisible() = showSpends
        fun showFloatingDate() = showDateSums && (showSpends || showProfits)
    }

    val showInfo = combineLatest(listOf(showSpends, showProfits, showDateSums, showMonthSums))
            .map {
                ShowInfo(
                        showSpends = it[0] == true,
                        showProfits = it[1] == true,
                        showDateSums = it[2] == true,
                        showMonthSums = it[3] == true
                )
            }

    private val recordsList = spendsRepo.getRecordsList()

    var pendingMovedSpendId: Long? = null
    var pendingMovedProfitId: Long? = null

    val recordsLiveData: LiveData<Pair<List<RecordsListItem>, FastDiffUtils.FastDiffResult>> = showInfo
            .switchMap { showInfo ->
                recordsList
                        .mapBG { it.toRecordItemsList(showInfo) }
            }
            .pairWithPrev()
            .mapBG {
                val fastDiffResult = FastDiffUtils.fastCalculateDiff(
                        oldList = it.first ?: emptyList(),
                        newList = it.second ?: emptyList(),
                        id = { this.idInList() },
                        compareOrder = { r1, r2 ->
                            return@fastCalculateDiff r2.time().compareTo(r1.time())
                                    .takeIf { it != 0 }
                                    ?: r2.priority().compareTo(r1.priority())
                                            .takeIf { it != 0 }
                                    ?: r2.id.compareTo(r1.id)
                        },
                        isEqual = { r1, r2 -> r1 == r2 },
                        possiblyMovedItemIds = listOfNotNull(
                                pendingMovedSpendId?.plus(RecordsListItem.ADDENDUM_ID_SPEND),
                                pendingMovedProfitId?.plus(RecordsListItem.ADDENDUM_ID_PROFIT)
                        )
                )
                pendingMovedSpendId = null
                pendingMovedProfitId = null
                (it.second ?: emptyList()) to fastDiffResult
            }

    fun showSpends(show: Boolean) {
        showSpends.value = show
        userSettingsRepo.showSpends = show
    }

    fun showProfits(show: Boolean) {
        showProfits.value = show
        userSettingsRepo.showProfits = show
    }

    fun showDateSums(show: Boolean) {
        showDateSums.value = show
        userSettingsRepo.showDateSums = show
    }

    fun showMonthSums(show: Boolean) {
        showMonthSums.value = show
        userSettingsRepo.showMonthSums = show
    }

    fun showIds(show: Boolean) {
        showIds.value = show
        userSettingsRepo.showIds = show
    }

    fun showChangeKinds(show: Boolean) {
        showChangeKinds.value = show
        userSettingsRepo.showChangeKinds = show
    }

    fun showBalance(show: Boolean) {
        showBalance.value = show
        userSettingsRepo.showBalance = show
    }

    fun showTimes(show: Boolean) {
        showTimes.value = show
        userSettingsRepo.showTimes = show
    }

    fun addStubSpends() {
        val stubSpendKinds = listOf("трамвай", "столовая", "шоколадка", "автобус")
        val random = Random()
        spendsRepo.addSpends((1..200)
                .map {
                    CreatingSpend(
                            kind = stubSpendKinds[random.nextInt(stubSpendKinds.size)],
                            value = random.nextInt(1000) + 1,
                            date = Date() - (random.nextInt(2100)).days
                    )
                })
    }

    fun addStubProfits() {
        val stubSpendKinds = listOf("стипендия", "зарплата", "аванс", "доход")
        val random = Random()
        profitsRepo.addProfits((1..200)
                .map {
                    CreatingProfit(
                            kind = stubSpendKinds[random.nextInt(stubSpendKinds.size)],
                            value = random.nextInt(10000) + 1,
                            date = Date() - (random.nextInt(2100)).days
                    )
                }
        )
    }

    fun clearAll() {
        spendsRepo.removeAllSpends()
        profitsRepo.removeAllProfits()
    }

    val balance30Days = showBalance
            .switchMap {
                if (it) {
                    spendsRepo.get30DaysBalance()
                } else {
                    MutableLiveData<Long>().also { it.value = null }
                }
            }

    fun deleteSpend(id: Long) {
        spendsRepo.removeSpend(id)
    }

    fun deleteProfit(id: Long) {
        profitsRepo.removeProfit(id)
    }

    fun addProfit(creatingProfit: CreatingProfit) {
        profitsRepo.addProfit(creatingProfit)
    }

    fun editSpend(spend: Spend) {
        spendsRepo.editSpend(spend)
    }

    fun editProfit(profit: Profit) {
        profitsRepo.editProfit(profit)
    }

    fun sendRecords() {
        val vmRef = asReference()
        launch(UI) {
            vmRef().creatingRecordsText.value = Unit
            val spends = async(CommonPool) { spendsRepo.getDumpText() }
            val profits = async(CommonPool) { profitsRepo.getDumpText() }
            vmRef().sendRecords.value = "SPENDS:\n${spends.await()}\n\nPROFITS:\n${profits.await()}"
        }
    }

    val createdSpendsEvents: SingleLiveEvent<Spend> = spendsRepo.locallyCreatedSpends()

    val createdProfitsEvents: SingleLiveEvent<Profit> = profitsRepo.locallyCreatedProfits()

    val creatingRecordsText = SingleLiveEvent<Unit>()
}