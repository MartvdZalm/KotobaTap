package com.example.kotobatap.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object DataStoreManager {
    suspend fun <T> putValue(
        context: Context,
        key: String,
        value: T,
    ) {
        when (value) {
            is String ->
                context.dataStore.edit { preferences ->
                    preferences[
                        stringPreferencesKey(
                            key,
                        ),
                    ] = value
                }

            is Int ->
                context.dataStore.edit { preferences ->
                    preferences[
                        intPreferencesKey(key),
                    ] = value
                }

            is Boolean ->
                context.dataStore.edit { preferences ->
                    preferences[
                        booleanPreferencesKey(
                            key,
                        ),
                    ] = value
                }

            is Long ->
                context.dataStore.edit { preferences ->
                    preferences[
                        longPreferencesKey(key),
                    ] = value
                }

            is Float ->
                context.dataStore.edit { preferences ->
                    preferences[
                        floatPreferencesKey(key),
                    ] = value
                }

            is Double ->
                context.dataStore.edit { preferences ->
                    preferences[
                        doublePreferencesKey(
                            key,
                        ),
                    ] = value
                }

            else -> throw IllegalArgumentException("This type is not supported")
        }
    }

    fun getStringValue(
        context: Context,
        key: String,
        defaultValue: String = "",
    ): Flow<String> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[stringPreferencesKey(key)] ?: defaultValue
            }
    }

    fun getIntValue(
        context: Context,
        key: String,
        defaultValue: Int = 0,
    ): Flow<Int> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[intPreferencesKey(key)] ?: defaultValue
            }
    }

    fun getBooleanValue(
        context: Context,
        key: String,
        defaultValue: Boolean = false,
    ): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[booleanPreferencesKey(key)] ?: defaultValue
            }
    }

    suspend fun clearDataStore(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun removeKey(
        context: Context,
        key: String,
    ) {
        context.dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
        }
    }
}
