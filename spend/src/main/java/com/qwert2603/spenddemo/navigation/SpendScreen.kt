package com.qwert2603.spenddemo.navigation

import android.support.v4.app.Fragment
import com.qwert2603.spenddemo.about.AboutFragment
import com.qwert2603.spenddemo.records_list.RecordsListFragmentBuilder
import com.qwert2603.spenddemo.records_list.RecordsListKey
import com.qwert2603.spenddemo.sums.SumsFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen
import java.io.Serializable

sealed class SpendScreen(
        private val fragmentCreator: () -> Fragment = { null!! }
) : SupportAppScreen(), Serializable {

    override fun getFragment(): Fragment = fragmentCreator().also { it.setScreen(this) }

    data class RecordsList(val recordsListKey: RecordsListKey) : SpendScreen({ RecordsListFragmentBuilder.newRecordsListFragment(recordsListKey) })
    object Sums : SpendScreen({ SumsFragment() })
    object About : SpendScreen({ AboutFragment() })
}