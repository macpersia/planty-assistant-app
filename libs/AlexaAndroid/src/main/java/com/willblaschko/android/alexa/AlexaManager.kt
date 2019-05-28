package com.willblaschko.android.alexa

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import com.willblaschko.android.alexa.AuthorizationManager.Companion.createCodeVerifier
import com.willblaschko.android.alexa.callbacks.AsyncCallback
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback
import com.willblaschko.android.alexa.data.Event
import com.willblaschko.android.alexa.interfaces.AvsException
import com.willblaschko.android.alexa.interfaces.AvsItem
import com.willblaschko.android.alexa.interfaces.AvsResponse
import com.willblaschko.android.alexa.interfaces.GenericSendEvent
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem
import com.willblaschko.android.alexa.interfaces.response.ResponseParser
import com.willblaschko.android.alexa.interfaces.response.ResponseParser.getBoundary
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendAudio
import com.willblaschko.android.alexa.interfaces.speechrecognizer.SpeechSendText
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem
import com.willblaschko.android.alexa.requestbody.DataRequestBody
import com.willblaschko.android.alexa.service.DownChannelService
import com.willblaschko.android.alexa.system.AndroidSystemHandler
import com.willblaschko.android.alexa.utility.Util
import com.willblaschko.android.alexa.utility.Util.IDENTIFIER
import okhttp3.Call
import okio.BufferedSink
import java.io.IOException
import java.net.HttpURLConnection

/**
 * The overarching instance that handles all the state when requesting intents to the Alexa Voice Service servers, it creates all the required instances and confirms that users are logged in
 * and authenticated before allowing them to send intents.
 *
 * Beyond initialization, mostly it supplies wrapped helper functions to the other classes to assure authentication state.
 */
class AlexaManager private constructor(context: Context, productId: String?) {
    val authorizationManager: AuthorizationManager
    private var mSpeechSendAudio: SpeechSendAudio? = null
    private var mSpeechSendText: SpeechSendText? = null
    private lateinit var mVoiceHelper: VoiceHelper
    var urlEndpoint: String? = null
        set(url) {
            field = url
            Util.getPreferences(mContext)
                    .edit()
                    .putString(KEY_URL_ENDPOINT, url)
                    .apply()
        }

    private val mContext: Context
    /**
     * Helper function to check if we're currently recording
     * @return
     */
    val isRecording = false


    val speechSendText: SpeechSendText
        get() {
            if (mSpeechSendText == null) {
                mSpeechSendText = SpeechSendText()
            }
            return mSpeechSendText!!
        }

    val speechSendAudio: SpeechSendAudio
        get() {
            if (mSpeechSendAudio == null) {
                mSpeechSendAudio = SpeechSendAudio()
            }
            return mSpeechSendAudio!!
        }

    val voiceHelper: VoiceHelper
        get() {
            if (!::mVoiceHelper.isInitialized) {
                mVoiceHelper = VoiceHelper.getInstance(mContext)
            }
            return mVoiceHelper
        }

    val pingUrl: String
        get() = StringBuilder()
                .append(urlEndpoint)
                .append("/ping")
                .toString()

    val eventsUrl: String
        get() = StringBuilder()
                .append(urlEndpoint)
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("events")
                .toString()

    val directivesUrl: String
        get() = StringBuilder()
                .append(urlEndpoint)
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("directives")
                .toString()

    init {
        var productId = productId
        mContext = context.applicationContext
        if (productId == null) {
            productId = context.getString(R.string.alexa_product_id)
        }
        urlEndpoint = Util.getPreferences(context).getString(KEY_URL_ENDPOINT, context.getString(R.string.alexa_api))

        authorizationManager = AuthorizationManager(mContext, productId!!)
        mAndroidSystemHandler = AndroidSystemHandler.getInstance(context)
        val stickyIntent = Intent(context, DownChannelService::class.java)
        context.startService(stickyIntent)

        if (!Util.getPreferences(mContext).contains(IDENTIFIER)) {
            Util.getPreferences(mContext)
                    .edit()
                    .putString(IDENTIFIER, createCodeVerifier(30))
                    .apply()
        }
    }

