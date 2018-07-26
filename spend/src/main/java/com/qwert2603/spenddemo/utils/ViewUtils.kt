package com.qwert2603.spenddemo.utils

import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.qwert2603.andrlib.util.color
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.spenddemo.R
import java.text.SimpleDateFormat
import java.util.*

fun TextView.setStrike(strike: Boolean) {
    paintFlags = if (strike) {
        paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

fun EditText.selectEnd() {
    setSelection(text.length)
}

fun View.getGlobalVisibleRectRightNow() = Rect().also { getGlobalVisibleRect(it) }

object DateTimeTextViews {

    private val TIME_FORMAT = SimpleDateFormat(Const.TIME_FORMAT_PATTERN, Locale.getDefault())

    fun render(
            dateTextView: TextView,
            timeTextView: TextView,
            date: Date?,
            time: Date?,
            showTimeAtAll: Boolean = true,
            timePanel: View = timeTextView
    ) {
        val resources = dateTextView.resources
        if (date == null) {
            dateTextView.setText(R.string.now_text)
            dateTextView.setTextColor(resources.color(R.color.date_default))
            timePanel.setVisible(false)
        } else {
            dateTextView.text = date.toFormattedString(resources)
            dateTextView.setTextColor(resources.color(android.R.color.black))
            timePanel.setVisible(showTimeAtAll)
            if (time == null) {
                timeTextView.text = resources.getString(R.string.no_time_text)
                timeTextView.setTextColor(resources.color(R.color.date_default))
            } else {
                timeTextView.text = TIME_FORMAT.format(time)
                timeTextView.setTextColor(resources.color(android.R.color.black))
            }
        }
    }
}