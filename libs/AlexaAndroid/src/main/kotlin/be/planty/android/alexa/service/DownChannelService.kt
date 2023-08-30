package be.planty.android.alexa.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

import be.planty.android.alexa.AlexaManager
import be.planty.android.alexa.TokenManager
import be.planty.android.alexa.callbacks.ImplAsyncCallback
import be.planty.android.alexa.connection.ClientUtil
import be.planty.android.alexa.data.Event
import be.planty.android.alexa.interfaces.AvsResponse
import be.planty.android.alexa.interfaces.response.ResponseParser
import be.planty.android.alexa.system.AndroidSystemHandler
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ScopeFactory

import org.greenrobot.eventbus.EventBus

import java.io.IOException

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

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
        TokenManager.getAccessToken(
            alexaManager!!.lwaRequestContext, this@DownChannelService,
            object : TokenManager.TokenCallback {
                override fun onSuccess(token: String) {
//        AuthorizationManager.getToken(
//            applicationContext, arrayOf(alexaAllScope),
//            object : Listener<AuthorizeResult, AuthError> {
//                override fun onSuccess(res: AuthorizeResult?) {

                    val downChannelClient = ClientUtil.tlS12OkHttpClient

                    val request = Request.Builder()
                            .url(alexaManager!!.directivesUrl)
                            .get()
                            .addHeader("Authorization", "Bearer " + token)
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

                            response.body()?.source()?.let { bufferedSource ->
                                while (!bufferedSource.exhausted()) {
                                    bufferedSource.readUtf8Line()?.let { line ->
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
                            }
                        }
                    })

                }

                override fun onFailure(e: Throwable) {
//                override fun onError(e: AuthError?) {
                    e.printStackTrace()
                }
        })
    }

    private fun sendHeartbeat() {
        TokenManager.getAccessToken(
            alexaManager!!.lwaRequestContext, this@DownChannelService,
            object : TokenManager.TokenCallback {
                override fun onSuccess(token: String) {
//        AuthorizationManager.getToken(
//            applicationContext, arrayOf(alexaAllScope),
//            object : Listener<AuthorizeResult, AuthError> {
//                override fun onSuccess(res: AuthorizeResult?) {

                    Log.i(TAG, "Sending heartbeat")
                    val request = Request.Builder()
                            .url(alexaManager!!.pingUrl)
                            .get()
                            .addHeader("Authorization", "Bearer " + token)
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
//                override fun onError(e: AuthError?) {
                }
        })
    }

    companion object {
        private val TAG = DownChannelService::class.java.simpleName
    }
}
