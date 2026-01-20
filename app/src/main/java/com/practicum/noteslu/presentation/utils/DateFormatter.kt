package com.practicum.noteslu.presentation.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


object DateFormatter {

    private val millisInHour = TimeUnit.HOURS.toMillis(1)
    private val millisInDay = TimeUnit.DAYS.toMillis(1)
    private val formatter = SimpleDateFormat.getDateInstance(DateFormat.SHORT)

    fun formatCurrentDate(): String {
        return formatter.format(System.currentTimeMillis())
    }

    fun formatDateToString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < millisInHour -> "Прямо сейчас"
            diff < millisInDay -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours час назад"
            }

            else -> {
                formatter.format(timestamp)
            }
        }
    }
}