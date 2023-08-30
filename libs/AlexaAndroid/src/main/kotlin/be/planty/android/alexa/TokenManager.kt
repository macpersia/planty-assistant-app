package be.planty.android.alexa

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.planty.android.alexa.connection.ClientUtil
import be.planty.android.alexa.utility.Util
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.amazon.identity.auth.device.appid.AppIdentifier
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AppIdentifierHelper
import com.amazon.identity.auth.device.dataobject.AppInfo
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

/**
 * A utility class designed to request, receive, store, and renew Amazon authentication tokens using a Volley interface and the Amazon auth API
 *
 * Some more details here: https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/docs/authorizing-your-alexa-enabled-product-from-a-website
 */
object TokenManager {

    private val TAG = "TokenManager"

    private var REFRESH_TOKEN: String? = null
    private var ACCESS_TOKEN: String? = null

    private val ARG_GRANT_TYPE = "grant_type"
    private val ARG_CODE = "code"
    private val ARG_REDIRECT_URI = "redirect_uri"
    private val ARG_CLIENT_ID = "client_id"
    private val ARG_CODE_VERIFIER = "code_verifier"
    private val ARG_REFRESH_TOKEN = "refresh_token"


    val PREF_ACCESS_TOKEN = "access_token_042017"
    val PREF_REFRESH_TOKEN = "refresh_token_042017"
    val PREF_TOKEN_EXPIRES = "token_expires_042017"

    /**
     * Get an access token from the Amazon servers for the current user
     * @param context local/application level context
     * @param authCode the authorization code supplied by the Authorization Manager
     * @param codeVerifier a randomly generated verifier, must be the same every time
     * @param authorizationManager the AuthorizationManager class calling this function
     * @param callback the callback for state changes
     */
    fun getAccessToken(context: Context, authCode: String, codeVerifier: String?,
                       authorizationManager: AmazonAuthorizationManager,
                        //lwaRequestContext: RequestContext,
                       callback: TokenResponseCallback?) {
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        val url = "https://api.amazon.com/auth/O2/token"

        //set up our arguments for the api call, these will be the call headers
        val builder = FormBody.Builder()
                .add(ARG_GRANT_TYPE, "authorization_code")
                .add(ARG_CODE, authCode)
        try {
            builder.add(ARG_REDIRECT_URI, authorizationManager.redirectUri)
            builder.add(ARG_CLIENT_ID, authorizationManager.clientId)
//            val appInfo = AppIdentifierHelper.getAppInfo(context.packageName, context)
//            builder.add(ARG_REDIRECT_URI, appInfo.redirectUri)
//            builder.add(ARG_CLIENT_ID, appInfo.clientId)
        } catch (authError: AuthError) {
            authError.printStackTrace()
        }

        builder.add(ARG_CODE_VERIFIER, codeVerifier)

        val client = ClientUtil.tlS12OkHttpClient

        val request = Request.Builder()
                .url(url)
                .post(builder.build())
                .build()

        val handler = Handler(Looper.getMainLooper())


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                if (callback != null) {
                    //bubble up error
                    handler.post { callback.onFailure(e) }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val s = response.body()?.string()
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "$s")
                }
                val tokenResponse = Gson().fromJson(s, TokenResponse::class.java)
                //save our tokens to local shared preferences
                saveTokens(context, tokenResponse)

                if (callback != null) {
                    //bubble up success
                    handler.post { callback.onSuccess(tokenResponse) }
                }
            }
        })

    }

    /**
     * Check if we have a pre-existing access token, and whether that token is expired. If it is not, return that token, otherwise get a refresh token and then
     * use that to get a new token.
     * @param authorizationManager our AuthManager
     * @param context local/application context
     * @param callback the TokenCallback where we return our tokens when successful
     */
    fun getAccessToken(lwaRequestContext: RequestContext, context: Context, callback: TokenCallback) {
        val preferences = Util.getPreferences(context.applicationContext)
        //if we have an access token
        if (preferences.contains(PREF_ACCESS_TOKEN)) {

            if (preferences.getLong(PREF_TOKEN_EXPIRES, 0) > System.currentTimeMillis()) {
                //if it's not expired, return the existing token
                callback.onSuccess(preferences.getString(PREF_ACCESS_TOKEN, null)!!)
                return
            } else {
                //if it is expired but we have a refresh token, get a new token
                if (preferences.contains(PREF_REFRESH_TOKEN)) {
                    getRefreshToken(lwaRequestContext, context, callback, preferences.getString(PREF_REFRESH_TOKEN, ""))
                    return
                }
            }
        }

        //uh oh, the user isn't logged in, we have an IllegalStateException going on!
        callback.onFailure(IllegalStateException("User is not logged in and no refresh token found."))
    }

    /**
     * Get a new refresh token from the Amazon server to replace the expired access token that we currently have
     * @param authorizationManager
     * @param context
     * @param callback
     * @param refreshToken the refresh token we have stored in local cache (sharedPreferences)
     */
    private fun getRefreshToken(lwaRequestContext: RequestContext, context: Context, callback: TokenCallback, refreshToken: String?) {
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        val url = "https://api.amazon.com/auth/O2/token"


        //set up our arguments for the api call, these will be the call headers
        val builder = FormBody.Builder()
                .add(ARG_GRANT_TYPE, "refresh_token")
                .add(ARG_REFRESH_TOKEN, refreshToken!!)
        val appInfo = AppIdentifierHelper.getAppInfo(context.packageName, context)
        builder.add(ARG_CLIENT_ID, appInfo.clientId)


        val client = ClientUtil.tlS12OkHttpClient

        val request = Request.Builder()
                .url(url)
                .post(builder.build())
                .build()

        val handler = Handler(Looper.getMainLooper())

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                if (callback != null) {
                    //bubble up error
                    handler.post { callback.onFailure(e) }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val s = response.body()?.string()
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "$s")
                }

                //get our tokens back
                val tokenResponse = Gson().fromJson(s, TokenResponse::class.java)
                //save our tokens
                saveTokens(context, tokenResponse)
                //we have new tokens!
                handler.post { callback.onSuccess(tokenResponse.access_token) }
            }
        })
    }

    /**
     * Save our new tokens in SharePreferences so we can access them at a later point
     * @param context
     * @param tokenResponse
     */
    private fun saveTokens(context: Context, tokenResponse: TokenResponse) {
        REFRESH_TOKEN = tokenResponse.refresh_token
        ACCESS_TOKEN = tokenResponse.access_token

        val preferences = Util.getPreferences(context.applicationContext).edit()
        preferences.putString(PREF_ACCESS_TOKEN, ACCESS_TOKEN)
        preferences.putString(PREF_REFRESH_TOKEN, REFRESH_TOKEN)
        //comes back in seconds, needs to be milis
        preferences.putLong(PREF_TOKEN_EXPIRES, System.currentTimeMillis() + tokenResponse.expires_in * 1000)
        preferences.commit()
    }

    interface TokenResponseCallback {
        fun onSuccess(response: TokenResponse)
        fun onFailure(error: Exception)
    }

    //for JSON parsing of our token responses
    class TokenResponse {
        lateinit var access_token: String
        lateinit var refresh_token: String
        lateinit var token_type: String
        var expires_in: Long = 0
    }

    interface TokenCallback {
        fun onSuccess(token: String)
        fun onFailure(e: Throwable)
    }
}
