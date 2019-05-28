package ee.ioc.phon.android.speechutils

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener

/**
 * Pauses the audio stream by requesting the audio focus and
 * muting the music stream.
 *
 *
 * TODO: Test this is two interleaving instances of AudioPauser, e.g.
 * TTS starts playing and calls the AudioPauser, at the same time
 * the recognizer starts listening and also calls the AudioPauser.
 */
class AudioPauser @JvmOverloads constructor(context: Context, private val mIsMuteStream: Boolean = true) {
    private val mAudioManager: AudioManager
    private val mAfChangeListener: OnAudioFocusChangeListener
    private var mCurrentVolume = 0
    private var isPausing = false


    init {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mAfChangeListener = OnAudioFocusChangeListener { focusChange -> Log.i("onAudioFocusChange" + focusChange) }
    }


    /**
     * Requests audio focus with the goal of pausing any existing audio player.
     * Additionally mutes the music stream, since some audio players might
     * ignore the focus request.
     * In other words, during the pause no sound will be heard,
     * but whether the audio resumes from the same position after the pause
     * depends on the audio player.
     */
    fun pause() {
        if (!isPausing) {
            val result = mAudioManager.requestAudioFocus(mAfChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.i("AUDIOFOCUS_REQUEST_GRANTED")
            }

            if (mIsMuteStream) {
                mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (mCurrentVolume > 0) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                }
            }
            isPausing = true
        }
    }


    /**
     * Abandons audio focus and restores the audio volume.
     */
    fun resume() {
        if (isPausing) {
            mAudioManager.abandonAudioFocus(mAfChangeListener)
            if (mIsMuteStream && mCurrentVolume > 0) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0)
            }
            isPausing = false
        }
    }

}