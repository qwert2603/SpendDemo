package com.qwert2603.spend.records_list

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.getWho
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.qwert2603.andrlib.base.mvi.BaseFragment
import com.qwert2603.andrlib.base.mvi.ViewAction
import com.qwert2603.andrlib.util.*
import com.qwert2603.spend.R
import com.qwert2603.spend.change_records.ChangeRecordsDialogFragment
import com.qwert2603.spend.dialogs.*
import com.qwert2603.spend.env.E
import com.qwert2603.spend.model.entity.*
import com.qwert2603.spend.navigation.BackPressListener
import com.qwert2603.spend.navigation.DialogTarget
import com.qwert2603.spend.navigation.KeyboardManager
import com.qwert2603.spend.records_list.vh.DaySumViewHolder
import com.qwert2603.spend.save_record.SaveRecordKey
import com.qwert2603.spend.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_records_list.*
import org.koin.android.ext.android.get
import java.util.concurrent.TimeUnit

class RecordsListFragment : BaseFragment<RecordsListViewState, RecordsListView, RecordsListPresenter>(), RecordsListView, BackPressListener {

    companion object {
        private const val REQUEST_CHOOSE_LONG_SUM_PERIOD = 5
        private const val REQUEST_CHOOSE_SHORT_SUM_PERIOD = 6
        private const val REQUEST_START_DATE = 7
        private const val REQUEST_END_DATE = 8

        private var layoutAnimationShown = false
    }

    private val args by navArgs<RecordsListFragmentArgs>()

    override fun createPresenter() = get<RecordsListPresenter>()

    private var initialScrollDone by BundleBoolean("initialScrollDone", { arguments!! }, false)

    private val adapter: RecordsListAdapter get() = records_RecyclerView.adapter as RecordsListAdapter
    private val itemAnimator: RecordsListAnimator get() = records_RecyclerView.itemAnimator as RecordsListAnimator
    private val layoutManager: LinearLayoutManager get() = records_RecyclerView.layoutManager as LinearLayoutManager

    private val menuHolder = MenuHolder()
    private val selectModeMenuHolder = MenuHolder()

    private val longSumPeriodSelected = PublishSubject.create<Days>()
    private val shortSumPeriodSelected = PublishSubject.create<Minutes>()

    private val cancelSelection = PublishSubject.create<Any>()

    private lateinit var searchEditText: UserInputEditText

    private val startDateSelected = PublishSubject.create<Wrapper<SDate>>()
    private val endDateSelected = PublishSubject.create<Wrapper<SDate>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments = arguments ?: Bundle()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_records_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectPanel_LinearLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        requireActivity().menuInflater.inflate(R.menu.records_select, select_ActionMenuView.menu)
        selectModeMenuHolder.menu = select_ActionMenuView.menu

