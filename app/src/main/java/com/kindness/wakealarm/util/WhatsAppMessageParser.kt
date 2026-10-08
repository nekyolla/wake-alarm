package com.kindness.wakealarm.util

import com.kindness.wakealarm.util.MessageDeduplicator.Message

/**
 * Turns the contents of a WhatsApp notification into individual message bubbles.
 * Kept free of Android types so the rules are unit-testable; the listener service only maps
 * `NotificationCompat.MessagingStyle` into [RawMessage]s.
 */
object WhatsAppMessageParser {

    /** A MessagingStyle `Person`, reduced to what we need. */
    data class Sender(val name: String?, val key: String?)

    /** One MessagingStyle message. [sender] is null when the notification doesn't name one. */
    data class RawMessage(val text: String?, val timestamp: Long, val sender: Sender?)

    /**
     * Parses MessagingStyle messages, dropping the user's own messages (e.g. replies sent from the
     * notification) and blank ones. Each bubble carries its own sender label, so in a group the
     * alarm names the person who actually wrote the urgent message.
     *
     * @param user the device owner (`MessagingStyle.user`).
     * @param conversationTitle the group name; null or the contact's name for one-to-one chats.
     * @param fallbackTitle the notification title, used when a bubble has no sender name.
     */
    fun parseMessagingStyle(
        messages: List<RawMessage>,
        user: Sender?,
        conversationTitle: String?,
        fallbackTitle: String
    ): List<Message> {
        // MessagingStyle marks the user's own messages with a null sender. That is only a reliable
        // signal when the other messages do name their sender; if none do, keep everything so an
        // urgent message is never silently skipped.
        val sendersPresent = messages.any { it.sender != null }
        return messages
            .filterNot { isOwnMessage(it, user, sendersPresent) }
            .mapNotNull { msg ->
                val text = msg.text?.trim().orEmpty()
                if (text.isBlank()) null else Message(text, msg.timestamp, senderLabel(msg.sender, conversationTitle, fallbackTitle))
            }
    }

    /**
     * Parses a notification without MessagingStyle: every distinct non-blank line is a bubble,
     * all attributed to the notification title.
     */
    fun parsePlainText(title: String, lines: List<String>, text: String?): List<Message> {
        val texts = LinkedHashSet<String>()
        (lines + listOfNotNull(text)).forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) texts.add(trimmed)
        }
        return texts.map { Message(it, null, title) }
    }

    private fun isOwnMessage(msg: RawMessage, user: Sender?, sendersPresent: Boolean): Boolean {
        val sender = msg.sender ?: return sendersPresent
        if (user == null) return false
        val sameKey = !user.key.isNullOrBlank() && sender.key == user.key
        val sameName = !user.name.isNullOrBlank() && sender.name == user.name
        return sameKey || sameName
    }

    private fun senderLabel(sender: Sender?, conversationTitle: String?, fallbackTitle: String): String {
        val name = sender?.name?.trim()
        val conversation = conversationTitle?.trim()
        return when {
            !name.isNullOrBlank() && !conversation.isNullOrBlank() && name != conversation -> "$name · $conversation"
            !name.isNullOrBlank() -> name
            else -> fallbackTitle
        }
    }
}
