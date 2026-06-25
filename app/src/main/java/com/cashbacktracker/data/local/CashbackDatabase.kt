package com.cashbacktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cashbacktracker.data.model.CashbackStatus

@Database(
    entities = [
        BankAccountEntity::class,
        CashbackDeviceEntity::class,
        CashbackEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CashbackTypeConverters::class)
abstract class CashbackDatabase : RoomDatabase() {
    abstract fun cashbackDao(): CashbackDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun deviceDao(): DeviceDao
}

class CashbackTypeConverters {
    @TypeConverter
    fun cashbackStatusToString(value: CashbackStatus): String = value.name

    @TypeConverter
    fun stringToCashbackStatus(value: String): CashbackStatus = CashbackStatus.valueOf(value)
}
