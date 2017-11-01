package com.qwert2603.spenddemo.model.local_db.converters

import android.arch.persistence.room.TypeConverter
import java.util.*

class DateConverter {
    @TypeConverter
    fun toDate(millis: Long) = Date(millis)

    @TypeConverter
    fun toMillis(date: Date) = date.time
}