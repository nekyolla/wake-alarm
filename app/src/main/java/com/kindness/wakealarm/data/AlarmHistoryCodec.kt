package com.kindness.wakealarm.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * One urgent message. Stored on-device only (never transmitted).
 *
 * @param whileRinging true when the message arrived while another alarm was already ringing,
 * so it was recorded without ringing again.
 */
data class AlarmEvent(
    val timestamp: Long,
    val sender: String,
    val message: String,
    val keywords: List<String>,
    val whileRinging: Boolean = false
)

/**
 * JSON encoding for the history list. Kept free of Android/DataStore types so it is unit-testable.
 * Corrupt or unexpected data decodes to an empty list instead of crashing the app, and entries
 * written by older versions (without newer fields) still decode.
 */
object AlarmHistoryCodec {

    fun encode(events: List<AlarmEvent>): String {
        val array = JSONArray()
        events.forEach { e ->
            val o = JSONObject()
                .put("t", e.timestamp)
                .put("s", e.sender)
                .put("m", e.message)
                .put("k", JSONArray(e.keywords))
            if (e.whileRinging) o.put("r", true)
            array.put(o)
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
                    keywords = if (k == null) emptyList() else (0 until k.length()).map { k.optString(it) },
                    whileRinging = o.optBoolean("r", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
