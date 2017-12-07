package com.willblaschko.android.alexa

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log

import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.authorization.BuildConfig
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener
import com.amazon.identity.auth.device.authorization.api.AuthzConstants
import com.willblaschko.android.alexa.callbacks.AsyncCallback
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback
import com.willblaschko.android.alexa.utility.Util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Random

/**
 * A static instance class that manages Authentication with the Amazon servers, it uses the TokenManager helper class to do most of its operations
 * including get new/refresh tokens from the server
 *
 * Some more details here: https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/docs/authorizing-your-alexa-enabled-product-from-a-website
 */
class AuthorizationManager
/**
 * Create a new Auth Manager based on the supplied product id
 *
 * This will throw an error if our assets/api_key.txt file, our package name, and our signing key don't match the product ID, this is
 * a common sticking point for the application not working
 * @param context
 * @param productId
 */
(private val mContext: Context, private val mProductId: String) {
    lateinit var amazonAuthorizationManager: AmazonAuthorizationManager
        private set
    private var mCallback: AuthorizationCallback? = null

    //An authorization callback to check when we get success/failure from the Amazon authentication server
    private val authListener = object : AuthorizationListener {
        /**
         * Authorization was completed successfully.
         * Display the profile of the user who just completed authorization
         * @param response bundle containing authorization response. Not used.
         */
        override fun onSuccess(response: Bundle) {
            val authCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.`val`)

            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Authorization successful")
                Util.showAuthToast(mContext, "Authorization successful.")
            }

            TokenManager.getAccessToken(mContext, authCode!!, codeVerifier, amazonAuthorizationManager, object : TokenManager.TokenResponseCallback {
                override fun onSuccess(response: TokenManager.TokenResponse) {

                    if (mCallback != null) {
                        mCallback!!.onSuccess()
                    }
                }

                override fun onFailure(error: Exception) {
                    if (mCallback != null) {
                        mCallback!!.onError(error)
                    }
                }
            })

        }


        /**
         * There was an error during the attempt to authorize the application.
         * Log the error, and reset the profile text view.
         * @param ae the error that occurred during authorize
         */
        override fun onError(ae: AuthError) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "AuthError during authorization", ae)
                Util.showAuthToast(mContext, "Error during authorization.  Please try again.")
            }
            if (mCallback != null) {
                mCallback!!.onError(ae)
            }
        }

        /**
         * Authorization was cancelled before it could be completed.
         * A toast is shown to the user, to confirm that the operation was cancelled, and the profile text view is reset.
         * @param cause bundle containing the cause of the cancellation. Not used.
         */
        override fun onCancel(cause: Bundle) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "User cancelled authorization")
                Util.showAuthToast(mContext, "Authorization cancelled")
            }

            if (mCallback != null) {
                mCallback!!.onCancel()
            }
        }
    }


    /**
     * Return our stored code verifier, which needs to be consistent, if this doesn't exist, we create a new one and store the new result
     * @return the String code verifier
     */
    private//no verifier found, make and store the new one
    val codeVerifier: String
        get() {
            if (Util.getPreferences(mContext).contains(CODE_VERIFIER)) {
                return Util.getPreferences(mContext).getString(CODE_VERIFIER, "")
            }
            val verifier = createCodeVerifier()
            Util.getPreferences(mContext).edit().putString(CODE_VERIFIER, verifier).apply()
            return verifier
        }

    /**
     * Create a String hash based on the code verifier, this is used to verify the Token exchanges
     * @return
     */
    private val codeChallenge: String
        get() {
            val verifier = codeVerifier
            return base64UrlEncode(getHash(verifier))
        }

    init {
        try {
            amazonAuthorizationManager = AmazonAuthorizationManager(mContext, Bundle.EMPTY)
        } catch (e: IllegalArgumentException) {
            //This error will be thrown if the main project doesn't have the assets/api_key.txt file in it--this contains the security credentials from Amazon
            Util.showAuthToast(mContext, "APIKey is incorrect or does not exist.")
            Log.e(TAG, "Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist. Does assets/api_key.txt exist in the main application?", e)
        }
    }


    /**
     * Check if the user is currently logged in by checking for a valid access token (present and not expired).
     * @param context
     * @param callback
     */
    fun checkLoggedIn(context: Context, callback: AsyncCallback<Boolean, Throwable>) {
        TokenManager.getAccessToken(amazonAuthorizationManager!!, context, object : TokenManager.TokenCallback {
            override fun onSuccess(token: String) {
                callback.success(true)
            }

            override fun onFailure(e: Throwable) {
                callback.success(false)
                callback.failure(e)
            }
        })
    }

    /**
     * Request authorization for the user to be able to use the application, this opens an intent that feeds back to the app:
     *
     * <intent-filter>
     * <action android:name="android.intent.action.VIEW"></action>
     * <category android:name="android.intent.category.DEFAULT"></category>
     * <category android:name="android.intent.category.BROWSABLE"></category>
     *
     * <data android:host="APPLICATION.PACKAGE" android:scheme="amzn"></data>
    </intent-filter> *
     *
     * Make sure this is in the main application's AndroidManifest
     *
     * @param callback our state change callback
     */
    fun authorizeUser(callback: AuthorizationCallback) {
        mCallback = callback

        val PRODUCT_DSN = Settings.Secure.getString(mContext.contentResolver,
                Settings.Secure.ANDROID_ID)

        val options = Bundle()
        val scope_data = "{\"alexa:all\":{\"productID\":\"" + mProductId +
                "\", \"productInstanceAttributes\":{\"deviceSerialNumber\":\"" +
                PRODUCT_DSN + "\"}}}"
        options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.`val`, scope_data)

        options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.`val`, true)
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.`val`, codeChallenge)
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.`val`, "S256")

        amazonAuthorizationManager!!.authorize(APP_SCOPES, options, authListener)
    }

    companion object {

        private val TAG = "AuthorizationHandler"
        private val APP_SCOPES = arrayOf("alexa:all")


        private val CODE_VERIFIER = "code_verifier"

        /**
         * Create a new code verifier for our token exchanges
         * @return the new code verifier
         */
        @JvmOverloads internal fun createCodeVerifier(count: Int = 128): String {
        //fun createCodeVerifier(count: Int = 128): String {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray()
            val sb = StringBuilder()
            val random = Random()
            for (i in 0 until count) {
                val c = chars[random.nextInt(chars.size)]
                sb.append(c)
            }
            return sb.toString()
        }


        /**
         * Encode a byte array into a string, while trimming off the last characters, as required by the Amazon token server
         *
         * See: http://brockallen.com/2014/10/17/base64url-encoding/
         *
         * @param arg our hashed string
         * @return a new Base64 encoded string based on the hashed string
         */
        private fun base64UrlEncode(arg: ByteArray): String {
            var s = Base64.encodeToString(arg, 0) // Regular base64 encoder
            s = s.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] // Remove any trailing '='s
            s = s.replace('+', '-') // 62nd char of encoding
            s = s.replace('/', '_') // 63rd char of encoding
            return s
        }

        /**
         * Hash a string based on the SHA-256 message digest
         * @param password
         * @return
         */
        private fun getHash(password: String): ByteArray {
            var digest: MessageDigest? = null
            try {
                digest = MessageDigest.getInstance("SHA-256")
            } catch (e1: NoSuchAlgorithmException) {
                e1.printStackTrace()
            }

            digest!!.reset()
            return digest.digest(password.toByteArray())
        }
    }

}
/**
 * Create a new code verifier for our token exchanges
 * @return the new code verifier
 */
