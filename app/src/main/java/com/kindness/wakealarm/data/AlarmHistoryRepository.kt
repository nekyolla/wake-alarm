package com.kindness.wakealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alarm_history"
)

/**
 * One alarm that actually rang. Stored on-device only (never transmitted).
 */
data class AlarmEvent(
    val timestamp: Long,
    val sender: String,
    val message: String,
    val keywords: List<String>
)

/**
 * JSON encoding for the history list. Kept separate from the repository so it is unit-testable.
 * Corrupt or unexpected data decodes to an empty list instead of crashing the app.
 */
object AlarmHistoryCodec {

    fun encode(events: List<AlarmEvent>): String {
        val array = JSONArray()
        events.forEach { e ->
            array.put(
                JSONObject()
                    .put("t", e.timestamp)
                    .put("s", e.sender)
                    .put("m", e.message)
                    .put("k", JSONArray(e.keywords))
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<AlarmEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val k = o.optJSONArray("k")
                AlarmEvent(
                    timestamp = o.optLong("t"),
                    sender = o.optString("s"),
                    message = o.optString("m"),
                    keywords = if (k == null) emptyList() else (0 until k.length()).map { k.optString(it) }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * Keeps the most recent [MAX_EVENTS] alarms, newest first.
 */
class AlarmHistoryRepository(private val context: Context) {

    companion object {
        const val MAX_EVENTS = 30
        private val EVENTS_KEY = stringPreferencesKey("events")
    }

    val eventsFlow: Flow<List<AlarmEvent>> = context.historyDataStore.data
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
