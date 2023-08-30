package be.planty.android.alexa.utility

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.text.TextUtils
import android.widget.Toast

import java.util.UUID

/**
 * A collection of utility functions.
 *
 * @author wblaschko on 8/13/15.
 */
object Util {
    private var mPreferences: SharedPreferences? = null
    val IDENTIFIER = "identifier"

    val identifier: String
        get() = mPreferences?.getString(IDENTIFIER, "").orEmpty()

    val uuid: String
        get() {
            val identifier = identifier
            val prefix = if (TextUtils.isEmpty(identifier)) "" else identifier + "."
            return prefix + UUID.randomUUID().toString()
        }

    /**
     * Show an authorization toast on the main thread to make sure the user sees it
     * @param context local context
     * @param message the message to show the user
     */
    fun showAuthToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            val authToast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            authToast.show()
        }
    }


    /**
     * Get our default shared preferences
     * @param context local/application context
     * @return default shared preferences
     */
    fun getPreferences(context: Context): SharedPreferences {
        if (mPreferences == null) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        }
        return mPreferences as SharedPreferences
    }
}
