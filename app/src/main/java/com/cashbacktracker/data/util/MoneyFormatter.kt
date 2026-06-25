package com.cashbacktracker.data.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {
    private val locale = Locale.GERMANY

    fun parseMinor(input: String): Long? {
        val normalized = input
            .trim()
            .replace("EUR", "", ignoreCase = true)
            .replace("€", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        if (normalized.isBlank()) return null

        val sign = if (normalized.startsWith("-")) -1 else 1
        val unsigned = normalized.removePrefix("-")
        val parts = unsigned.split(".")
        if (parts.size > 2 || parts[0].isBlank()) return null

        val euros = parts[0].toLongOrNull() ?: return null
        val cents = parts.getOrNull(1)
            ?.padEnd(2, '0')
            ?.take(2)
            ?.toLongOrNull()
            ?: 0L

        return sign * ((euros * 100L) + cents)
    }

    fun formatMinor(amountMinor: Long?, currency: String = "EUR"): String {
        if (amountMinor == null) return "-"

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = java.util.Currency.getInstance(currency)
        }
        return formatter.format(amountMinor / 100.0)
    }

    fun formatPlainMinor(amountMinor: Long, currency: String = "EUR"): String {
        val major = amountMinor / 100
        val cents = abs(amountMinor % 100)
        return "%d,%02d %s".format(locale, major, cents, currency)
    }
}
