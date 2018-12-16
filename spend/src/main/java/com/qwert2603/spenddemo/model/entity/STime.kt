package com.qwert2603.spenddemo.model.entity

import com.qwert2603.spenddemo.utils.Const
import com.qwert2603.spenddemo.utils.hour
import com.qwert2603.spenddemo.utils.millisecond
import com.qwert2603.spenddemo.utils.minute
import java.util.*

/** format is "HHmm" */
data class STime(val time: Int) : Comparable<STime> {
    override fun toString() = String.format("%d:%02d", time / 100, time % 100)

    fun toTimeCalendar() = GregorianCalendar(
            Const.MIN_YEAR,
            Calendar.JANUARY,
            1,
            time / 100,
            time % 100,
            0
    ).also { it.millisecond = 0 }

    override fun compareTo(other: STime): Int = this.time.compareTo(other.time)
}

fun Int.toSTime() = STime(this)

fun Calendar.toSTime() = STime(hour * 100 + minute)