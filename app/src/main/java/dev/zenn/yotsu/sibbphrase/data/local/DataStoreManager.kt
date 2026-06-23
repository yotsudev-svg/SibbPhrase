package dev.zenn.yotsu.sibbphrase.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zenn.yotsu.sibbphrase.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sibbphrase_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_AUTO_DELETE_SEC = intPreferencesKey("auto_delete_sec")
        private val KEY_RESTORE_DISPLAY_SEC = intPreferencesKey("restore_display_sec")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_ONBOARDING_DONE] ?: false }

    val autoDeleteSec: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_AUTO_DELETE_SEC] ?: 60 }

    val restoreDisplaySec: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_RESTORE_DISPLAY_SEC] ?: 60 }

    val themeMode: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            val themeName = prefs[KEY_THEME_MODE] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }

    suspend fun markOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun setAutoDeleteSeconds(sec: Int) {
        context.dataStore.edit { it[KEY_AUTO_DELETE_SEC] = sec }
    }

    suspend fun setRestoreDisplaySeconds(sec: Int) {
        context.dataStore.edit { it[KEY_RESTORE_DISPLAY_SEC] = sec }
    }

    suspend fun setThemeMode(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME_MODE] = theme.name }
    }
}
