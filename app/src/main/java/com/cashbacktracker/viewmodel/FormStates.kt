package com.cashbacktracker.viewmodel

data class CashbackFormState(
    val cashbackUrl: String = "",
    val productName: String = "",
    val redemptionStart: String = "",
    val redemptionEnd: String = "",
    val purchasePrice: String = "",
    val purchaseBankAccountId: Long? = null,
    val payoutBankAccountId: Long? = null,
    val deviceId: Long? = null,
    val notes: String = "",
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
