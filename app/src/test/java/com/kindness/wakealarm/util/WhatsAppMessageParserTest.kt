package com.kindness.wakealarm.util

import com.kindness.wakealarm.util.MessageDeduplicator.Message
import com.kindness.wakealarm.util.WhatsAppMessageParser.RawMessage
import com.kindness.wakealarm.util.WhatsAppMessageParser.Sender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppMessageParserTest {

    private val me = Sender(name = "Anda", key = "me")
    private val rina = Sender(name = "Rina", key = "rina")
    private val budi = Sender(name = "Budi", key = "budi")

    @Test
    fun `each bubble in a group keeps its own sender`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            messages = listOf(
                RawMessage("jaga urgent segera ke IGD", 1L, rina),
                RawMessage("oke siap", 2L, budi)
            ),
            user = me,
            conversationTitle = "Koas Anak",
            fallbackTitle = "Koas Anak: 2 pesan"
        )
        assertEquals(
            listOf(
                Message("jaga urgent segera ke IGD", 1L, "Rina · Koas Anak"),
                Message("oke siap", 2L, "Budi · Koas Anak")
            ),
            parsed
        )
    }

    @Test
    fun `one to one chat uses the contact name`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(RawMessage("segera ke IGD", 1L, rina)), me, conversationTitle = "Rina", fallbackTitle = "Rina"
        )
        assertEquals("Rina", parsed.single().sender)
    }

    @Test
    fun `missing sender name falls back to the notification title`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(RawMessage("segera", 1L, Sender(name = null, key = "x"))), me, null, "Grup Jaga"
        )
        assertEquals("Grup Jaga", parsed.single().sender)
    }

    @Test
    fun `own replies are dropped by key or by name`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(
                RawMessage("jaga urgent", 1L, rina),
                RawMessage("urgent segera saya ke IGD", 2L, Sender(name = "Something else", key = "me")),
                RawMessage("igd segera", 3L, Sender(name = "Anda", key = null))
            ),
            me, null, "Rina"
        )
        assertEquals(listOf("jaga urgent"), parsed.map { it.text })
    }

    @Test
    fun `null sender is the user when other bubbles name their sender`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(RawMessage("jaga urgent", 1L, rina), RawMessage("ok segera ke igd", 2L, null)),
            me, null, "Rina"
        )
        assertEquals(listOf("jaga urgent"), parsed.map { it.text })
    }

    @Test
    fun `when no bubble names a sender nothing is dropped`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(RawMessage("jaga urgent", 1L, null), RawMessage("segera ke igd", 2L, null)),
            user = null, conversationTitle = null, fallbackTitle = "Rina"
        )
        assertEquals(listOf("jaga urgent", "segera ke igd"), parsed.map { it.text })
        assertTrue(parsed.all { it.sender == "Rina" })
    }

    @Test
    fun `blank bubbles are skipped and text is trimmed`() {
        val parsed = WhatsAppMessageParser.parseMessagingStyle(
            listOf(RawMessage("   ", 1L, rina), RawMessage(null, 2L, rina), RawMessage("  igd  ", 3L, rina)),
            me, null, "Rina"
        )
        assertEquals(listOf(Message("igd", 3L, "Rina")), parsed)
    }

    @Test
    fun `plain text lines are deduplicated and attributed to the title`() {
        val parsed = WhatsAppMessageParser.parsePlainText(
            title = "Rina",
            lines = listOf("jaga urgent", " jaga urgent ", ""),
            text = "segera ke IGD"
        )
        assertEquals(
            listOf(Message("jaga urgent", null, "Rina"), Message("segera ke IGD", null, "Rina")),
            parsed
        )
    }
}
