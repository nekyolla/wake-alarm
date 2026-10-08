package com.kindness.wakealarm.util

/**
 * Remembers which message bubbles have already been evaluated for each notification key.
 *
 * WhatsApp re-posts the same notification (same key) every time a chat gets a new message,
 * and the re-posted notification still carries the older unread bubbles. Without this,
 * an urgent bubble that already rang the alarm would ring it again on every later message
 * in that chat until the user opened WhatsApp.
 *
 * Messages are marked as seen as soon as they are evaluated — including while the Master Switch
 * is OFF — so turning the switch back ON never re-fires an alarm for an old message.
 */
class MessageDeduplicator(
    private val maxKeys: Int = 200,
    private val maxMessagesPerKey: Int = 100
) {

    /**
     * @param timestamp sender timestamp from MessagingStyle, or null when only plain text is available.
     * @param sender display label of who wrote this bubble; not part of its identity.
     */
    data class Message(val text: String, val timestamp: Long?, val sender: String = "") {
        internal val id: String get() = if (timestamp != null) "$timestamp|$text" else text
    }

    private val seenByKey = object : LinkedHashMap<String, LinkedHashSet<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LinkedHashSet<String>>?): Boolean =
            size > maxKeys
    }

    /**
     * Returns only the messages not seen before for [key] (in original order) and marks them as seen.
     */
    @Synchronized
    fun takeNew(key: String, messages: List<Message>): List<Message> {
        val seen = seenByKey.getOrPut(key) { LinkedHashSet() }
        val fresh = messages.filter { seen.add(it.id) }
        while (seen.size > maxMessagesPerKey) {
            seen.remove(seen.first())
        }
        return fresh
    }
}
