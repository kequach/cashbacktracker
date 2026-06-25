package com.cashbacktracker.data.repository

import com.cashbacktracker.data.crypto.SensitiveDataCipher
import com.cashbacktracker.data.crypto.decryptNullable
import com.cashbacktracker.data.crypto.encryptNullable
import com.cashbacktracker.data.local.CashbackDao
import com.cashbacktracker.data.local.CashbackEntryEntity
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.CashbackStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class CashbackRepository(
    private val cashbackDao: CashbackDao,
    private val cipher: SensitiveDataCipher,
) {
    val cashbacks: Flow<List<CashbackEntry>> = cashbackDao.observeCashbacks()
        .map { entities -> entities.map { it.toDomain(cipher) } }

    suspend fun addCashback(
        cashbackUrl: String,
        productName: String,
        redemptionStart: LocalDate?,
        redemptionEnd: LocalDate?,
        purchasePriceMinor: Long,
        bankAccountId: Long?,
        deviceId: Long?,
        notes: String,
        status: CashbackStatus,
    ) {
        val now = System.currentTimeMillis()
        cashbackDao.insert(
            CashbackEntryEntity(
                cashbackUrl = cashbackUrl.trim(),
                productName = productName.trim(),
                redemptionStartEpochDay = redemptionStart?.toEpochDay(),
                redemptionEndEpochDay = redemptionEnd?.toEpochDay(),
                purchasePriceMinor = purchasePriceMinor,
                currency = "EUR",
                bankAccountId = bankAccountId,
                deviceId = deviceId,
                notesCipherText = cipher.encryptNullable(notes.trim()),
                status = status,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun setStatus(id: Long, status: CashbackStatus) {
        cashbackDao.updateStatus(id, status, System.currentTimeMillis())
    }
}

private fun CashbackEntryEntity.toDomain(cipher: SensitiveDataCipher): CashbackEntry =
    CashbackEntry(
        id = id,
        cashbackUrl = cashbackUrl,
        productName = productName,
        redemptionStart = redemptionStartEpochDay?.let(LocalDate::ofEpochDay),
        redemptionEnd = redemptionEndEpochDay?.let(LocalDate::ofEpochDay),
        purchasePriceMinor = purchasePriceMinor,
        currency = currency,
        bankAccountId = bankAccountId,
        deviceId = deviceId,
        notes = cipher.decryptNullable(notesCipherText),
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
