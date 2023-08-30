package be.planty.android.speechutils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

import java.util.HashMap
import java.util.Locale

/**
 * TODO: add the capability to aggregate different TTS engines with support different languages
 * getEngines() on API level 14
 */
class TtsProvider(context: Context, listener: TextToSpeech.OnInitListener) {

    private val mTts: TextToSpeech
    private val mAudioPauser: AudioPauser

    init {
        // TODO: use the 3-arg constructor (API 14) that supports passing the engine.
        // Choose the engine that supports the selected language, if there are several
        // then let the user choose.
        mTts = TextToSpeech(context, listener)
        mAudioPauser = AudioPauser(context, false)
        Log.i("Default TTS engine:" + mTts.defaultEngine)
    }


    @SuppressLint("NewApi")
    fun say(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                override fun onDone(utteranceId: String) {
                    mAudioPauser.resume()
                }

                override fun onError(utteranceId: String) {
                    mAudioPauser.resume()
                }

                override fun onStart(utteranceId: String) {}
            })
        } else {
            mTts.setOnUtteranceCompletedListener { mAudioPauser.resume() }
        }
        val params = HashMap<String, String>()
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTT_COMPLETED_FEEDBACK)
        mAudioPauser.pause()
        mTts.speak(text, TextToSpeech.QUEUE_FLUSH, params)
    }


    /**
     * Interrupts the current utterance and discards other utterances in the queue.
     *
     * @return {@ERROR} or {@SUCCESS}
     */
    fun stop(): Int {
        mAudioPauser.resume()
        // TODO: not sure which callbacks get called as a result of stop()
        return mTts.stop()
    }


    /**
     * TODO: is the language available on any engine (not just the default)
     */
    fun isLanguageAvailable(localeAsStr: String): Boolean {
        return mTts.isLanguageAvailable(Locale(localeAsStr)) >= 0
    }


    /**
     * TODO: set the language, changing the engine if the default engine
     * does not support this language
     */
    fun setLanguage(locale: Locale) {
        mTts.language = locale
    }


    /**
     * TODO: add this logic to setLanguage and deprecate this method
     */
    fun chooseLanguage(localeAsStr: String): Locale? {
        val locale = Locale(localeAsStr)
        if (mTts.isLanguageAvailable(locale) >= 0) {
            Log.i("Chose TTS: $localeAsStr -> $locale")
            return locale
        }
        val similarLocales = TtsLocaleMapper.getSimilarLocales(locale)
        if (similarLocales != null) {
            for (l in similarLocales) {
                if (mTts.isLanguageAvailable(l) >= 0) {
                    Log.i("Chose TTS: $localeAsStr -> $l from $similarLocales")
                    return l
                }
            }
        }
        Log.i("Chose TTS: $localeAsStr -> NULL from $similarLocales")
        return null
    }


    /**
     * Shuts down the TTS instance, resuming the audio if needed.
     */
    fun shutdown() {
        mTts.shutdown()
        mAudioPauser.resume()
    }

    companion object {

        private val UTT_COMPLETED_FEEDBACK = "UTT_COMPLETED_FEEDBACK"
    }

}