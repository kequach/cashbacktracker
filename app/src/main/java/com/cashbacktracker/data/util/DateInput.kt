package com.cashbacktracker.data.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateInput {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(input: String): LocalDate? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        return try {
            LocalDate.parse(trimmed, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun format(date: LocalDate?): String = date?.format(formatter).orEmpty()
}
