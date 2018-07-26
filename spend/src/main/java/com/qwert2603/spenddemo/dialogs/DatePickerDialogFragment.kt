package com.qwert2603.spenddemo.dialogs

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import com.qwert2603.spenddemo.BuildConfig
import com.qwert2603.spenddemo.R
import com.qwert2603.spenddemo.utils.day
import com.qwert2603.spenddemo.utils.month
import com.qwert2603.spenddemo.utils.onlyDate
import com.qwert2603.spenddemo.utils.year
import java.util.*

@FragmentWithArgs
class DatePickerDialogFragment : DialogFragment() {

    companion object {
        const val MILLIS_KEY = "${BuildConfig.APPLICATION_ID}.MILLIS_KEY"
    }

    @Arg
    var millis: Long = 0

    @Arg
    var withNow: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
                .also { it.timeInMillis = millis }
                .also { it.time = it.onlyDate() }
        return DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    calendar.year = year
                    calendar.month = month
                    calendar.day = dayOfMonth
                    targetFragment!!.onActivityResult(
                            targetRequestCode,
                            Activity.RESULT_OK,
                            Intent().putExtra(MILLIS_KEY, calendar.timeInMillis)
                    )
                },
                calendar.year,
                calendar.month,
                calendar.day
        ).also {
            if (withNow) {
                it.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.now_text)) { _, _ ->
                    targetFragment!!.onActivityResult(
                            targetRequestCode,
                            Activity.RESULT_OK,
                            Intent()
                    )
                }
            }
        }
    }
}