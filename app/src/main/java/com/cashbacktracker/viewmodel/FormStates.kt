package com.cashbacktracker.viewmodel

import com.cashbacktracker.data.model.CashbackStatus

data class CashbackFormState(
    val cashbackUrl: String = "",
    val productName: String = "",
    val redemptionStart: String = "",
    val redemptionEnd: String = "",
    val purchasePrice: String = "",
    val bankAccountId: Long? = null,
    val deviceId: Long? = null,
    val notes: String = "",
    val status: CashbackStatus = CashbackStatus.PLANNED,
)

data class BankAccountFormState(
    val nickname: String = "",
    val accountHolder: String = "",
    val iban: String = "",
)

data class DeviceFormState(
    val name: String = "",
    val notes: String = "",
)
