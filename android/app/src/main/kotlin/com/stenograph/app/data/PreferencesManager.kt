package com.stenograph.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("stenograph_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val PC_IP = stringPreferencesKey("pc_ip")
        val PC_PORT = intPreferencesKey("pc_port")
        val IS_PAIRED = booleanPreferencesKey("is_paired")
    }

    val isPaired: Flow<Boolean> = context.dataStore.data.map { it[IS_PAIRED] ?: false }
    val authToken: Flow<String> = context.dataStore.data.map { it[AUTH_TOKEN] ?: "" }
    val pcIp: Flow<String> = context.dataStore.data.map { it[PC_IP] ?: "" }
    val pcPort: Flow<Int> = context.dataStore.data.map { it[PC_PORT] ?: 9476 }

    suspend fun savePairingInfo(token: String, ip: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[PC_IP] = ip
            prefs[PC_PORT] = port
            prefs[IS_PAIRED] = true
        }
    }

    suspend fun clearPairing() {
        context.dataStore.edit { it.clear() }
    }
}
