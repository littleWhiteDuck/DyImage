package hua.dy.image.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hua.dy.image.data.SortOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import splitties.init.appCtx

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "eimage_settings")

enum class ThemeMode {
    System, Light, Dark
}

data class AppSettings(
    val sortType: Int = 0,
    val minScanFileSizeKb: Int = 32,
    val preferShizuku: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val followSystemDynamicColor: Boolean = true,
    val activeSchemeId: Long = 1L
)

object AppSettingsStore {

    private val sortTypeKey = intPreferencesKey("sort_type")
    private val minScanSizeKey = intPreferencesKey("scan_min_file_size_kb")
    private val preferShizukuKey = booleanPreferencesKey("prefer_shizuku_scan")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val followDynamicColorKey = booleanPreferencesKey("follow_dynamic_color")
    private val activeSchemeIdKey = longPreferencesKey("active_scheme_id")

    val settingsFlow: Flow<AppSettings> = appCtx.appDataStore.data.map { pref ->
        AppSettings(
            sortType = (pref[sortTypeKey] ?: 0).coerceIn(0, SortOptions.labels.lastIndex),
            minScanFileSizeKb = (pref[minScanSizeKey] ?: 32).coerceIn(2, 4096),
            preferShizuku = pref[preferShizukuKey] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(pref[themeModeKey] ?: ThemeMode.System.name)
            }.getOrDefault(ThemeMode.System),
            followSystemDynamicColor = pref[followDynamicColorKey] ?: true,
            activeSchemeId = pref[activeSchemeIdKey] ?: 1L
        )
    }

    suspend fun setSortType(value: Int) {
        appCtx.appDataStore.edit { it[sortTypeKey] = value.coerceIn(0, SortOptions.labels.lastIndex) }
    }

    suspend fun setMinScanFileSizeKb(value: Int) {
        appCtx.appDataStore.edit { it[minScanSizeKey] = value.coerceIn(2, 4096) }
    }

    suspend fun setPreferShizuku(value: Boolean) {
        appCtx.appDataStore.edit { it[preferShizukuKey] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appCtx.appDataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setFollowSystemDynamicColor(value: Boolean) {
        appCtx.appDataStore.edit { it[followDynamicColorKey] = value }
    }

    suspend fun setActiveSchemeId(value: Long) {
        appCtx.appDataStore.edit { it[activeSchemeIdKey] = value }
    }
}
