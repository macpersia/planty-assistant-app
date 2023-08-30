package be.planty.android.alexa.callbacks

import com.amazon.identity.auth.device.AuthError

/**
 * A generic callback to handle four states of asynchronous operations
 */
interface AsyncCallback<D, E> {
    fun start()
    fun success(result: D)
    fun failure(error: AuthError?)
    fun complete()
}
