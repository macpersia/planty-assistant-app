package com.willblaschko.android.alexa.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

import com.willblaschko.android.alexa.AlexaManager
import com.willblaschko.android.alexa.TokenManager
import com.willblaschko.android.alexa.callbacks.ImplAsyncCallback
import com.willblaschko.android.alexa.connection.ClientUtil
import com.willblaschko.android.alexa.data.Directive
import com.willblaschko.android.alexa.data.Event
import com.willblaschko.android.alexa.interfaces.AvsItem
import com.willblaschko.android.alexa.interfaces.AvsResponse
import com.willblaschko.android.alexa.interfaces.response.ResponseParser
import com.willblaschko.android.alexa.system.AndroidSystemHandler

import org.greenrobot.eventbus.EventBus

import java.io.IOException

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource

/**
 * @author will on 4/27/2016.
 */
class DownChannelService : Service() {

    private var alexaManager: AlexaManager? = null
    private var currentCall: Call? = null
    private var handler: AndroidSystemHandler? = null


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Launched")
        alexaManager = AlexaManager.getInstance(this)
        handler = AndroidSystemHandler.getInstance(this)

        openDownChannel()

    }


    override fun onDestroy() {
        super.onDestroy()
        if (currentCall != null) {
            currentCall!!.cancel()
        }
    }


    private fun openDownChannel() {
        TokenManager.getAccessToken(alexaManager!!.authorizationManager.amazonAuthorizationManager!!, this@DownChannelService, object : TokenManager.TokenCallback {
            override fun onSuccess(token: String) {

                val downChannelClient = ClientUtil.tlS12OkHttpClient

                val request = Request.Builder()
                        .url(alexaManager!!.directivesUrl)
                        .get()
                        .addHeader("Authorization", "Bearer " + token!!)
                        .build()

                currentCall = downChannelClient.newCall(request)
                currentCall!!.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {

                        alexaManager!!.sendEvent(Event.synchronizeStateEvent, object : ImplAsyncCallback<AvsResponse, Exception?>() {
                            override fun success(result: AvsResponse) {
                                handler!!.handleItems(result)
                                sendHeartbeat()
                            }
                        })

                        val bufferedSource = response.body().source()

                        while (!bufferedSource.exhausted()) {
                            val line = bufferedSource.readUtf8Line()
                            try {
                                val directive = ResponseParser.getDirective(line)
                                handler!!.handleDirective(directive)

                                //surface to our UI if it's up
                                val item = ResponseParser.parseDirective(directive)
                                EventBus.getDefault().post(item)

                            } catch (e: Exception) {
                                Log.e(TAG, "Bad line: ${e.message}")
                            }

                        }

                    }
                })

            }

            override fun onFailure(e: Throwable) {
                e.printStackTrace()
            }
        })
    }

    private fun sendHeartbeat() {
        TokenManager.getAccessToken(alexaManager!!.authorizationManager.amazonAuthorizationManager!!, this@DownChannelService, object : TokenManager.TokenCallback {
            override fun onSuccess(token: String) {

                Log.i(TAG, "Sending heartbeat")
                val request = Request.Builder()
                        .url(alexaManager!!.pingUrl)
                        .get()
                        .addHeader("Authorization", "Bearer " + token!!)
                        .build()

                ClientUtil.tlS12OkHttpClient
                        .newCall(request)
                        .enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {

                            }

                            @Throws(IOException::class)
                            override fun onResponse(call: Call, response: Response) {

                            }
                        })

                Handler(Looper.getMainLooper()).postDelayed({ sendHeartbeat() }, (4 * 60 * 1000).toLong())
            }

            override fun onFailure(e: Throwable) {

            }
        })
    }

    companion object {
        private val TAG = DownChannelService.javaClass.simpleName
    }
}
