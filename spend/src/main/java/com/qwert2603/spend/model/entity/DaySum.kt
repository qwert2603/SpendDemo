package com.qwert2603.spend.model.entity

data class DaySum(
        val day: SDate,
        val showSpends: Boolean,
        val showProfits: Boolean,
        val spends: Long,
        val profits: Long
) : RecordsListItem {
    override val id = day.date.toLong()

    val balance = profits - spends
}