    /**
     * Check if the user is logged in to the Amazon service, uses an async callback with a boolean to return response
     * @param callback state callback
     */
    fun checkLoggedIn(callback: AsyncCallback<Boolean, Throwable>) {
        authorizationManager.checkLoggedIn(mContext, object : AsyncCallback<Boolean, Throwable> {
            override fun start() {}
            override fun success(result: Boolean) = callback.success(result)
            override fun failure(error: Throwable) = callback.failure(error)
            override fun complete() {}
        })
    }

    /**
     * Send a log in request to the Amazon Authentication Manager
     * @param callback state callback
     */
    fun logIn(callback: AuthorizationCallback?, force: Boolean = false) {
        //check if we're already logged in
        if (force)
            authorizationManager.authorizeUser(callback)
        else
            authorizationManager.checkLoggedIn(mContext, object : AsyncCallback<Boolean, Throwable> {
                override fun start() {}
                override fun success(result: Boolean) {
                    //if we are, return a success
                    if (callback != null)
                        if (result) {
                            callback.onSuccess()
                        } else {
                            //otherwise start the authorization process
                            authorizationManager.authorizeUser(callback)
                        }
                }
                override fun failure(error: Throwable) {
                    callback?.onError(Exception(error))
                }

                override fun complete() {

                }
            })
    }


    /**
     * Send a synchronize state [Event] request to Alexa Servers to retrieve pending [com.willblaschko.android.alexa.data.Directive]
     * See: [.sendEvent]
     * @param callback state callback
     */
    fun sendSynchronizeStateEvent(callback: AsyncCallback<AvsResponse, Exception?>?) {
        sendEvent(Event.synchronizeStateEvent, callback)
    }


    /**
     * Send a text string request to the AVS server, this is run through Text-To-Speech to create the raw audio file needed by the AVS server.
     *
     * This allows the developer to pre/post-pend or send any arbitrary text to the server, versus the startRecording()/stopRecording() combination which
     * expects input from the user. This operation, because of the extra steps is generally slower than the above option.
     *
     * @param text the arbitrary text that we want to send to the AVS server
     * @param callback the state change callback
     */
    fun sendTextRequest(text: String, callback: AsyncCallback<AvsResponse, Exception?>?) {
        //check if the user is already logged in
        authorizationManager.checkLoggedIn(mContext, object : ImplCheckLoggedInCallback() {
            override fun success(result: Boolean) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    val url = eventsUrl
                    //do this off the main thread
                    object : AsyncTask<Void, Void, AvsResponse?>() {
                        override fun doInBackground(vararg params: Void): AvsResponse? {
                            //get our access token
                            TokenManager.getAccessToken(authorizationManager.amazonAuthorizationManager, mContext, object : TokenManager.TokenCallback {
                                override fun onSuccess(token: String) {
                                    try {
                                        speechSendText.sendText(mContext, url, token, text, AsyncEventHandler(this@AlexaManager, callback))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        //bubble up the error
                                        callback?.failure(e)
                                    }
                                }

                                override fun onFailure(e: Throwable) {

                                }
                            })
                            return null
                        }

                        override fun onPostExecute(avsResponse: AvsResponse?) {
                            super.onPostExecute(avsResponse)
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(object : ImplAuthorizationCallback<AvsResponse>(callback) {

                        override fun onSuccess() {
                            //call our function again
                            sendTextRequest(text, callback)
                        }

                    })
                }
            }

        })
    }


