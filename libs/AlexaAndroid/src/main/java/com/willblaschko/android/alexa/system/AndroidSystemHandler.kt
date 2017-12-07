package com.willblaschko.android.alexa.system

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

import com.willblaschko.android.alexa.AlexaManager
import com.willblaschko.android.alexa.callbacks.ImplAsyncCallback
import com.willblaschko.android.alexa.data.Directive
import com.willblaschko.android.alexa.data.Event
import com.willblaschko.android.alexa.interfaces.AvsItem
import com.willblaschko.android.alexa.interfaces.AvsResponse
import com.willblaschko.android.alexa.interfaces.alerts.AvsDeleteAlertItem
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem
import com.willblaschko.android.alexa.interfaces.response.ResponseParser
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem
import com.willblaschko.android.alexa.interfaces.system.AvsSetEndpointItem
import com.willblaschko.android.alexa.service.DownChannelService

import java.io.IOException
import java.text.ParseException

import android.content.Context.AUDIO_SERVICE

/**
 * Created by will on 4/8/2017.
 */

class AndroidSystemHandler private constructor(context: Context) {
    private val context: Context

    init {
        this.context = context.applicationContext
    }

    fun handleItems(response: AvsResponse) {
        for (current in response) {

            Log.i(TAG, "Handling AvsItem: " + current.javaClass)
            if (current is AvsSetEndpointItem) {
                Log.i(TAG, "Setting URL endpoint: " + current.endpoint)
                AlexaManager.getInstance(context).urlEndpoint = current.endpoint

                context.stopService(Intent(context, DownChannelService::class.java))
                context.startService(Intent(context, DownChannelService::class.java))
            } else if (current is AvsSetVolumeItem) {
                //set our volume
                setVolume(current.volume)
            } else if (current is AvsAdjustVolumeItem) {
                //adjust the volume
                adjustVolume(current.adjustment)
            } else if (current is AvsSetMuteItem) {
                //mute/unmute the device
                setMute(current.isMute)
            } else if (current is AvsMediaPlayCommandItem) {
                //fake a hardware "play" press
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY)
                Log.i(TAG, "Media play command issued")
            } else if (current is AvsMediaPauseCommandItem) {
                //fake a hardware "pause" press
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_PAUSE)
                Log.i(TAG, "Media pause command issued")
            } else if (current is AvsMediaNextCommandItem) {
                //fake a hardware "next" press
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
                Log.i(TAG, "Media next command issued")
            } else if (current is AvsMediaPreviousCommandItem) {
                //fake a hardware "previous" press
                sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                Log.i(TAG, "Media previous command issued")
            } else if (current is AvsSetAlertItem) {
                if (current.isAlarm) {
                    setAlarm(current)
                } else if (current.isTimer) {
                    setTimer(current)
                }
            } else if (current is AvsDeleteAlertItem) {

            }
        }
    }


    fun handleDirective(directive: Directive) {
        try {
            val item = ResponseParser.parseDirective(directive)
            handleItem(item)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun handleItem(item: AvsItem?) {
        if (item == null) {
            return
        }
        val response = AvsResponse()
        response.add(item)
        handleItems(response)
    }

    private fun setTimer(item: AvsSetAlertItem) {
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
        try {
            val time = ((item.scheduledTimeMillis - System.currentTimeMillis()) / 1000).toInt()
            i.putExtra(AlarmClock.EXTRA_LENGTH, time)
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            context.startActivity(i)
            AlexaManager.getInstance(context)
                    .sendEvent(Event.getSetAlertSucceededEvent(item.token), null)

            //cheating way to tell Alexa that the timer happened successfully--this SHOULD be improved
            //todo make this better
            Handler(Looper.getMainLooper()).postDelayed({
                AlexaManager.getInstance(context)
                        .sendEvent(Event.getAlertStartedEvent(item.token), object : ImplAsyncCallback<AvsResponse, Exception>() {
                            override fun complete() {
                                AlexaManager.getInstance(context)
                                        .sendEvent(Event.getAlertStoppedEvent(item.token), null)
                            }
                        })
            }, (time * 1000).toLong())
        } catch (e: ParseException) {
            e.printStackTrace()
        }

    }

    private fun setAlarm(item: AvsSetAlertItem) {
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
        try {
            i.putExtra(AlarmClock.EXTRA_HOUR, item.hour)
            i.putExtra(AlarmClock.EXTRA_MINUTES, item.minutes)
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            context.startActivity(i)
            AlexaManager.getInstance(context)
                    .sendEvent(Event.getSetAlertSucceededEvent(item.token), null)

        } catch (e: ParseException) {
            e.printStackTrace()
        }

    }

    private fun adjustVolume(adjust: Long) {
        setVolume(adjust, true)
    }

    private fun setVolume(volume: Long, adjust: Boolean = false) {

        val am = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var vol = am.getStreamVolume(AudioManager.STREAM_MUSIC).toLong()
        if (adjust) {
            vol += volume * max / 100
        } else {
            vol = volume * max / 100
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, vol.toInt(), AudioManager.FLAG_VIBRATE)

        AlexaManager.getInstance(context).sendVolumeChangedEvent(volume, vol == 0L, null)

        Handler(Looper.getMainLooper()).post {
            if (adjust) {
                Toast.makeText(context, "Volume adjusted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Volume set to: " + volume / 10, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setMute(isMute: Boolean) {
        val am = context.getSystemService(AUDIO_SERVICE) as AudioManager
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute)

        AlexaManager.getInstance(context).sendMutedEvent(isMute, null)

        Log.i(TAG, "Mute set to : " + isMute)

        Handler(Looper.getMainLooper()).post { Toast.makeText(context, "Volume " + if (isMute) "muted" else "unmuted", Toast.LENGTH_SHORT).show() }


    }

    companion object {
        private val TAG = "AndroidSystemHandler"
        lateinit private var instance: AndroidSystemHandler
        fun getInstance(context: Context): AndroidSystemHandler {
            if (!::instance.isInitialized) {
                instance = AndroidSystemHandler(context)
            }
            return instance
        }


        /**
         * Force the device to think that a hardware button has been pressed, this is used for Play/Pause/Previous/Next Media commands
         * @param keyCode keycode for the hardware button we're emulating
         */
        private fun sendMediaButton(keyCode: Int) {
            val inst = Instrumentation()
            inst.sendKeyDownUpSync(keyCode)
        }
    }
}
