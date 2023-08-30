package be.planty.assistant.app

import android.app.Application
import android.util.Log
import be.planty.assistant.app.utility.SigningKey

/**
 * An application to handle all our initialization for the Alexa library before we
 * launch our VoiceLaunchActivity
 */
class AlexaApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        //if we run in DEBUG mode, we can get our signing key in the LogCat
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "${SigningKey.getCertificateMD5Fingerprint(this)}")
        }
    }

    companion object {

        private val TAG = AlexaApplication::class.java.simpleName

// Commented by Hadi
//        //Our Amazon application product ID, this is passed to the server when we authenticate
//        private val PRODUCT_ID = "interactive_conversation"

        //Our Application instance if we need to reference it directly
        /**
         * Return a reference to our mInstance instance
         * @return our current application instance, created in onCreate()
         */
        var instance: AlexaApplication? = null
            private set
    }
}
