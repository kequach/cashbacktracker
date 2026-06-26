package com.cashbacktracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.cashbacktracker.data.model.MilestoneSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val milestoneSettings: Flow<MilestoneSettings> = dataStore.data.map { preferences ->
        MilestoneSettings(
            celebrationsEnabled = preferences[MILESTONE_CELEBRATIONS_ENABLED] ?: true,
            shownMilestonesMinor = preferences[SHOWN_MILESTONES]
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                .orEmpty(),
        )
    }

    suspend fun setMilestoneCelebrationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MILESTONE_CELEBRATIONS_ENABLED] = enabled
        }
    }

    suspend fun markMilestoneShown(amountMinor: Long) {
        dataStore.edit { preferences ->
            val existing = preferences[SHOWN_MILESTONES].orEmpty()
            preferences[SHOWN_MILESTONES] = existing + amountMinor.toString()
        }
    }

    suspend fun keepShownMilestonesAtOrBelow(maxAmountMinor: Long) {
        dataStore.edit { preferences ->
            val existing = preferences[SHOWN_MILESTONES].orEmpty()
            val filtered = existing
                .mapNotNull { value ->
                    value.toLongOrNull()
                        ?.takeIf { it <= maxAmountMinor }
                        ?.toString()
                }
                .toSet()
            if (filtered != existing) {
                preferences[SHOWN_MILESTONES] = filtered
            }
        }
    }

    private companion object {
        val MILESTONE_CELEBRATIONS_ENABLED = booleanPreferencesKey("milestone_celebrations_enabled")
        val SHOWN_MILESTONES = stringSetPreferencesKey("shown_milestones_minor")
    }
}
