package com.qwert2603.spenddemo.records_list_mvvm

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.view.animation.AnimationUtils
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView
import com.qwert2603.andrlib.util.addTo
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.BuildConfig
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.dialogs.*
import com.qwert2603.spenddemo.model.entity.CreatingProfit
import com.qwert2603.spenddemo.model.entity.Profit
import com.qwert2603.spenddemo.model.entity.Spend
import com.qwert2603.spenddemo.navigation.KeyboardManager
import com.qwert2603.spenddemo.records_list.RecordsListAnimator
import com.qwert2603.spenddemo.records_list.entity.DateSumUI
import com.qwert2603.spenddemo.records_list.entity.MonthSumUI
import com.qwert2603.spenddemo.records_list.entity.RecordsListItem
import com.qwert2603.spenddemo.records_list.entity.TotalsUI
import com.qwert2603.spenddemo.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_records_list.*
import kotlinx.android.synthetic.main.toolbar_default.*
import kotlinx.android.synthetic.main.view_spend_draft.view.*
import java.util.*
import java.util.concurrent.TimeUnit

class RecordsListMvvmFragment : Fragment() {

    companion object {
        private const val REQUEST_DELETE_SPEND = 1
        private const val REQUEST_EDIT_SPEND = 2
        private const val REQUEST_ADD_PROFIT = 3
        private const val REQUEST_EDIT_PROFIT = 4
        private const val REQUEST_DELETE_PROFIT = 5
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this, ViewModelFactory()).get(RecordsListViewModel::class.java)
    }

    private val viewDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_records_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = RecordsListAdapter()
        records_RecyclerView.adapter = adapter
        records_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_SPEND, 20)
        records_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_PROFIT, 20)
        records_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_DATE_SUM, 20)
        records_RecyclerView.addItemDecoration(ConditionDividerDecoration(requireContext(), { rv, vh ->
            vh.adapterPosition > 0 && rv.findViewHolderForAdapterPosition(vh.adapterPosition - 1) is DateSumViewHolder
        }))
        records_RecyclerView.itemAnimator = RecordsListAnimator()
                .also {
                    it.spendOrigin = object : RecordsListAnimator.SpendOrigin {
                        override fun getDateGlobalVisibleRect(): Rect = draftViewImpl.date_EditText.getGlobalVisibleRectRightNow()
                        override fun getKindGlobalVisibleRect(): Rect = draftViewImpl.kind_EditText.getGlobalVisibleRectRightNow()
                        override fun getValueGlobalVisibleRect(): Rect = draftViewImpl.value_EditText.getGlobalVisibleRectRightNow()
                    }
                }

        var showFloatingDate = false
        var records = emptyList<RecordsListItem>()

        var layoutAnimationShown = false
        viewModel.recordsLiveData.observe(this, Observer {
            if (it == null) return@Observer
            adapter.list = it
            records = it
            if (savedInstanceState == null && !layoutAnimationShown) {
                layoutAnimationShown = true
                val layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
                records_RecyclerView.layoutAnimation = layoutAnimation
            }
        })
        viewModel.recordsCounts.observe(this, Observer { toolbar.subtitle = it })
        viewModel.showInfo.observe(this, Observer {
            if (it == null) return@Observer
            draftPanel_LinearLayout.setVisible(it.newSpendVisible())
            if (!it.newSpendVisible()) (context as KeyboardManager).hideKeyboard()
            showFloatingDate = it.showFloatingDate()
        })

        viewModel.balance30Days.observe(this, Observer { toolbar.title = getString(R.string.app_name) + it?.toString() })

        draftViewImpl.dialogShower = object : DialogAwareView.DialogShower {
            override fun showDialog(dialogFragment: DialogFragment, requestCode: Int) {
                dialogFragment
                        .also { it.setTargetFragment(this@RecordsListMvvmFragment, requestCode) }
                        .show(fragmentManager, dialogFragment.toString())
            }
        }

        toolbar.title = listOfNotNull(
                getString(R.string.app_name),
                BuildConfig.FLAVOR,
                BuildConfig.BUILD_TYPE.takeIf { it != "debug" }
        ).reduce { acc, s -> "$acc $s" }

        RxRecyclerView.scrollEvents(records_RecyclerView)
                .switchMap {
                    Observable.interval(0, 1, TimeUnit.SECONDS)
                            .take(2)
                            .map { it == 0L }
                }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    floatingDate_TextView.animate()
                            .setStartDelay(0L)
                            .setDuration(250L)
                            .alpha(if (it && showFloatingDate && records.any { it is DateSumUI }) 1f else 0f)
                }
                .addTo(viewDisposable)
        RxRecyclerView.scrollEvents(records_RecyclerView)
                .subscribe {
                    if (!showFloatingDate) return@subscribe
                    var i = (records_RecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val floatingTop = floatingDate_TextView.getGlobalVisibleRectRightNow().bottom
                    val vhTop = records_RecyclerView.findViewHolderForAdapterPosition(i).itemView.getGlobalVisibleRectRightNow().bottom
                    if (i > 0 && vhTop < floatingTop) --i
                    if (i in 1..records.lastIndex && records[i] is TotalsUI) --i
                    if (i in 1..records.lastIndex && records[i] is MonthSumUI) --i
                    i = records.indexOfFirst(startIndex = i) { it is DateSumUI }
                    if (i >= 0) {
                        floatingDate_TextView.text = (records[i] as DateSumUI).date.toFormattedString(resources)
                    } else {
                        floatingDate_TextView.text = ""
                    }
                }
                .addTo(viewDisposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewDisposable.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        draftViewImpl.onDialogResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_DELETE_SPEND -> viewModel.deleteSpend(data.getLongExtra(DeleteSpendDialogFragment.ID_KEY, 0))
                REQUEST_EDIT_SPEND -> viewModel.editSpend(Spend(
                        data.getLongExtra(EditSpendDialogFragment.ID_KEY, 0),
                        data.getStringExtra(EditSpendDialogFragment.KIND_KEY),
                        data.getIntExtra(EditSpendDialogFragment.VALUE_KEY, 0),
                        Date(data.getLongExtra(EditSpendDialogFragment.DATE_KEY, 0L))
                ))
                REQUEST_ADD_PROFIT -> viewModel.addProfit(CreatingProfit(
                        data.getStringExtra(AddProfitDialogFragment.KIND_KEY),
                        data.getIntExtra(AddProfitDialogFragment.VALUE_KEY, 0),
                        Date(data.getLongExtra(AddProfitDialogFragment.DATE_KEY, 0))
                ))
                REQUEST_EDIT_PROFIT -> viewModel.editProfit(Profit(
                        data.getLongExtra(AddProfitDialogFragment.ID_KEY, 0),
                        data.getStringExtra(AddProfitDialogFragment.KIND_KEY),
                        data.getIntExtra(AddProfitDialogFragment.VALUE_KEY, 0),
                        Date(data.getLongExtra(AddProfitDialogFragment.DATE_KEY, 0L))
                ))
                REQUEST_DELETE_PROFIT -> viewModel.deleteProfit(data.getLongExtra(DeleteProfitDialogFragment.ID_KEY, 0))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.records_list, menu)

        val showSpendsMenuItem = menu.findItem(R.id.show_spends)
        val showProfitsMenuItem = menu.findItem(R.id.show_profits)
        val showDateSumsMenuItem = menu.findItem(R.id.show_date_sums)
        val showMonthSumsMenuItem = menu.findItem(R.id.show_month_sums)
        viewModel.showSpends.observe(this, Observer { showSpendsMenuItem.isChecked = it == true })
        viewModel.showProfits.observe(this, Observer { showProfitsMenuItem.isChecked = it == true })
        viewModel.showDateSums.observe(this, Observer { showDateSumsMenuItem.isChecked = it == true })
        viewModel.showMonthSums.observe(this, Observer { showMonthSumsMenuItem.isChecked = it == true })
        showSpendsMenuItem.setOnMenuItemClickListener { viewModel.showSpends(!showSpendsMenuItem.isChecked);true }
        showProfitsMenuItem.setOnMenuItemClickListener { viewModel.showProfits(!showProfitsMenuItem.isChecked);true }
        showDateSumsMenuItem.setOnMenuItemClickListener { viewModel.showDateSums(!showDateSumsMenuItem.isChecked);true }
        showMonthSumsMenuItem.setOnMenuItemClickListener { viewModel.showMonthSums(!showMonthSumsMenuItem.isChecked);true }

        viewModel.showInfo.observe(this, Observer {
            if (it == null) return@Observer
            menu.findItem(R.id.new_profit).isEnabled = it.newProfitEnable()
            showSpendsMenuItem.isEnabled = it.showSpendsEnable()
            showProfitsMenuItem.isEnabled = it.showProfitsEnable()
            showDateSumsMenuItem.isEnabled = it.showDateSumsEnable()
            showMonthSumsMenuItem.isEnabled = it.showMonthSumsEnable()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.new_profit -> AddProfitDialogFragmentBuilder.newAddProfitDialogFragment(true)
                    .also { it.setTargetFragment(this, REQUEST_ADD_PROFIT) }
                    .show(fragmentManager, "add_profit")
                    .also { (context as KeyboardManager).hideKeyboard() }
            R.id.add_stub_spends -> viewModel.addStubSpends()
            R.id.add_stub_profits -> viewModel.addStubProfits()
            R.id.about -> AppInfoDialogFragment()
                    .show(fragmentManager, "app_info")
                    .also { (context as KeyboardManager).hideKeyboard() }
            R.id.clear_all -> viewModel.clearAll()
            R.id.send_records -> TODO()
            R.id.show_change_kinds -> TODO()
            R.id.show_ids -> TODO()
        }
        return super.onOptionsItemSelected(item)
    }
}