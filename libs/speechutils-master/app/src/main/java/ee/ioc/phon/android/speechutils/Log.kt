package ee.ioc.phon.android.speechutils

object Log {

    val DEBUG = BuildConfig.DEBUG

    val LOG_TAG = "speechutils"

    fun i(msg: String) {
        if (DEBUG) android.util.Log.i(LOG_TAG, msg)
    }

    fun i(msgs: List<String>) {
        if (DEBUG) {
            for (msg in msgs) {
                if (msg == null) {
                    msg = "<null>"
                }
                android.util.Log.i(LOG_TAG, msg)
            }
        }
    }

    fun e(msg: String) {
        if (DEBUG) android.util.Log.e(LOG_TAG, msg)
    }

    fun i(tag: String, msg: String) {
        if (DEBUG) android.util.Log.i(tag, msg)
    }

    fun e(tag: String, msg: String) {
        if (DEBUG) android.util.Log.e(tag, msg)
    }
}
