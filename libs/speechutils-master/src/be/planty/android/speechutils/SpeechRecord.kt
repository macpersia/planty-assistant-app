package be.planty.android.speechutils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build

/**
 * The following takes effect only on Jelly Bean and higher.
 *
 * @author Kaarel Kaljurand
 */
class SpeechRecord
    @Throws(IllegalArgumentException::class)
    @JvmOverloads
    constructor(audioSource: Int, sampleRateInHz: Int, channelConfig: Int, audioFormat: Int,
                bufferSizeInBytes: Int, noise: Boolean = false, gain: Boolean = false, echo: Boolean = false)
    : AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes) {

    @Throws(IllegalArgumentException::class)
    @JvmOverloads
    constructor(sampleRateInHz: Int, bufferSizeInBytes: Int)
    : this(MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRateInHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            false,
            false,
            false)

    @Throws(IllegalArgumentException::class)
    @JvmOverloads
    constructor(sampleRateInHz: Int, bufferSizeInBytes: Int, noise: Boolean, gain: Boolean, echo: Boolean)
    : this(MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRateInHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            noise,
            gain,
            echo)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Log.i("Trying to enhance audio because running on SDK " + Build.VERSION.SDK_INT)
            val audioSessionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                audioSessionId
            } else {
                TODO("VERSION.SDK_INT < JELLY_BEAN")
            }
            if (noise) {
                if (NoiseSuppressor.create(audioSessionId) == null) {
                    Log.i("NoiseSuppressor: failed")
                } else {
                    Log.i("NoiseSuppressor: ON")
                }
            } else {
                Log.i("NoiseSuppressor: OFF")
            }

            if (gain) {
                if (AutomaticGainControl.create(audioSessionId) == null) {
                    Log.i("AutomaticGainControl: failed")
                } else {
                    Log.i("AutomaticGainControl: ON")
                }
            } else {
                Log.i("AutomaticGainControl: OFF")
            }

            if (echo) {
                if (AcousticEchoCanceler.create(audioSessionId) == null) {
                    Log.i("AcousticEchoCanceler: failed")
                } else {
                    Log.i("AcousticEchoCanceler: ON")
                }
            } else {
                Log.i("AcousticEchoCanceler: OFF")
            }
        }
    }

    companion object {
        val isNoiseSuppressorAvailable: Boolean
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                NoiseSuppressor.isAvailable()
            } else false
    }
}