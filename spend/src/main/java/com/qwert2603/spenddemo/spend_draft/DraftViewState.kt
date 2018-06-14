package com.qwert2603.spenddemo.spend_draft

import com.qwert2603.spenddemo.model.entity.CreatingSpend

data class DraftViewState(
        val creatingSpend: CreatingSpend,
        val createEnable: Boolean,
        val showTime: Boolean
) {
    val valueString: String = creatingSpend.value.takeIf { it != 0 }?.toString() ?: ""
}