        search_EditText.doOnTextChanged(withInitial = true) {
            search_EditText.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    if (it.isNotEmpty()) R.drawable.ic_close_black_20dp else 0,
                    0
            )
        }
        search_EditText.onRightDrawableClicked { search_EditText.setText("") }
        searchEditText = UserInputEditText(search_EditText)
        search_EditText.doOnTextChanged {
            when (it) {
                "log error 1918" -> LogUtils.e("log error 19", RuntimeException("log error 1918"))
                "make error 1918" -> throw RuntimeException("make error 1918")
            }
        }

        startDate_EditText.onRightDrawableClicked { startDateSelected.onNext(Wrapper(null)) }
        endDate_EditText.onRightDrawableClicked { endDateSelected.onNext(Wrapper(null)) }

        records_RecyclerView.adapter = RecordsListAdapter(isDaySumsClickable = false)
        records_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_RECORD, 30)
        records_RecyclerView.recycledViewPool.setMaxRecycledViews(RecordsListAdapter.VIEW_TYPE_DATE_SUM, 20)
        records_RecyclerView.addItemDecoration(ConditionDividerDecoration(requireContext()) { rv, vh, _ ->
            vh.adapterPosition > 0 && rv.findViewHolderForAdapterPosition(vh.adapterPosition - 1) is DaySumViewHolder
        })
        records_RecyclerView.itemAnimator = RecordsListAnimator(createSpendViewImpl)

        createSpendViewImpl.dialogShower = object : DialogAwareView.DialogShower {
            override val fragmentWho: String = this@RecordsListFragment.getWho()
        }

        val scrollEvents = records_RecyclerView
                .scrollEvents()
                .skip(1)
                .skipWhile { this::currentViewState.getLateInitOrNull()?.records.isNullOrEmpty() }
                .share()
        Observable
                .combineLatest(
                        scrollEvents
                                .switchMap {
                                    Observable.interval(0, 1, TimeUnit.SECONDS)
                                            .take(2)
                                            .map { it == 0L }
                                }
                                .distinctUntilChanged()
                                .observeOn(AndroidSchedulers.mainThread()),
                        scrollEvents
                                .map {
                                    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                                    val lastIsTotal = lastVisiblePosition != RecyclerView.NO_POSITION
                                            && currentViewState.records?.get(lastVisiblePosition) is Totals
                                    return@map !lastIsTotal
                                },
                        Boolean::and.toRxBiFunction()
                )
                .subscribe { showFromScroll ->
                    val showFloatingDate = currentViewState.showInfo.showFloatingDate()
                    val records = currentViewState.records
                    floatingDate_TextView.setVisible(showFromScroll && showFloatingDate && records?.any { it is DaySum } == true)
                }
                .disposeOnDestroyView()
        scrollEvents
                .subscribe {
                    if (!currentViewState.showInfo.showFloatingDate()) return@subscribe
                    var lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                    if (lastVisiblePosition == RecyclerView.NO_POSITION) return@subscribe
                    val floatingCenter = floatingDate_TextView.getGlobalVisibleRectRightNow().centerY()
                    val viewHolder = records_RecyclerView.findViewHolderForAdapterPosition(lastVisiblePosition)
                    val records = currentViewState.records
                    if (viewHolder == null || records == null) {
                        floatingDate_TextView.text = ""
                        return@subscribe
                    }
                    val vhBottom = viewHolder.itemView.getGlobalVisibleRectRightNow().bottom
                    if (lastVisiblePosition > 0 && vhBottom < floatingCenter) --lastVisiblePosition
                    floatingDate_TextView.text = (records[lastVisiblePosition].date()).toFormattedString(resources, stringMonth = true)
                }
                .disposeOnDestroyView()

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        selectModeMenuHolder.menu = null
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        createSpendViewImpl.onDialogResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CHOOSE_LONG_SUM_PERIOD -> data.getSerializableExtra(ChooseLongSumPeriodDialog.DAYS_KEY)
                        .let { it as? Days }
                        ?.also { longSumPeriodSelected.onNext(it) }
                REQUEST_CHOOSE_SHORT_SUM_PERIOD -> data.getSerializableExtra(ChooseShortSumPeriodDialog.MINUTES_KEY)
                        .let { it as? Minutes }
                        ?.also { shortSumPeriodSelected.onNext(it) }
                REQUEST_START_DATE -> startDateSelected.onNext(data.getIntExtra(DatePickerDialogFragment.DATE_KEY, 0).toSDate().wrap())
                REQUEST_END_DATE -> endDateSelected.onNext(data.getIntExtra(DatePickerDialogFragment.DATE_KEY, 0).toSDate().wrap())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.records_list, menu)

        menu.findItem(R.id.add_stub_records).isVisible = E.env.buildForTesting()
        menu.findItem(R.id.clear_all).isVisible = E.env.buildForTesting()
        menu.findItem(R.id.debug_dialog).isVisible = E.env.buildForTesting()

        menuHolder.menu = menu

        menu.findItem(R.id.debug_dialog).setOnMenuItemClickListener {
            findNavController().navigateFixed(RecordsListFragmentDirections.actionRecordsListFragmentToDebugDialogFragment())
            true
        }

        renderAll()
    }

    override fun onDestroyOptionsMenu() {
        menuHolder.menu = null
        super.onDestroyOptionsMenu()
    }

    override fun recordClicks(): Observable<Record> = adapter.itemClicks.mapNotNull { it as? Record }
    override fun recordLongClicks(): Observable<Record> = adapter.itemLongClicks.mapNotNull { it as? Record }
    override fun createProfitClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.new_profit)
    override fun chooseLongSumPeriodClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.long_sum)
    override fun chooseShortSumPeriodClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.short_sum)

    override fun showSpendsChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_spends)
    override fun showProfitsChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_profits)
    override fun showSumsChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_sums)
    override fun showChangeKindsChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_change_kinds)
    override fun showTimesChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.show_times)

    override fun sortByValueChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.sort_by_value)
    override fun showFiltersChanges(): Observable<Boolean> = menuHolder.menuItemCheckedChanges(R.id.filters)

    override fun longSumPeriodSelected(): Observable<Days> = longSumPeriodSelected
    override fun shortSumPeriodSelected(): Observable<Minutes> = shortSumPeriodSelected

    override fun addStubRecordsClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.add_stub_records)
    override fun clearAllClicks(): Observable<Any> = menuHolder.menuItemClicks(R.id.clear_all)

    override fun cancelSelection(): Observable<Any> = Observable.merge(
            closeSelectPanel_ImageView.clicks().map { },
            cancelSelection
    )

    override fun deleteSelectedClicks(): Observable<Any> = selectModeMenuHolder.menuItemClicks(R.id.delete)
    override fun combineSelectedClicks(): Observable<Any> = selectModeMenuHolder.menuItemClicks(R.id.combine)
    override fun changeSelectedClicks(): Observable<Any> = selectModeMenuHolder.menuItemClicks(R.id.change)

    override fun searchQueryChanges(): Observable<String> = searchEditText.userInputs()

    override fun selectStartDateClicks(): Observable<Any> = startDate_EditText.clicks().map { }
    override fun selectEndDateClicks(): Observable<Any> = endDate_EditText.clicks().map { }

    override fun startDateSelected(): Observable<Wrapper<SDate>> = startDateSelected
    override fun endDateSelected(): Observable<Wrapper<SDate>> = endDateSelected

    override fun render(vs: RecordsListViewState) {
        LogUtils.withErrorLoggingOnly { super.render(vs) }

        renderIfChanged({ showInfo }) { showInfo ->
            FirebaseAnalytics.getInstance(requireContext()).logEvent("RecordsListFragment_render", Bundle().also {
                it.putString("showInfo", showInfo.toString())
            })
        }
        renderIfChanged({ showInfo.showSums }) { showSums ->
            FirebaseAnalytics.getInstance(requireContext()).logEvent("showInfo_showSums", Bundle().also {
                it.putString("showSums", showSums.toString())
            })
        }

        renderIfChanged({ showInfo.showChangeKinds }) { adapter.showChangeKinds = it }
        renderIfChanged({ showInfo.showTimes }) { adapter.showTimesInRecords = it }
        renderIfChanged({ !showInfo.showSums || sortByValue || showFilters }) { adapter.showDatesInRecords = it }
        renderIfChanged({ selectedRecordsUuids }) { adapter.selectedRecordsUuids = it }
        renderIfChanged({ selectMode }) { adapter.selectMode = it }
        renderIfChanged({ sortByValue }) { adapter.sortByValue = it }

        renderIfChanged({ records }) { records ->
            if (records == null) return@renderIfChanged
            adapter.recordsChanges = vs.recordsChanges
            val isListEverSet = adapter.isListEverSet
            val isListChanged = adapter.list !== records
            adapter.list = records
            if (args.key is RecordsListKey.Now && !layoutAnimationShown) {
                layoutAnimationShown = true
                records_RecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
            }
            if (isListEverSet) {
                adapter.redrawViewHolders()
                if (isListChanged) vs.diff.dispatchToAdapter(adapter)
            } else {
                adapter.notifyDataSetChanged()
            }
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                records_RecyclerView.scrollToPosition(0)
            }
            val pendingCreatedRecordUuid = itemAnimator.pendingCreatedRecordUuid
            if (pendingCreatedRecordUuid != null) {
                val position = records.indexOfFirst { it is Record && it.uuid == pendingCreatedRecordUuid }
                if (position >= 0) {
                    records_RecyclerView.scrollToPosition(position)
                }
            }
            val pendingEditedRecordUuid = itemAnimator.pendingEditedRecordUuid
            if (pendingEditedRecordUuid != null) {
                LogUtils.d { "RecordsListAnimator pendingEditedRecordUuid $pendingEditedRecordUuid" }
                val position = records.indexOfFirst { it is Record && it.uuid == pendingEditedRecordUuid }
                LogUtils.d { "RecordsListAnimator position $position" }
                if (position >= 0) {
                    records_RecyclerView.scrollToPosition(position)
                }
            }
            val pendingCombinedRecordUuid = itemAnimator.pendingCombinedRecordUuid
            if (pendingCombinedRecordUuid != null) {
                LogUtils.d { "RecordsListAnimator pendingCombinedRecordUuid $pendingCombinedRecordUuid" }
                val position = records.indexOfFirst { it is Record && it.uuid == pendingCombinedRecordUuid }
                LogUtils.d { "RecordsListAnimator position $position" }
                if (position >= 0) {
                    records_RecyclerView.scrollToPosition(position)
                }
            }
            if (!initialScrollDone) {
                initialScrollDone = true
                val key = args.key
                LogUtils.d("RecordsListFragment key=$key")
                @Suppress("IMPLICIT_CAST_TO_ANY")
                when (key) {
                    is RecordsListKey.Now -> Unit
                    is RecordsListKey.Date -> {
                        records
                                .indexOfFirst { it.date() < key.date }
                                .let {
                                    if (it >= 0) {
                                        it - 5
                                    } else {
                                        records.lastIndex
                                    }
                                }
                                .coerceAtLeast(0)
                                .also {
                                    LogUtils.d("RecordsListFragment records_RecyclerView.scrollToPosition($it)")
                                    records_RecyclerView.scrollToPosition(it)
                                }
                    }
                }.also {}
            }
        }

        renderIfChanged({ showFilters }) {
            (requireActivity() as KeyboardManager).hideKeyboard()
        }

        renderIfChanged({ showInfo.showSpends }) { showSpends ->
            if (!showSpends && !vs.showFilters) (requireActivity() as KeyboardManager).hideKeyboard()
        }

        renderIfChanged({ showInfo.showSpends && !showFilters }) {
            createSpendViewImpl.setVisible(it)
        }

        renderIfChanged({ showFilters }) { filters_LinearLayout.setVisible(it) }

        menuHolder.menu?.also { menu ->
            renderIfChanged({ showInfo.showProfits && !showFilters }) {
                menu.findItem(R.id.new_profit).isEnabled = it
            }

            renderIfChanged({ showInfo }) {
                menu.findItem(R.id.show_spends).isChecked = it.showSpends
                menu.findItem(R.id.show_profits).isChecked = it.showProfits
                menu.findItem(R.id.show_sums).isChecked = it.showSums
                menu.findItem(R.id.show_change_kinds).isChecked = it.showChangeKinds
                menu.findItem(R.id.show_times).isChecked = it.showTimes

                menu.findItem(R.id.show_spends).isEnabled = it.showSpendsEnable()
                menu.findItem(R.id.show_profits).isEnabled = it.showProfitsEnable()
            }

            renderIfChanged({ sortByValue }) { menu.findItem(R.id.sort_by_value).isChecked = it }
            renderIfChanged({ showFilters }) { menu.findItem(R.id.filters).isChecked = it }

            renderIfChanged({ longSumPeriod }) {
                menu.findItem(R.id.long_sum).title = resources.getString(
                        R.string.menu_item_long_sum_format,
                        ChooseLongSumPeriodDialog.variantToString(it, resources)
                )
            }
            renderIfChanged({ shortSumPeriod }) {
                menu.findItem(R.id.short_sum).title = resources.getString(
                        R.string.menu_item_short_sum_format,
                        ChooseShortSumPeriodDialog.variantToString(it, resources)
                )
            }
        }

        renderIfChangedThree({ Triple(sumsInfo, longSumPeriod, shortSumPeriod) }) { (sumsInfo, longSumPeriodDays, shortSumPeriodMinutes) ->
            val longSumText = sumsInfo.longSum
                    ?.let {
                        resources.getString(
                                R.string.text_period_sum_format,
                                resources.formatTimeLetters(longSumPeriodDays),
                                it.toPointedString()
                        )
                    }
            val shortSumText = sumsInfo.shortSum
                    ?.let {
                        resources.getString(
                                R.string.text_period_sum_format,
                                resources.formatTimeLetters(shortSumPeriodMinutes),
                                it.toPointedString()
                        )
                    }
            val changesCountText = sumsInfo.changesCount
                    ?.let {
                        when (it) {
                            0 -> getString(R.string.no_changes_text)
                            else -> getString(R.string.changes_count_format, it)
                        }
                    }
            toolbar.subtitle = listOfNotNull(longSumText, shortSumText, changesCountText)
                    .reduceEmptyToNull { acc, s -> "$acc    $s" }
        }

        renderIfChanged({ syncState }) {
            toolbar.title = getString(R.string.fragment_title_records) + it.indicator
        }

        renderIfChangedWithFirstRendering({ selectMode }) { visible, firstRendering -> setDeletePanelVisible(visible, firstRendering) }
        renderIfChanged({ selectedRecordsUuids.size }) { selectedCount_VectorIntegerView.setInteger(it.toLong(), true) }
        renderIfChanged({ selectedSum }) {
            selectedSum_VectorIntegerView.digitColor = resources.color(
                    if (it >= 0) {
                        R.color.balance_positive
                    } else {
                        R.color.balance_negative
                    }
            )
            selectedSum_VectorIntegerView.setInteger(it, true)
        }

        selectModeMenuHolder.menu?.also { menu ->
            renderIfChanged({ canDeleteSelected }) { menu.findItem(R.id.delete).isEnabled = it }
            renderIfChanged({ canChangeSelected }) { menu.findItem(R.id.change).isEnabled = it }
            renderIfChanged({ canCombineSelected }) { menu.findItem(R.id.combine).isEnabled = it }
        }

        renderIfChanged({ searchQuery }) { searchEditText.setText(it) }

        fun renderDate(textView: TextView, date: SDate?) {
            textView.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    if (date != null) R.drawable.ic_close_black_20dp else 0,
                    0
            )
            textView.setTextColor(resources.color(if (date != null) android.R.color.black else R.color.dont_change))
            textView.setTypeface(null, if (date != null) Typeface.NORMAL else Typeface.ITALIC)
            textView.text = date?.toFormattedString(resources)
                    ?: getString(R.string.text_not_selected)
        }

        renderIfChanged({ startDate }) { renderDate(startDate_EditText, it) }
        renderIfChanged({ endDate }) { renderDate(endDate_EditText, it) }
    }

    override fun executeAction(va: ViewAction) {
        if (va !is RecordsListViewAction) null!!
        when (va) {
            is RecordsListViewAction.AskForRecordActions -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToRecordActionsDialogFragment(va.recordUuid))
            is RecordsListViewAction.AskToCreateRecord -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToSaveRecordDialogFragment(SaveRecordKey.NewRecord(va.recordTypeId)))
            is RecordsListViewAction.AskToChooseLongSumPeriod -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToChooseLongSumPeriodDialog(va.days, DialogTarget(getWho(), REQUEST_CHOOSE_LONG_SUM_PERIOD)))
            is RecordsListViewAction.AskToChooseShortSumPeriod -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToChooseShortSumPeriodDialog(va.minutes, DialogTarget(getWho(), REQUEST_CHOOSE_SHORT_SUM_PERIOD)))
            is RecordsListViewAction.OnRecordCreatedLocally -> itemAnimator.pendingCreatedRecordUuid = va.uuid
            is RecordsListViewAction.OnRecordEditedLocally -> itemAnimator.pendingEditedRecordUuid = va.uuid
            is RecordsListViewAction.OnRecordCombinedLocally -> itemAnimator.pendingCombinedRecordUuid = va.uuid
            RecordsListViewAction.RerenderAll -> renderAll()
            is RecordsListViewAction.AskToCombineRecords -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToCombineRecordsDialogFragment(CombineRecordsDialogFragment.Key(va.recordUuids, va.categoryUuid, va.kind)))
            is RecordsListViewAction.AskToDeleteRecords -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToDeleteRecordsListDialogFragment(DeleteRecordsListDialogFragment.Key(va.recordUuids)))
            is RecordsListViewAction.AskToChangeRecords -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToChangeRecordsDialogFragment(ChangeRecordsDialogFragment.Key(va.recordUuids)))
            RecordsListViewAction.ScrollToTop -> records_RecyclerView.scrollToPosition(0)
            is RecordsListViewAction.AskToSelectStartDate -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToDatePickerDialogFragment(
                                    date = va.startDate,
                                    withNow = false,
                                    minDate = null,
                                    maxDate = va.maxDate,
                                    target = DialogTarget(getWho(), REQUEST_START_DATE)
                            ))
            is RecordsListViewAction.AskToSelectEndDate -> findNavController()
                    .navigateFixed(RecordsListFragmentDirections
                            .actionRecordsListFragmentToDatePickerDialogFragment(
                                    date = va.endDate,
                                    withNow = false,
                                    minDate = va.minDate,
                                    maxDate = null,
                                    target = DialogTarget(getWho(), REQUEST_END_DATE)
                            ))
        }.also { }
    }

    override fun onBackPressed(): Boolean {
        if (currentViewState.selectMode) {
            cancelSelection.onNext(Any())
            return true
        }
        return false
    }

    private fun setDeletePanelVisible(visible: Boolean, firstRendering: Boolean) {
        val isRoot = requireFragmentManager().backStackEntryCount == 0
        val defaultIconState = if (isRoot) R.attr.state_drawer else R.attr.state_back_arrow
        val newState = intArrayOf(
                R.attr.state_close.let { if (visible) it else -it },
                defaultIconState.let { if (!visible) it else -it }
        )

        if (firstRendering) {
            toolbar.alpha = if (visible) 0f else 1f
            selectPanel_LinearLayout.alpha = if (visible) 1f else 0f
            selectPanel_LinearLayout.setVisible(visible)
            appBarLayout.setBackgroundColor(resources.color(if (visible) R.color.colorAccent else R.color.colorPrimary))
            closeSelectPanel_ImageView.setImageState(newState, true)
            closeSelectPanel_ImageView.jumpDrawablesToCurrentState()
            return
        }

        val animationDuration = resources.getInteger(R.integer.toolbar_icon_animation_duration).toLong()
        if (visible) {
            toolbar.animate()
                    .setDuration(animationDuration)
                    .alpha(0f)
            selectPanel_LinearLayout.setVisible(true)
            selectPanel_LinearLayout.animate()
                    .setDuration(animationDuration)
                    .alpha(1f)
            appBarLayout.animateBackgroundColor(
                    appBarLayout.getBackgroundColor() ?: resources.color(R.color.colorPrimary),
                    resources.color(R.color.colorAccent),
                    animationDuration
            )
            closeSelectPanel_ImageView.postDelayed({
                closeSelectPanel_ImageView?.setImageState(newState, true)
            }, 100)
        } else {
            closeSelectPanel_ImageView.post {
                closeSelectPanel_ImageView?.setImageState(newState, true)
            }
            closeSelectPanel_ImageView.postDelayed({
                toolbar?.animate()
                        ?.setDuration(animationDuration)
                        ?.alpha(1f)
                selectPanel_LinearLayout?.animate()
                        ?.setDuration(animationDuration)
                        ?.alpha(0f)
                        ?.withEndAction { selectPanel_LinearLayout?.setVisible(false) }
                appBarLayout?.animateBackgroundColor(
                        appBarLayout?.getBackgroundColor() ?: resources.color(R.color.colorAccent),
                        resources.color(R.color.colorPrimary),
                        animationDuration
                )
            }, animationDuration)
        }
    }
}