package com.sentinel.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "sentinel_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val MODEL_NAME_KEY = stringPreferencesKey("model_name")
        val TEMPERATURE_KEY = floatPreferencesKey("temperature")
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL_KEY] ?: "http://localhost:11434/"
    }

    val modelNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL_NAME_KEY] ?: "llama3.2"
    }

    val temperatureFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[TEMPERATURE_KEY] ?: 0.7f
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    suspend fun saveModelName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL_NAME_KEY] = name
        }
    }

    suspend fun saveTemperature(temp: Float) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE_KEY] = temp
        }
    }
}
