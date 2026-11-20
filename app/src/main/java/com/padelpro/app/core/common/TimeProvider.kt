package com.padelpro.app.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

interface TimeProvider {
    fun nowMillis(): Long
    fun currentHourOfDay(): Int
    fun currentLocalDate(): LocalDate
    fun formatMonthYear(epochMillis: Long): String
    fun formatShortDate(epochMillis: Long): String
}

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun currentHourOfDay(): Int {
        return Instant.ofEpochMilli(nowMillis())
            .atZone(ZoneId.systemDefault())
            .hour
    }

    override fun currentLocalDate(): LocalDate {
        return Instant.ofEpochMilli(nowMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private val russianLocale: Locale = Locale.forLanguageTag("ru")

    override fun formatMonthYear(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMM yyyy", russianLocale))
    }

    override fun formatShortDate(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", russianLocale))
    }
}
