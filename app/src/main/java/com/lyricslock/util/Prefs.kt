package com.lyricslock.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PrefKeys {
    val LOCK_SCREEN_ENABLED = booleanPreferencesKey("lock_screen_enabled")
}

suspend fun Context.setLockScreenEnabled(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.LOCK_SCREEN_ENABLED] = enabled }
}

fun Context.lockScreenEnabledFlow(): Flow<Boolean> =
    dataStore.data.map { it[PrefKeys.LOCK_SCREEN_ENABLED] ?: true }
