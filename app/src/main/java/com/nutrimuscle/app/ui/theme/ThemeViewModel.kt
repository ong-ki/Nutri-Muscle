package com.nutrimuscle.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimuscle.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nutrimuscle_theme"
)

data class ThemeState(
    val preset: ThemePreset = ThemePreset.PASTEL,
    val mode: ThemeMode = ThemeMode.SYSTEM
)

@Singleton
class ThemePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRESET = stringPreferencesKey("theme_preset")
        val MODE = stringPreferencesKey("theme_mode")
    }

    val themeStateFlow: Flow<ThemeState> = context.themeDataStore.data.map { prefs ->
        val preset = ThemePreset.fromKey(prefs[Keys.PRESET])
        val mode = prefs[Keys.MODE]?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.SYSTEM
        ThemeState(preset = preset, mode = mode)
    }

    suspend fun setPreset(preset: ThemePreset) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.PRESET] = preset.storageKey
        }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.MODE] = mode.name
        }
    }
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePrefs: ThemePreferencesDataSource
) : ViewModel() {

    val availablePresets: List<ThemePreset> = ThemePreset.availablePresets()

    val themeState: StateFlow<ThemeState> = themePrefs.themeStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeState()
    )

    fun setPreset(preset: ThemePreset) {
        viewModelScope.launch {
            themePrefs.setPreset(preset)
        }
    }

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePrefs.setMode(mode)
        }
    }
}
