package be.planty.android.alexa.interfaces.speechrecognizer

import android.content.Context
import android.text.TextUtils
import android.util.Log

import be.planty.android.alexa.VoiceHelper
import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.interfaces.AvsException
import be.planty.android.alexa.requestbody.DataRequestBody
import com.amazon.identity.auth.device.AuthError

import java.io.IOException

import okhttp3.Call
import okhttp3.RequestBody
import okio.BufferedSink

/**
 * A subclass of [SpeechSendEvent] that allows an arbitrary text string to be sent to the AVS servers, translated through Google's text to speech engine
 * This speech is rendered using the VoiceHelper utility class, and is done on whatever thread this call is running
 */
class SpeechSendText : SpeechSendEvent() {

    internal var start: Long = 0


    override val requestBody: RequestBody
        get() = object : DataRequestBody() {
            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(mOutputStream.toByteArray())
            }
        }

    /**
     * Use VoiceHelper utility to create an audio file from arbitrary text using Text-To-Speech to be passed to the AVS servers
     * @param context local/application context
     * @param url the URL to which we're sending the AVS post
     * @param accessToken our user's access token for the server
     * @param text the text we want to translate into speech
     * @param callback our event callbacks
     * @throws IOException
     */
    @Throws(IOException::class)
    fun sendText(context: Context, url: String, accessToken: String, text: String,
                 callback: AsyncCallback<Call, Exception>?) {
        var text = text

        callback?.start()

        Log.i(TAG, "Starting SpeechSendText procedure")
        start = System.currentTimeMillis()

        //add a pause to the end to be better understood
        if (!TextUtils.isEmpty(text)) {
            text = "... $text ..."
        }

        val input = text


        //call the parent class's prepareConnection() in order to prepare our URL POST
        prepareConnection(url, accessToken)

        //get our VoiceHelper and use an async callback to get the data and send it off to the AVS server via completePost()
        val voiceHelper = VoiceHelper.getInstance(context)
        voiceHelper.getSpeechFromText(input, object : VoiceHelper.SpeechFromTextCallback {
            override fun onSuccess(data: ByteArray) {

                Log.i(TAG, "We have audio")

                try {
                    mOutputStream.write(data)

                    Log.i(TAG, "Audio sent")
                    Log.i(TAG, "Audio creation process took: " + (System.currentTimeMillis() - start))
                    if (callback != null) {
                        callback.success(completePost())
                        callback.complete()
                    }

                } catch (e: IOException) {
                    onError(e)
                } catch (e: AvsException) {
                    onError(e)
                }

            }


            override fun onError(e: Exception) {
                if (callback != null) {
                    callback.failure(AuthError(e.message, e, AuthError.ERROR_TYPE.ERROR_UNKNOWN))
                    callback.complete()
                }
            }
        })

    }

    companion object {

        private val TAG = "SpeechSendText"
    }
}
