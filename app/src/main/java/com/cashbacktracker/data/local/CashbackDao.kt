package com.cashbacktracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CashbackDao {
    @Query("SELECT * FROM cashback_entries ORDER BY createdAt DESC")
    fun observeCashbacks(): Flow<List<CashbackEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CashbackEntryEntity): Long

    @Query("UPDATE cashback_entries SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: com.cashbacktracker.data.model.CashbackStatus, updatedAt: Long)
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY nickname, id")
    fun observeBankAccounts(): Flow<List<BankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BankAccountEntity): Long
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM cashback_devices ORDER BY name")
    fun observeDevices(): Flow<List<CashbackDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CashbackDeviceEntity): Long
}
