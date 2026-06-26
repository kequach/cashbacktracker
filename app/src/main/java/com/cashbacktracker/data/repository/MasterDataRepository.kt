package com.cashbacktracker.data.repository

import com.cashbacktracker.data.crypto.SensitiveDataCipher
import com.cashbacktracker.data.crypto.decryptNullable
import com.cashbacktracker.data.crypto.encryptNullable
import com.cashbacktracker.data.local.BankAccountDao
import com.cashbacktracker.data.local.BankAccountEntity
import com.cashbacktracker.data.local.CashbackDeviceEntity
import com.cashbacktracker.data.local.DeviceDao
import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BankAccountRepository(
    private val bankAccountDao: BankAccountDao,
    private val cipher: SensitiveDataCipher,
) {
    val bankAccounts: Flow<List<BankAccount>> = bankAccountDao.observeBankAccounts()
        .map { entities -> entities.map { it.toDomain(cipher) } }

    suspend fun addBankAccount(nickname: String, accountHolder: String, iban: String): Long {
        val now = System.currentTimeMillis()
        return bankAccountDao.insert(
            BankAccountEntity(
                nickname = nickname.trim().ifBlank { iban.takeLast(4).padStart(4, '*') },
                accountHolderCipherText = cipher.encryptNullable(accountHolder.trim()),
                ibanCipherText = cipher.encryptNullable(iban.trim()),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val cipher: SensitiveDataCipher,
) {
    val devices: Flow<List<CashbackDevice>> = deviceDao.observeDevices()
        .map { entities -> entities.map { it.toDomain(cipher) } }

    suspend fun addDevice(name: String, notes: String): Long {
        val now = System.currentTimeMillis()
        return deviceDao.insert(
            CashbackDeviceEntity(
                name = name.trim(),
                notesCipherText = cipher.encryptNullable(notes.trim()),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

private fun BankAccountEntity.toDomain(cipher: SensitiveDataCipher): BankAccount =
    BankAccount(
        id = id,
        nickname = nickname,
        accountHolder = cipher.decryptNullable(accountHolderCipherText),
        iban = cipher.decryptNullable(ibanCipherText),
    )

private fun CashbackDeviceEntity.toDomain(cipher: SensitiveDataCipher): CashbackDevice =
    CashbackDevice(
        id = id,
        name = name,
        notes = cipher.decryptNullable(notesCipherText),
    )
