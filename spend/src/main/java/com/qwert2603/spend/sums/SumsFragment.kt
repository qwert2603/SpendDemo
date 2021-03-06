package com.qwert2603.spend.sums

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qwert2603.andrlib.base.mvi.BaseFragment
import com.qwert2603.andrlib.base.mvi.ViewAction
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.andrlib.util.renderIfChanged
import com.qwert2603.spend.R
import com.qwert2603.spend.env.E
import com.qwert2603.spend.records_list.RecordsListAdapter
import com.qwert2603.spend.records_list.RecordsListKey
import com.qwert2603.spend.records_list.vh.DaySumViewHolder
import com.qwert2603.spend.utils.ConditionDividerDecoration
import com.qwert2603.spend.utils.MenuHolder
import com.qwert2603.spend.utils.navigateFixed
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_sums.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.koin.android.ext.android.get

class SumsFragment : BaseFragment<SumsViewState, SumsView, SumsPresenter>(), SumsView {

    override fun createPresenter() = get<SumsPresenter>()

    private val adapter: RecordsListAdapter get() = sums_RecyclerView.adapter as RecordsListAdapter
    private val layoutManager: LinearLayoutManager get() = sums_RecyclerView.layoutManager as LinearLayoutManager

    private val menuHolder = MenuHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_sums, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sums_RecyclerView.adapter = RecordsListAdapter(isDaySumsClickable = true)
        sums_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_DATE_SUM, 30)
        sums_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_MONTH_SUM, 20)
        sums_RecyclerView.addItemDecoration(ConditionDividerDecoration(requireContext()) { rv, vh, _ ->
            vh.adapterPosition > 0 && rv.findViewHolderForAdapterPosition(vh.adapterPosition - 1) is DaySumViewHolder
        })

        adapter.itemClicks
                .subscribe {
                    val recordsListKey = RecordsListKey.Date(it.date())
                    findNavController().navigateFixed(SumsFragmentDirections.actionSumsFragmentToRecordsListFragment(recordsListKey))
                }
                .disposeOnDestroyView()

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.sums, menu)

        menu.findItem(R.id.clear_all).isVisible = E.env.buildForTesting()

        menuHolder.menu = menu
        renderAll()
    }

    override fun onDestroyOptionsMenu() {
        menuHolder.menu = null
        super.onDestroyOptionsMenu()
    }

    override fun showDaySums(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_sums_by_day)
    override fun showMonthSums(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_sums_by_month)
    override fun showYearSums(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_sums_by_year)
    override fun showBalances(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_balances)
    override fun clearAllClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.clear_all)

    override fun render(vs: SumsViewState) {
        LogUtils.withErrorLoggingOnly { super.render(vs) }

        renderIfChanged({ sumsShowInfo.showBalances }) { adapter.showBalancesInSums = it }

        renderIfChanged({ records }) { records ->
            val isListEverSet = adapter.isListEverSet
            val isListChanged = adapter.list !== records
            adapter.list = records
            if (isListEverSet) {
                adapter.redrawViewHolders()
                if (isListChanged) vs.diff.dispatchToAdapter(adapter)
            } else {
                adapter.notifyDataSetChanged()
            }
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                sums_RecyclerView.scrollToPosition(0)
            }
        }

        menuHolder.menu?.also { menu ->
            renderIfChanged({ sumsShowInfo }) {
                menu.findItem(R.id.show_sums_by_day).isChecked = it.showDaySums
                menu.findItem(R.id.show_sums_by_month).isChecked = it.showMonthSums
                menu.findItem(R.id.show_sums_by_year).isChecked = it.showYearSums
                menu.findItem(R.id.show_balances).isChecked = it.showBalances

                menu.findItem(R.id.show_sums_by_day).isEnabled = it.showDaySumsEnable()
                menu.findItem(R.id.show_sums_by_month).isEnabled = it.showMonthSumsEnable()
                menu.findItem(R.id.show_sums_by_year).isEnabled = it.showYearSumsEnable()
            }
        }

        renderIfChanged({ syncState }) {
            toolbar.title = getString(R.string.fragment_title_sums) + it.indicator
        }
    }

    override fun executeAction(va: ViewAction) {
        if (va !is SumsViewAction) null!!
        return when (va) {
            SumsViewAction.RerenderAll -> renderAll()
        }
    }
}