package com.cashbacktracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cashbacktracker.data.model.CashbackStatus

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val accountHolderCipherText: String,
    val ibanCipherText: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "cashback_devices")
data class CashbackDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notesCipherText: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "cashback_entries",
    foreignKeys = [
        ForeignKey(
            entity = BankAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseBankAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BankAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["payoutBankAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CashbackDeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("purchaseBankAccountId"),
        Index("payoutBankAccountId"),
        Index("deviceId"),
        Index("status"),
    ],
)
data class CashbackEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashbackUrl: String,
    val productName: String,
    val redemptionStartEpochDay: Long?,
    val redemptionEndEpochDay: Long?,
    val purchasePriceMinor: Long,
    val currency: String,
    val purchaseBankAccountId: Long?,
    val payoutBankAccountId: Long?,
    val deviceId: Long?,
    val notesCipherText: String,
    val status: CashbackStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