    /**
     * Send raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param data the audio data that we want to send to the AVS server
     * @param callback the state change callback
     */
    fun sendAudioRequest(data: ByteArray, callback: AsyncCallback<AvsResponse, Exception?>?) {
        sendAudioRequest(object : DataRequestBody() {
            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(data)
            }
        }, callback)
    }

    /**
     * Send streamed raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param requestBody a request body that incorporates either a static byte[] write to the BufferedSink or a streamed, managed byte[] data source
     * @param callback the state change callback
     */
    fun sendAudioRequest(requestBody: DataRequestBody, callback: AsyncCallback<AvsResponse, Exception?>?) {
        //check if the user is already logged in
        authorizationManager.checkLoggedIn(mContext, object : ImplCheckLoggedInCallback() {

            override fun success(result: Boolean) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    val url = eventsUrl
                    //get our access token
                    TokenManager.getAccessToken(authorizationManager.amazonAuthorizationManager!!, mContext, object : TokenManager.TokenCallback {
                        override fun onSuccess(token: String) {
                            //do this off the main thread
                            object : AsyncTask<Void, Void, AvsResponse?>() {
                                override fun doInBackground(vararg params: Void): AvsResponse? {
                                    try {
                                        speechSendAudio.sendAudio(url, token, requestBody, AsyncEventHandler(this@AlexaManager, callback))
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                        //bubble up the error
                                        callback?.failure(e)
                                    }
                                    return null
                                }
                                override fun onPostExecute(avsResponse: AvsResponse?) {
                                    super.onPostExecute(avsResponse)
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                        }
                        override fun onFailure(e: Throwable) {
                            Log.e(TAG, e.message, e)
                        }
                    })
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(object : ImplAuthorizationCallback<AvsResponse>(callback) {
                        override fun onSuccess() {
                            //call our function again
                            sendAudioRequest(requestBody, callback)
                        }
                    })
                }
            }

        })
    }

    fun cancelAudioRequest() {
        //check if the user is already logged in
        authorizationManager.checkLoggedIn(mContext, object : ImplCheckLoggedInCallback() {
            override fun success(result: Boolean) {
                if (result) {
                    //if the user is logged in
                    speechSendAudio.cancelRequest()
                }
            }

        })
    }

    /** Send a confirmation to the Alexa server that the device volume has been changed in response to a directive
     * See: [.sendEvent]
     *
     * @param volume volume as reported by the [com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem] Directive
     * @param isMute report whether the device is currently muted
     * @param callback state callback
     */
    fun sendVolumeChangedEvent(volume: Long, isMute: Boolean, callback: AsyncCallback<AvsResponse, Exception?>?) {
        sendEvent(Event.getVolumeChangedEvent(volume, isMute), callback)
    }


    /** Send a confirmation to the Alexa server that the mute state has been changed in response to a directive
     * See: [.sendEvent]
     *
     * @param isMute mute state as reported by the [com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem] Directive
     * @param callback
     */
    fun sendMutedEvent(isMute: Boolean, callback: AsyncCallback<AvsResponse, Exception?>?) {
        sendEvent(Event.getMuteEvent(isMute), callback)
    }

    /**
     * Send confirmation that the device has timed out without receiving a speech request when expected
     * See: [.sendEvent]
     *
     * @param callback
     */
    fun sendExpectSpeechTimeoutEvent(callback: AsyncCallback<AvsResponse, Exception?>) {
        sendEvent(Event.expectSpeechTimedOutEvent, callback)
    }


    /**
     * Send an event to indicate that playback of a speech item has started
     * See: [.sendEvent]
     *
     * @param item our speak item
     * @param callback
     */
    fun sendPlaybackStartedEvent(item: AvsItem?, milliseconds: Long, callback: AsyncCallback<AvsResponse, Exception?>?) {
        if (item == null) {
            return
        }
        val event: String
        if (isAudioPlayItem(item)) {
            event = Event.getPlaybackStartedEvent(item.token, milliseconds)
        } else {
            event = Event.getSpeechStartedEvent(item.token)
        }

        sendEvent(event, callback)
    }

    /**
     * Send an event to indicate that playback of a speech item has finished
     * See: [.sendEvent]
     *
     * @param item our speak item
     * @param callback
     */
    fun sendPlaybackFinishedEvent(item: AvsItem?, callback: AsyncCallback<AvsResponse, Exception?>?) {
        if (item == null) {
            return
        }
        val event: String
        if (isAudioPlayItem(item)) {
            event = Event.getPlaybackFinishedEvent(item.token)
        } else {
            event = Event.getSpeechFinishedEvent(item.token)
        }
        sendEvent(event, callback)
    }


    /**
     * Send an event to indicate that playback of an item has nearly finished
     *
     * @param item our speak/playback item
     * @param callback
     */
    fun sendPlaybackNearlyFinishedEvent(item: AvsPlayAudioItem?, milliseconds: Long, callback: AsyncCallback<AvsResponse, Exception?>?) {
        if (item == null) {
            return
        }
        val event = Event.getPlaybackNearlyFinishedEvent(item.token, milliseconds)

        sendEvent(event, callback)
    }

    /**
     * Send a generic event to the AVS server, this is generated using [com.willblaschko.android.alexa.data.Event.Builder]
     * @param event the string JSON event
     * @param callback
     */
    fun sendEvent(event: String, callback: AsyncCallback<AvsResponse, Exception?>?) {
        //check if the user is already logged in
        authorizationManager.checkLoggedIn(mContext, object : ImplCheckLoggedInCallback() {

            override fun success(result: Boolean) {
                if (result) {
                    //if the user is logged in

                    //set our URL
                    val url = eventsUrl
                    //get our access token
                    TokenManager.getAccessToken(authorizationManager.amazonAuthorizationManager, mContext, object : TokenManager.TokenCallback {
                        override fun onSuccess(token: String) {
                            //do this off the main thread
                            object : AsyncTask<Void, Void, AvsResponse?>() {
                                override fun doInBackground(vararg params: Void): AvsResponse? {
                                    Log.i(TAG, event)
                                    GenericSendEvent(url, token, event, AsyncEventHandler(this@AlexaManager, callback))
                                    return null
                                }

                                override fun onPostExecute(avsResponse: AvsResponse?) {
                                    super.onPostExecute(avsResponse)
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                        }

                        override fun onFailure(e: Throwable) {

                        }
                    })
                } else {
                    //if the user is not logged in, log them in and then call the function again
                    logIn(object : ImplAuthorizationCallback<AvsResponse>(callback) {
                        override fun onSuccess() {
                            //call our function again
                            sendEvent(event, callback)
                        }
                    })
                }
            }

        })
    }

    private fun isAudioPlayItem(item: AvsItem?): Boolean {
        return item != null && (item is AvsPlayAudioItem || item !is AvsSpeakItem)
    }


    private class AsyncEventHandler(internal var manager: AlexaManager, internal var callback: AsyncCallback<AvsResponse, Exception?>?) : AsyncCallback<Call, Exception> {

        override fun start() {
            if (callback != null) {
                callback!!.start()
            }
        }

        override fun success(currentCall: Call) {
            try {
                val response = currentCall.execute()

                if (response.code() == HttpURLConnection.HTTP_NO_CONTENT) {
                    Log.w(TAG, "Received a 204 response code from Amazon, is this expected?")
                }

                val items = if (response.code() == HttpURLConnection.HTTP_NO_CONTENT)
                    AvsResponse()
                else
                    ResponseParser.parseResponse(response.body().byteStream(), getBoundary(response))

                response.body().close()

                mAndroidSystemHandler.handleItems(items)

                if (callback != null) {
                    callback!!.success(items)
                }
            } catch (e: IOException) {
                if (!currentCall.isCanceled) {
                    if (callback != null) {
                        callback!!.failure(e)
                    }
                }
            } catch (e: AvsException) {
                if (!currentCall.isCanceled) {
                    if (callback != null) {
                        callback!!.failure(e)
                    }
                }
            }

        }

        override fun failure(error: Exception) {
            //bubble up the error
            if (callback != null) {
                callback!!.failure(error)
            }
        }

        override fun complete() {
            if (callback != null) {
                callback!!.complete()
            }
            manager.mSpeechSendAudio = null
            manager.mSpeechSendText = null
        }
    }

    private abstract class ImplAuthorizationCallback<E>(internal var callback: AsyncCallback<E, Exception?>?) : AuthorizationCallback {

        override fun onCancel() {

        }

        override fun onError(error: Exception) {
            if (callback != null) {
                //bubble up the error
                callback!!.failure(error)
            }
        }
    }

    private abstract class ImplCheckLoggedInCallback : AsyncCallback<Boolean, Throwable> {

        override fun start() {

        }


        override fun failure(error: Throwable) {

        }

        override fun complete() {

        }
    }

    companion object {

        private val TAG = "AlexaManager"
        private val KEY_URL_ENDPOINT = "url_endpoint"

        private var mInstance: AlexaManager? = null
        private lateinit var mAndroidSystemHandler: AndroidSystemHandler

        /**
         * Return an instance of AlexaManager
         *
         * @param context application context
         * @return AlexaManager instance
         */
        fun getInstance(context: Context): AlexaManager {
            return getInstance(context, context.applicationInfo.packageName)
        }

        /**
         * Return an instance of AlexaManager
         *
         * Deprecated: use @getInstance(Context) instead and set R.string.alexa_product_id in your application resources,
         * this change was made to properly support the DownChannelService
         *
         * @param context application context
         * @param productId AVS product id
         * @return AlexaManager instance
         */
        fun getInstance(context: Context, productId: String?): AlexaManager {
            if (mInstance == null) {
                mInstance = AlexaManager(context, productId)
            }
            return mInstance!!
        }
    }
}
