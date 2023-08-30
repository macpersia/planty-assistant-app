package be.planty.assistant.app

import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import be.planty.android.alexa.AlexaManager
import be.planty.android.alexa.audioplayer.AlexaAudioPlayer
import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.interfaces.AvsItem
import be.planty.android.alexa.interfaces.AvsResponse
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayAudioItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayContentItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem
import be.planty.android.alexa.interfaces.errors.AvsResponseException
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsStopItem
import be.planty.android.alexa.interfaces.speaker.AvsAdjustVolumeItem
import be.planty.android.alexa.interfaces.speaker.AvsSetMuteItem
import be.planty.android.alexa.interfaces.speaker.AvsSetVolumeItem
import be.planty.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem
import be.planty.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem
import be.planty.assistant.app.actions.BaseListenerFragment
import be.planty.assistant.app.global.Constants.PRODUCT_ID
import com.amazon.identity.auth.device.AuthError

/**
 * @author will on 5/30/2016.
 */

abstract class BaseActivity : AppCompatActivity(), BaseListenerFragment.AvsListenerInterface {

    private var alexaManager: AlexaManager? = null
    private var audioPlayer: AlexaAudioPlayer? = null
    private val avsQueue = ArrayList<AvsItem>()

    private var startTime: Long = 0

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private val alexaAudioPlayerCallback = object : AlexaAudioPlayer.Callback {

        private var almostDoneFired = false
        private var playbackStartedFired = false

        override fun playerPrepared(pendingItem: AvsItem?) {

        }

        override fun playerProgress(item: AvsItem?, offsetInMilliseconds: Long, percent: Float) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Player percent: $percent")
            }
            if (item is AvsPlayContentItem || item == null) {
                return
            }
            if (!playbackStartedFired) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "PlaybackStarted " + item.token + " fired: " + percent)
                }
                playbackStartedFired = true
                sendPlaybackStartedEvent(item)
            }
            if (!almostDoneFired && percent > .8f) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "AlmostDone " + item.token + " fired: " + percent)
                }
                almostDoneFired = true
                if (item is AvsPlayAudioItem) {
                    sendPlaybackNearlyFinishedEvent(item as AvsPlayAudioItem?, offsetInMilliseconds)
                }
            }
        }

        override fun itemComplete(completedItem: AvsItem?) {
            almostDoneFired = false
            playbackStartedFired = false
            avsQueue.remove(completedItem)
            checkQueue()
            if (completedItem is AvsPlayContentItem || completedItem == null) {
                return
            }
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Complete " + completedItem.token + " fired")
            }
            sendPlaybackFinishedEvent(completedItem)
        }

        override fun playerError(item: AvsItem?, what: Int, extra: Int): Boolean {
            return false
        }

        override fun dataError(item: AvsItem?, e: Exception) {
            e.printStackTrace()
        }
    }

    //async callback for commands sent to Alexa Voice
    override val requestCallback = object : AsyncCallback<AvsResponse, Exception?> {
        override fun start() {
            startTime = System.currentTimeMillis()
            Log.i(TAG, "Event Start")
            setState(STATE_PROCESSING)
        }

        override fun success(result: AvsResponse) {
            Log.i(TAG, "Event Success")
            handleResponse(result)
        }

        override fun failure(error: AuthError?) {
            error!!.printStackTrace()
            Log.i(TAG, "Event Error")
            setState(STATE_FINISHED)
        }

        override fun complete() {
            Log.i(TAG, "Event Complete")
            Handler(Looper.getMainLooper()).post {
                val totalTime = System.currentTimeMillis() - startTime
                Toast.makeText(this@BaseActivity, "Total request time: $totalTime miliseconds", Toast.LENGTH_LONG).show()
                //Log.i(TAG, "Total request time: "+totalTime+" miliseconds");
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initAlexaAndroid()
    }

    override fun onStop() {
        super.onStop()
        if (audioPlayer != null) {
            audioPlayer!!.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioPlayer != null) {
            //remove callback to avoid memory leaks
            audioPlayer!!.removeCallback(alexaAudioPlayerCallback)
            audioPlayer!!.release()
        }
    }

    private fun initAlexaAndroid() {
        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(this, PRODUCT_ID)

        //instantiate our audio player
        audioPlayer = AlexaAudioPlayer.getInstance(this)

        //Remove the current item and check for more items once we've finished playing
        audioPlayer!!.addCallback(alexaAudioPlayerCallback)

        //open our downchannel
        //alexaManager.sendOpenDownchannelDirective(requestCallback);


        //synchronize our device
        //alexaManager.sendSynchronizeStateEvent(requestCallback);
    }

    /**
     * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private fun sendPlaybackNearlyFinishedEvent(item: AvsPlayAudioItem?, offsetInMilliseconds: Long) {
        if (item != null) {
            alexaManager!!.sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, requestCallback)
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent")
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private fun sendPlaybackStartedEvent(item: AvsItem?) {
        alexaManager!!.sendPlaybackStartedEvent(item, 0, null)
        Log.i(TAG, "Sending SpeechStartedEvent")
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private fun sendPlaybackFinishedEvent(item: AvsItem?) {
        if (item != null) {
            object : AsyncCallback<AvsResponse, AvsResponseException> {
                override fun start() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun success(result: AvsResponse) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun failure(error: AuthError?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun complete() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
            alexaManager!!.sendPlaybackFinishedEvent(item, null )
            Log.i(TAG, "Sending PlaybackFinishedEvent")
        }
    }

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
    </AvsItem> */
    private fun handleResponse(response: AvsResponse?) {
        val checkAfter = avsQueue.size == 0
        if (response != null) {
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for (i in response.indices.reversed()) {
                if (response[i] is AvsReplaceAllItem || response[i] is AvsReplaceEnqueuedItem) {
                    //clear our queue
                    avsQueue.clear()
                    //remove item
                    response.removeAt(i)
                }
            }
            Log.i(TAG, "Adding " + response.size + " items to our queue")
            if (BuildConfig.DEBUG) {
                for (i in response.indices) {
                    Log.i(TAG, "\tAdding: " + response[i].token)
                }
            }
            avsQueue.addAll(response)
        }
        if (checkAfter) {
            checkQueue()
        }
    }


    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     *
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private fun checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size == 0) {
            setState(STATE_FINISHED)
            Handler(Looper.getMainLooper()).post {
                val totalTime = System.currentTimeMillis() - startTime
                Toast.makeText(this@BaseActivity, "Total interaction time: $totalTime miliseconds", Toast.LENGTH_LONG).show()
                Log.i(TAG, "Total interaction time: $totalTime miliseconds")
            }
            return
        }

        val current = avsQueue[0]

        Log.i(TAG, "Item type " + current.javaClass.name)

        if (current is AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer!!.isPlaying) {
                audioPlayer!!.playItem(current)
            }
        } else if (current is AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer!!.isPlaying) {
                audioPlayer!!.playItem(current)
            }
        } else if (current is AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer!!.isPlaying) {
                audioPlayer!!.playItem(current)
            }
            setState(STATE_SPEAKING)
        } else if (current is AvsStopItem) {
            //stop our play
            audioPlayer!!.stop()
            avsQueue.remove(current)
        } else if (current is AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer!!.stop()
            avsQueue.remove(current)
        } else if (current is AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();
            avsQueue.remove(current)
        } else if (current is AvsExpectSpeechItem) {

            //listen for user input
            audioPlayer!!.stop()
            avsQueue.clear()
            startListening()
        } else if (current is AvsSetVolumeItem) {
            //set our volume
            setVolume(current.volume)
            avsQueue.remove(current)
        } else if (current is AvsAdjustVolumeItem) {
            //adjust the volume
            adjustVolume(current.adjustment)
            avsQueue.remove(current)
        } else if (current is AvsSetMuteItem) {
            //mute/unmute the device
            setMute(current.isMute)
            avsQueue.remove(current)
        } else if (current is AvsMediaPlayCommandItem) {
            //fake a hardware "play" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY)
            Log.i(TAG, "Media play command issued")
            avsQueue.remove(current)
        } else if (current is AvsMediaPauseCommandItem) {
            //fake a hardware "pause" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE)
            Log.i(TAG, "Media pause command issued")
            avsQueue.remove(current)
        } else if (current is AvsMediaNextCommandItem) {
            //fake a hardware "next" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT)
            Log.i(TAG, "Media next command issued")
            avsQueue.remove(current)
        } else if (current is AvsMediaPreviousCommandItem) {
            //fake a hardware "previous" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Log.i(TAG, "Media previous command issued")
            avsQueue.remove(current)
        } else if (current is AvsResponseException) {
            runOnUiThread {
                AlertDialog.Builder(this@BaseActivity)
                        .setTitle("Error")
                        .setMessage(current.directive.payload.code + ": " + current.directive.payload.description)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }

            avsQueue.remove(current)
            checkQueue()
        }
    }

    protected abstract fun startListening()

    private fun adjustVolume(adjust: Long) {
        setVolume(adjust, true)
    }

    private fun setVolume(volume: Long, adjust: Boolean = false) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var vol = am.getStreamVolume(AudioManager.STREAM_MUSIC).toLong()
        if (adjust) {
            vol += volume * max / 100
        } else {
            vol = volume * max / 100
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, vol.toInt(), AudioManager.FLAG_VIBRATE)

        alexaManager!!.sendVolumeChangedEvent(volume, vol == 0L, requestCallback)

        Log.i(TAG, "Volume set to : $vol/$max ($volume)")

        Handler(Looper.getMainLooper()).post {
            if (adjust) {
                Toast.makeText(this@BaseActivity, "Volume adjusted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@BaseActivity, "Volume set to: " + volume / 10, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setMute(isMute: Boolean) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute)

        alexaManager!!.sendMutedEvent(isMute, requestCallback)

        Log.i(TAG, "Mute set to : $isMute")

        Handler(Looper.getMainLooper()).post { Toast.makeText(this@BaseActivity, "Volume " + if (isMute) "muted" else "unmuted", Toast.LENGTH_SHORT).show() }
    }

    private fun setState(state: Int) {
        runOnUiThread {
            when (state) {
                STATE_LISTENING -> stateListening()
                STATE_PROCESSING -> stateProcessing()
                STATE_SPEAKING -> stateSpeaking()
                STATE_FINISHED -> stateFinished()
                STATE_PROMPTING -> statePrompting()
                else -> stateNone()
            }
        }
    }

    protected abstract fun stateListening()
    protected abstract fun stateProcessing()
    protected abstract fun stateSpeaking()
    protected abstract fun stateFinished()
    protected abstract fun statePrompting()
    protected abstract fun stateNone()

    companion object {

        private const val TAG = "BaseActivity"

        private const val STATE_LISTENING = 1
        private const val STATE_PROCESSING = 2
        private const val STATE_SPEAKING = 3
        private const val STATE_PROMPTING = 4
        private const val STATE_FINISHED = 0

        /**
         * Force the device to think that a hardware button has been pressed, this is used for Play/Pause/Previous/Next Media commands
         * @param context
         * @param keyCode keycode for the hardware button we're emulating
         */
        private fun sendMediaButton(context: Context, keyCode: Int) {
            val inst = Instrumentation()
            inst.sendKeyDownUpSync(keyCode)
        }
    }

}
