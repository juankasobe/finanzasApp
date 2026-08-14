package com.saldoclaro.finance.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToString(value: LocalDate): String = value.toString()
    @TypeConverter fun stringToDate(value: String): LocalDate = LocalDate.parse(value)
}
