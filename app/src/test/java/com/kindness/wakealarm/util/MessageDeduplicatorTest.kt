package com.kindness.wakealarm.util

import com.kindness.wakealarm.util.MessageDeduplicator.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDeduplicatorTest {

    private val urgent = Message("jaga urgent segera ke IGD", 1_000L)
    private val casual = Message("udah makan belum?", 2_000L)

    @Test
    fun `first sighting returns every message`() {
        val dedup = MessageDeduplicator()
        assertEquals(listOf(urgent, casual), dedup.takeNew("chat-a", listOf(urgent, casual)))
    }

    @Test
    fun `reposted notification only returns the new bubble`() {
        // The bug this guards against: WhatsApp re-posts the chat notification with the old
        // urgent bubble still in it, which used to ring the alarm again after dismissal.
        val dedup = MessageDeduplicator()
        dedup.takeNew("chat-a", listOf(urgent))
        assertEquals(listOf(casual), dedup.takeNew("chat-a", listOf(urgent, casual)))
    }

    @Test
    fun `identical repost returns nothing`() {
        val dedup = MessageDeduplicator()
        dedup.takeNew("chat-a", listOf(urgent, casual))
        assertTrue(dedup.takeNew("chat-a", listOf(urgent, casual)).isEmpty())
    }

    @Test
    fun `same text with a new timestamp is a new message`() {
        val dedup = MessageDeduplicator()
        dedup.takeNew("chat-a", listOf(urgent))
        val again = urgent.copy(timestamp = 5_000L)
        assertEquals(listOf(again), dedup.takeNew("chat-a", listOf(urgent, again)))
    }

    @Test
    fun `keys are tracked independently`() {
        val dedup = MessageDeduplicator()
        dedup.takeNew("chat-a", listOf(urgent))
        assertEquals(listOf(urgent), dedup.takeNew("chat-b", listOf(urgent)))
    }

    @Test
    fun `plain text messages without timestamp are deduplicated by text`() {
        val dedup = MessageDeduplicator()
        val line = Message("jaga urgent", null)
        dedup.takeNew("chat-a", listOf(line))
        assertTrue(dedup.takeNew("chat-a", listOf(line)).isEmpty())
    }

    @Test
    fun `per-key memory is bounded`() {
        val dedup = MessageDeduplicator(maxMessagesPerKey = 3)
        val messages = (1..5).map { Message("m$it", it.toLong()) }
        dedup.takeNew("chat-a", messages)
        // Oldest two were evicted, so they would count as new again; the newest three stay seen
        assertEquals(messages.take(2), dedup.takeNew("chat-a", messages))
    }

    @Test
    fun `number of tracked keys is bounded`() {
        val dedup = MessageDeduplicator(maxKeys = 2)
        dedup.takeNew("a", listOf(urgent))
        dedup.takeNew("b", listOf(urgent))
        dedup.takeNew("c", listOf(urgent)) // evicts "a"
        assertEquals(listOf(urgent), dedup.takeNew("a", listOf(urgent)))
        assertTrue(dedup.takeNew("c", listOf(urgent)).isEmpty())
    }
}
