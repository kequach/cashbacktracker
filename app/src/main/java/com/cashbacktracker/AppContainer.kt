package com.cashbacktracker

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.cashbacktracker.data.crypto.AndroidKeystoreAesGcmCipher
import com.cashbacktracker.data.export.ExportService
import com.cashbacktracker.data.local.CashbackDatabase
import com.cashbacktracker.data.parser.CashbackPromotionParser
import com.cashbacktracker.data.parser.HttpPromotionPageFetcher
import com.cashbacktracker.data.repository.BankAccountRepository
import com.cashbacktracker.data.repository.CashbackRepository
import com.cashbacktracker.data.repository.DeviceRepository
import com.cashbacktracker.data.repository.SettingsRepository

private val Context.cashbackSettings by preferencesDataStore(name = "cashback_settings")

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val cipher = AndroidKeystoreAesGcmCipher()
    private val database = Room.databaseBuilder(
        applicationContext,
        CashbackDatabase::class.java,
        "cashback-tracker-v1.db",
    )
        .fallbackToDestructiveMigration(true)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .build()

    val cashbackRepository = CashbackRepository(database.cashbackDao(), cipher)
    val bankAccountRepository = BankAccountRepository(database.bankAccountDao(), cipher)
    val deviceRepository = DeviceRepository(database.deviceDao(), cipher)
    val settingsRepository = SettingsRepository(applicationContext.cashbackSettings)
    val parser = CashbackPromotionParser(HttpPromotionPageFetcher())
    val exportService = ExportService()
}
