package com.kindness.wakealarm.util

/**
 * Decides what happens to the urgent messages found in one notification update.
 * Only one alarm rings at a time; every other urgent message is still recorded in the history
 * instead of being dropped.
 */
object TriggerPlanner {

    data class Plan<T>(val ring: T?, val recordOnly: List<T>)

    /**
     * @param triggered urgent messages, newest first.
     * @param alarmRunning whether an alarm is already ringing.
     */
    fun <T> plan(triggered: List<T>, alarmRunning: Boolean): Plan<T> =
        if (alarmRunning || triggered.isEmpty()) {
            Plan(ring = null, recordOnly = triggered)
        } else {
            Plan(ring = triggered.first(), recordOnly = triggered.drop(1))
        }
}
