package com.kindness.wakealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alarm_history",
    corruptionHandler = resetOnCorruption()
)

/**
 * Keeps the most recent [MAX_EVENTS] urgent messages, newest first.
 */
class AlarmHistoryRepository(private val context: Context) {

    companion object {
        const val MAX_EVENTS = 30
        private val EVENTS_KEY = stringPreferencesKey("events")
    }

    val eventsFlow: Flow<List<AlarmEvent>> = context.historyDataStore.data.orDefaultsOnIoError()
        .map { preferences -> AlarmHistoryCodec.decode(preferences[EVENTS_KEY]) }

    suspend fun add(event: AlarmEvent) {
        context.historyDataStore.edit { preferences ->
            val current = AlarmHistoryCodec.decode(preferences[EVENTS_KEY])
            preferences[EVENTS_KEY] = AlarmHistoryCodec.encode((listOf(event) + current).take(MAX_EVENTS))
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { preferences ->
            preferences.remove(EVENTS_KEY)
        }
    }
}
