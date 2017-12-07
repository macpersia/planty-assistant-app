package ee.ioc.phon.android.speechutils

import android.media.MediaRecorder

interface AudioRecorder {

    val wsArgs: String

    val state: State

    val rmsdb: Float

    val isPausing: Boolean

    fun consumeRecordingAndTruncate(): ByteArray

    fun consumeRecording(): ByteArray

    fun start()

    fun release()

    enum class State {
        // recorder is ready, but not yet recording
        READY,

        // recorder recording
        RECORDING,

        // error occurred, reconstruction needed
        ERROR,

        // recorder stopped
        STOPPED
    }

    companion object {
        val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
        val DEFAULT_SAMPLE_RATE = 16000
        val RESOLUTION_IN_BYTES: Short = 2
        // Number of channels (MONO = 1, STEREO = 2)
        val CHANNELS: Short = 1
    }
}