package be.planty.android.alexa.interfaces.speechrecognizer

import android.Manifest.permission.RECORD_AUDIO
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.interfaces.AvsException
import be.planty.android.alexa.requestbody.DataRequestBody
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.AuthError.ERROR_TYPE
import okhttp3.Call
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CompletableFuture.runAsync

/**
 * A subclass of [SpeechSendEvent] that sends a recorded raw audio to the AVS server, unlike [SpeechSendText], this does not use an intermediary steps, but the starting/stopping
 * need to be handled manually or programmatically, which Google's voice offering already does automatically (listens for speech thresholds).
 *
 * This is the class that should be used, ideally, instead of SpeechSendText, but it offers a little less flexibility as a standalone.
 *
 * Using the byte[] buffer in startRecording(), it's possible to prepend any audio recorded with pre-recorded or generated audio, in order to simplify or complicate the command.
 *
 */
@Deprecated("Use {@link SpeechSendAudio} instead, either with a byte[] or using the streamed RequestBody")
class SpeechSendVoice : SpeechSendEvent() {

    private lateinit var mAudioRecord: AudioRecord

    private var mIsRecording = false

    private val mLock = Any()


    override val requestBody: RequestBody
        get() = object : DataRequestBody() {
            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(mOutputStream.toByteArray())
            }
        }

    /**
     * The trigger to open a new AudioRecord and start recording with the intention of sending the audio to the AVS server using the stopRecord(). This will have permissions
     * issues in Marshmallow that need to be handled at the Activity level (checking for permission to record audio, and requesting it if we don't already have permissions).
     * @param url our POST url
     * @param accessToken our user's access token
     * @param buffer a byte[] that allows us to prepend whatever audio is recorded by the user with either generated ore pre-recorded audio, this needs
     * to be in the same format as the audio being recorded
     * @param callback our callback to notify us when we change states
     * @throws IOException
     *
     */
    @RequiresPermission(RECORD_AUDIO)
    @Deprecated("Manage this state on the application side, instead, and send the audio using {@link SpeechSendAudio}")
    @Throws(IOException::class)
    fun startRecording(url: String, accessToken: String, buffer: ByteArray?,
                       callback: AsyncCallback<Void, Exception>?) {
        synchronized(mLock) {
            mAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE)
        }

        callback?.start()

        mCallback = callback
        mIsRecording = true
        runAsync {
            synchronized(mLock) {
                prepareConnection(url, accessToken)
            }
        }

        if (buffer != null) {
            mOutputStream.write(buffer)
        }

        //record our audio
        recordAudio(mAudioRecord, mOutputStream)
    }

    /**
     * Stop recording the user's audio and send the request to the AVS server, parse the response
     * @return the parsed response from the AVS server based on the recorded audio
     * @throws IOException
     * @throws AvsException
     *
     */
    @Deprecated("Manage this state on the application side, instead, and send the audio using {@link SpeechSendAudio}")
    @Throws(IOException::class, AvsException::class)
    fun stopRecording(): Call {
        mIsRecording = false
        synchronized(mLock) {
            if (mAudioRecord != null) {
                mAudioRecord.stop()
                mAudioRecord.release()
            }
        }
        return completePost()
    }


    /**
     * Record audio using AudioRecord and our outputStream, but do it using a thread so we don't lock up whatever
     * thread the function was called on
     * @param audioRecord our AudioRecord native
     * @param outputStream our HttpURLConnection outputstream (from SendEvent class)
     *
     */

    @Deprecated("Manage this state on the application side, instead, and send the audio using {@link SpeechSendAudio}")
    private fun recordAudio(audioRecord: AudioRecord, outputStream: OutputStream?) {
        audioRecord.startRecording()
        if (outputStream == null) {
            mIsRecording = false
            return
        }

        val recordingThread = Thread(Runnable {
            val data = ByteArray(BUFFER_SIZE)
            while (mIsRecording) {
                var count: Int = synchronized(mLock) {
                    audioRecord.read(data, 0, BUFFER_SIZE)
                }
                if (count <= 0) {
                    Log.e(TAG, "audio read fail, error code:" + count)
                    mIsRecording = false
                    if (mCallback != null) {
                        mCallback!!.failure(AuthError(
                            "audio read fail, error code:" + count,
                            ERROR_TYPE.ERROR_UNKNOWN))
                    }
                    break
                }
                try {
                    outputStream.write(data, 0, count)
                    outputStream.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                    mIsRecording = false
                    if (mCallback != null) {
                        mCallback!!.failure(AuthError(e.message, e, ERROR_TYPE.ERROR_IO))
                        mCallback!!.complete()
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    mIsRecording = false
                    if (mCallback != null) {
                        mCallback!!.failure(AuthError(e.message, e, ERROR_TYPE.ERROR_UNKNOWN))
                        mCallback!!.complete()
                    }
                }

            }
        }, "AudioRecorder Thread")
        recordingThread.start()
    }

    companion object {
        private val TAG = "SpeechSendVoice"

        private val AUDIO_RATE = 16000
        private val BUFFER_SIZE = 800
    }

}
