package com.willblaschko.android.alexa

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

import org.apache.commons.io.FileUtils

import java.io.File
import java.io.IOException
import java.util.HashMap

/**
 * A helper class that utilizes the TextToSpeech engine built into Android to turn a string-based AVS intent
 * into parsable raw audio that can be sent to the server. This is a work around that allows for more flexibility
 * if the application wants to pre/post-pend strings to the user's request.
 *
 * This could also be done using the SendEvent byte[] buffer with pre-recorded or generated audio
 */
class VoiceHelper
/**
 * Initalize our TextToSpeech engine, use a few tricks to get it to use a smaller file size
 * and be more easily recognized by the Alexa parser
 * @param context local/application level context
 */
private constructor(context: Context) {
    private val mContext: Context
    private val mTextToSpeech: TextToSpeech
    private var mIsIntialized = false

    internal var mCallbacks: MutableMap<String, SpeechFromTextCallback>? = HashMap()

    /**
     * Our TextToSpeech Init state changed listener
     */
    private val mInitListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            mIsIntialized = true
        } else {
            IllegalStateException("Unable to initialize Text to Speech engine").printStackTrace()
        }
    }

    /**
     * Our TextToSpeech UtteranceProgress state changed listener
     * We keep track of when we're done and pass back the byte[] raw audio of the recorded speech
     */
    private val mUtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {

        }

        override fun onDone(utteranceId: String) {
            //this allows us to keep track of multiple callbacks
            val callback = mCallbacks!![utteranceId]
            if (callback != null) {
                //get our cache file where we'll be storing the audio
                val cacheFile = getCacheFile(utteranceId)
                try {
                    val data = FileUtils.readFileToByteArray(cacheFile)
                    callback.onSuccess(data)
                } catch (e: IOException) {
                    e.printStackTrace()
                    //bubble up our error
                    callback.onError(e)
                }

                cacheFile.delete()
                //remove the utteranceId from our callback once we're done
                mCallbacks!!.remove(utteranceId)
            }
        }

        override fun onError(utteranceId: String) {
            //add more information to our error
            this.onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            if (mCallbacks == null) {
                return
            }
            val callback = mCallbacks!![utteranceId]
            callback?.onError(Exception("Unable to process request, error code: " + errorCode))
        }
    }

    /**
     * Get the TextToSpeech instance
     * @return our TextToSpeech instance, if it's initialized
     */
    private val textToSpeech: TextToSpeech
        get() {
            var count = 0

            while (!mIsIntialized) {
                if (count < 100) {
                    count++
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                } else {
                    throw IllegalStateException("Text to Speech engine is not initalized")
                }
            }
            return mTextToSpeech
        }

    //helper function to get the default cache dir for the app
    private val cacheDir: File
        get() = mContext.cacheDir

    init {
        mContext = context.applicationContext
        mTextToSpeech = TextToSpeech(mContext, mInitListener)
        mTextToSpeech.setPitch(.8f)
        mTextToSpeech.setSpeechRate(1.3f)
        mTextToSpeech.setOnUtteranceProgressListener(mUtteranceProgressListener)
    }

    /**
     * Create a new audio recording based on text passed in, update the callback with the changing states
     * @param text the text to render
     * @param callback
     */
    fun getSpeechFromText(text: String, callback: SpeechFromTextCallback) {

        //create a new unique ID
        val utteranceId = AuthorizationManager.createCodeVerifier()

        //add the callback to our list of callbacks
        mCallbacks!!.put(utteranceId, callback)

        //get our TextToSpeech engine
        val textToSpeech = textToSpeech

        //set up our arguments
        val params = HashMap<String, String>()
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        //request an update from TTS
        textToSpeech.synthesizeToFile(text, params, getCacheFile(utteranceId).toString())
    }

    /**
     * Our cache file based on the unique id generated for the intent
     * @param utteranceId
     * @return
     */
    private fun getCacheFile(utteranceId: String): File {
        return File(cacheDir, utteranceId + ".wav")
    }

    /**
     * State-based callback for the VoiceHelper class
     */
    interface SpeechFromTextCallback {
        fun onSuccess(data: ByteArray)
        fun onError(e: Exception)
    }

    companion object {

        private val TAG = "VoiceHelper"

        private var mInstance: VoiceHelper? = null

        /**
         * Get an instance of the VoiceHelper utility class, if it's currently null,
         * create a new instance
         * @param context
         * @return
         */
        fun getInstance(context: Context): VoiceHelper {
            if (mInstance == null) {
                mInstance = VoiceHelper(context)
            }
            return mInstance!!
        }
    }
}
