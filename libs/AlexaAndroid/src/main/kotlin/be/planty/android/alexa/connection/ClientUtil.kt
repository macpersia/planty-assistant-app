package be.planty.android.alexa.connection

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Create a singleton OkHttp client that, hopefully, will someday be able to make sure all connections are valid according to AVS's strict
 * security policy--this will hopefully fix the Connection Reset By Peer issue.
 *
 * Created by willb_000 on 6/26/2016.
 */
object ClientUtil {

    private lateinit var mClient: OkHttpClient
    private val CONNECTION_POOL_TIMEOUT_MILLISECONDS = (60 * 60 * 1000).toLong()

    // 0 => no timeout.
    val tlS12OkHttpClient: OkHttpClient
        get() {
            if (!::mClient.isInitialized) {

                val connectionPool = ConnectionPool(5,
                        CONNECTION_POOL_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                val client = OkHttpClient.Builder().connectTimeout(0, TimeUnit.MILLISECONDS)
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .connectionPool(connectionPool)

                mClient = client.build()
            }
            return mClient
        }

}
