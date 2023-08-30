package be.planty.android.alexa.interfaces

import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.connection.ClientUtil

import java.io.ByteArrayOutputStream
import java.io.IOException

import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody

/**
 * An abstract class that supplies a DataOutputStream which is used to send a POST request to the AVS server
 * with a voice data intent, it handles the response with completePost() (called by extending classes)
 */
abstract class SendEvent {

    //the output stream that extending classes will use to pass data to the AVS server
    protected var mOutputStream = ByteArrayOutputStream()
    protected var mCallback: AsyncCallback<Void, Exception>? = null

    private lateinit var currentCall: Call

    //OkHttpClient for transfer of data
    internal var mRequestBuilder = Request.Builder()
    internal lateinit var mBodyBuilder: MultipartBody.Builder

    /**
     * Get our JSON [be.planty.android.alexa.data.Event] for this call
     * @return the JSON representation of the [be.planty.android.alexa.data.Event]
     */
    protected abstract val event: String

    /**
     * Set up all the headers that we need in our OkHttp POST/GET, this prepares the connection for
     * the event or the raw audio that we'll need to pass to the AVS server
     * @param url the URL we're posting to, this is either the default [be.planty.android.alexa.data.Directive] or [be.planty.android.alexa.data.Event] URL
     * @param accessToken the access token of the user who has given consent to the app
     */
    protected fun prepareConnection(url: String, accessToken: String) {

        //set the request URL
        mRequestBuilder.url(url)

        //set our authentication access token header
        mRequestBuilder.addHeader("Authorization", "Bearer " + accessToken)

        val event = event

        mBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata", RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), event))

        //reset our output stream
        mOutputStream = ByteArrayOutputStream()
    }

    /**
     * When finished adding voice data to the output, we close it using completePost() and it is sent off to the AVS server
     * and the response is parsed and returned
     * @return AvsResponse with all the data returned from the server
     * @throws IOException if the OkHttp request can't execute
     * @throws AvsException if we can't parse the response body into an [AvsResponse] item
     * @throws RuntimeException
     */
    @Throws(IOException::class, AvsException::class, RuntimeException::class)
    protected fun completePost(): Call {
        addFormDataParts(mBodyBuilder)
        mRequestBuilder.post(mBodyBuilder.build())
        return parseResponse()
    }

    @Throws(IOException::class, AvsException::class, RuntimeException::class)
    protected fun completeGet(): Call {
        mRequestBuilder.get()
        return parseResponse()
    }

    protected fun cancelCall() {
        if (currentCall != null && !currentCall.isCanceled) {
            currentCall.cancel()
        }
    }

    @Throws(IOException::class, AvsException::class, RuntimeException::class)
    private fun parseResponse(): Call {

        val request = mRequestBuilder.build()


        currentCall = ClientUtil.tlS12OkHttpClient.newCall(request)

        return currentCall
    }


    /**
     * When override, our extending classes can add their own data to the POST
     * @param builder with audio data
     */
    protected open fun addFormDataParts(builder: MultipartBody.Builder) {

    }

    companion object {

        private val TAG = "SendEvent"
    }

}