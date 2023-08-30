package be.planty.android.alexa.interfaces.speechrecognizer

import android.util.Log

import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.interfaces.AvsException
import be.planty.android.alexa.requestbody.DataRequestBody
import com.amazon.identity.auth.device.AuthError

import java.io.IOException

import okhttp3.Call

/**
 * A subclass of [SpeechSendEvent] that sends a RequestBody to the AVS servers, this request body can either be a byte[]
 * straight write, or a threaded write loop based on incoming data (recorded audio).
 *
 * @author will on 4/17/2016.
 */
class SpeechSendAudio : SpeechSendEvent() {

    internal var start: Long = 0
    override lateinit var requestBody: DataRequestBody

    /**
     * Post an audio byte[] to the Alexa Speech Recognizer API
     * @param url the URL to which we're sending the AVS post
     * @param accessToken our user's access token for the server
     * @param requestBody our OkHttp RequestBody for our mulitpart send, this request body can either be a byte[]
     * straight write, or a threaded write loop based on incoming data (recorded audio).
     * @param callback our event callbacks
     * @throws IOException
     */
    @Throws(IOException::class)
    fun sendAudio(url: String, accessToken: String, requestBody: DataRequestBody,
                  callback: AsyncCallback<Call, Exception>?) {
        this.requestBody = requestBody
        callback?.start()
        Log.i(TAG, "Starting SpeechSendAudio procedure")
        start = System.currentTimeMillis()

        //call the parent class's prepareConnection() in order to prepare our URL POST
        try {
            prepareConnection(url, accessToken)
            val response = completePost()

            if (callback != null) {
                if (response != null) {
                    callback.success(response)
                }
                callback.complete()
            }

            Log.i(TAG, "Audio sent")
            Log.i(TAG, "Audio sending process took: " + (System.currentTimeMillis() - start))
        } catch (e: IOException) {
            onError(callback, e)
        } catch (e: AvsException) {
            onError(callback, e)
        }

    }

    fun cancelRequest() {
        cancelCall()
    }

    fun onError(callback: AsyncCallback<Call, Exception>?, e: Exception) {
        if (callback != null) {
            callback.failure(AuthError(e.message, e, AuthError.ERROR_TYPE.ERROR_UNKNOWN))
            callback.complete()
        }
    }

    companion object {
        private val TAG = "SpeechSendAudio"
    }
}
