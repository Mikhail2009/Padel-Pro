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
    fun formatFullMonthYear(epochMillis: Long): String = formatMonthYear(epochMillis)
    fun formatShortDateTime(epochMillis: Long): String = formatShortDate(epochMillis)
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

    override fun formatFullMonthYear(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("LLLL yyyy", russianLocale))
    }

    override fun formatShortDateTime(epochMillis: Long): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", russianLocale))
    }
}
