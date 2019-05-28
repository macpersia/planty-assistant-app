package ee.ioc.phon.android.speechutils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.SystemClock

// TODO: add a method that calls back when audio is finished
class AudioCue {

    private val mContext: Context
    private val mStartSound: Int
    private val mStopSound: Int
    private val mErrorSound: Int

    constructor(context: Context) {
        mContext = context
        mStartSound = R.raw.explore_begin
        mStopSound = R.raw.explore_end
        mErrorSound = R.raw.error
    }

    constructor(context: Context, startSound: Int, stopSound: Int, errorSound: Int) {
        mContext = context
        mStartSound = startSound
        mStopSound = stopSound
        mErrorSound = errorSound
    }

    fun playStartSoundAndSleep() {
        if (playSound(mStartSound)) {
            SystemClock.sleep(DELAY_AFTER_START_BEEP)
        }
    }


    fun playStopSound() {
        playSound(mStopSound)
    }


    fun playErrorSound() {
        playSound(mErrorSound)
    }


    private fun playSound(sound: Int): Boolean {
        val mp = MediaPlayer.create(mContext, sound) ?: return false
        // create can return null, e.g. on Android Wear
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mp.setOnCompletionListener(object : MediaPlayer.OnCompletionListener() {
            @Override
            fun onCompletion(mp: MediaPlayer) {
                mp.release()
            }
        })
        mp.start()
        return true
    }

    companion object {

        private val DELAY_AFTER_START_BEEP = 200
    }

}