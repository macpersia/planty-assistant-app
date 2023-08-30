package be.planty.android.alexa.interfaces.alerts

import be.planty.android.alexa.interfaces.AvsItem

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * An AVS Item to handle setting alerts on the device
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 */
class AvsSetAlertItem
/**
 * Create a new AVSItem directive for an alert
 *
 * @param token the alert identifier
 * @param type the alert type
 * @param scheduledTime the alert time
 */
(token: String, var type: String?, var scheduledTime: String?) : AvsItem(token) {

    val scheduledTimeMillis: Long
        @Throws(ParseException::class)
        get() = date.time

    val hour: Int
        @Throws(ParseException::class)
        get() = date.hours
    val minutes: Int
        @Throws(ParseException::class)
        get() = date.minutes

    private val date: Date
        @Throws(ParseException::class)
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(scheduledTime)

    val isTimer: Boolean
        get() = type == TIMER

    val isAlarm: Boolean
        get() = type == ALARM

    companion object {

        val TIMER = "TIMER"
        val ALARM = "ALARM"
    }
}
