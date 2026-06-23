package dev.zenn.yotsu.sibbphrase.biometric.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_settings")

class BiometricSettingStorage(context: Context) {

    // ✅ ActivityのContextが渡されてもApplicationContextに切り替えてリークを防ぐ
    private val appContext = context.applicationContext

    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val isBiometricEnabled: Flow<Boolean> = appContext.dataStore.data
        .map { preferences -> preferences[KEY_BIOMETRIC_ENABLED] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
}