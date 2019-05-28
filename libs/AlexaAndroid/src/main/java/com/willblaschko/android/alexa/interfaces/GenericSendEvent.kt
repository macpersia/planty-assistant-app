package com.willblaschko.android.alexa.interfaces

import android.util.Log

import com.willblaschko.android.alexa.callbacks.AsyncCallback

import java.io.IOException

import okhttp3.Call

/**
 * @author will on 5/21/2016.
 */
class GenericSendEvent(url: String, accessToken: String, event: String,
                       callback: AsyncCallback<Call, Exception>?) : SendEvent() {

    override lateinit var event: String
        private set

    init {
        this.event = event

        callback?.start()
        try {
            prepareConnection(url, accessToken)
            if (callback != null) {
                callback.success(completePost())
                callback.complete()
            } else {
                completePost()
            }
            Log.i(TAG, "Event sent")
        } catch (e: IOException) {
            onError(callback, e)
        } catch (e: AvsException) {
            onError(callback, e)
        }

    }


    fun onError(callback: AsyncCallback<Call, Exception>?, e: Exception) {
        if (callback != null) {
            callback.failure(e)
            callback.complete()
        }
    }

    companion object {

        val TAG = "GenericSendEvent"
    }
}
